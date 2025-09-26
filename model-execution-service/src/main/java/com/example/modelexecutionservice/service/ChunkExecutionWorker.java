package com.example.modelexecutionservice.service;

import com.example.modelexecutionservice.domain.ChunkStatus;
import com.example.modelexecutionservice.domain.ExecutionStatus;
import com.example.modelexecutionservice.engine.FormulaEngine;
import com.example.modelexecutionservice.entity.ExecutionResult;
import com.example.modelexecutionservice.entity.ModelExecution;
import com.example.modelexecutionservice.entity.ModelExecutionChunk;
import com.example.modelexecutionservice.external.AssumptionServiceClient;
import com.example.modelexecutionservice.external.ModelServiceClient;
import com.example.modelexecutionservice.external.PositionServiceClient;
import com.example.modelexecutionservice.repository.ExecutionResultRepository;
import com.example.modelexecutionservice.repository.ModelExecutionChunkRepository;
import com.example.modelexecutionservice.repository.ModelExecutionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkExecutionWorker {

    private final ModelExecutionRepository executionRepo;
    private final ModelExecutionChunkRepository chunkRepo;
    private final ExecutionResultRepository resultRepo;

    private final PositionServiceClient positionClient;
    private final AssumptionServiceClient assumptionClient;
    private final ModelServiceClient modelClient;

    private final FormulaEngine formulaEngine;
    private final ObjectMapper objectMapper;

    /**
     * Process a single chunk. You can add @Async("chunkExecutor") once AsyncConfig is enabled.
     */
    @Async("chunkExecutor")
    @Transactional
    public void processChunk(UUID chunkId) {
        ModelExecutionChunk chunk = chunkRepo.findById(chunkId)
                .orElseThrow(() -> new IllegalArgumentException("Chunk not found: " + chunkId));

        ModelExecution exec = executionRepo.findById(chunk.getExecutionId())
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + chunk.getExecutionId()));

        if (exec.getStatus() == ExecutionStatus.CANCELED || exec.isCancelRequested()) {
            markChunk(chunk, ChunkStatus.CANCELED, "Execution canceled");
            return;
        }

        // Mark RUNNING
        chunk.setStatus(ChunkStatus.RUNNING);
        chunk.setAttemptCount(chunk.getAttemptCount() + 1);
        chunk.setStartedAt(LocalDateTime.now());
        chunk.setUpdatedAt(LocalDateTime.now());
        chunkRepo.saveAndFlush(chunk);

        if (exec.getStatus() == ExecutionStatus.QUEUED) {
            exec.setStatus(ExecutionStatus.RUNNING);
            exec.setStartedAt(LocalDateTime.now());
            executionRepo.saveAndFlush(exec);
        }

        try {
            // Inputs
            JsonNode modelDef = modelClient.getDefinition(exec.getModelId(), exec.getModelVersion());
            AssumptionServiceClient.AssumptionBundle bundle = assumptionClient.getBundle(exec.getAssumptionSetId());

            // Loans in this slice
            long offset = chunk.getStartOffset();
            int limit = (int) (chunk.getEndOffset() - chunk.getStartOffset());
            List<PositionServiceClient.LoanRow> loans =
                    positionClient.getLoansSlice(exec.getPositionFileId(), offset, limit);

            AtomicLong processed = new AtomicLong(0);
            AtomicLong succeeded = new AtomicLong(0);
            AtomicLong failed = new AtomicLong(0);

            for (PositionServiceClient.LoanRow loan : loans) {
                if (exec.isCancelRequested()) break;

                try {
                    ObjectNode output = evaluate(modelDef, loan, bundle, exec);
                    output.set("positionData", objectMapper.valueToTree(loan.fields()));

                    resultRepo.save(ExecutionResult.builder()
                            .executionId(exec.getId())
                            .loanId(loan.loanId())
                            .output(output)
                            .build());

                    succeeded.incrementAndGet();
                } catch (Exception ex) {
                    failed.incrementAndGet();
                    log.error("Chunk {} loan {} failed: {}", chunkId, loan.loanId(), ex.getMessage(), ex);
                } finally {
                    long p = processed.incrementAndGet();
                    if (p % 200 == 0) {
                        bumpExecCounters(exec.getId(), 200, (int) Math.min(200, succeeded.get()), (int) Math.min(200, failed.get()));
                        succeeded.addAndGet(-Math.min(200, (int) succeeded.get()));
                        failed.addAndGet(-Math.min(200, (int) failed.get()));
                    }
                }
            }

            // flush remaining
            bumpExecCounters(exec.getId(), (int) processed.get(), (int) succeeded.get(), (int) failed.get());

            // finalize chunk
            if (exec.isCancelRequested()) {
                markChunk(chunk, ChunkStatus.CANCELED, "Canceled during processing");
            } else if (failed.get() > 0 && processed.get() > failed.get()) {
                markChunk(chunk, ChunkStatus.PARTIAL_SUCCESS, "Some loans failed");
            } else if (failed.get() > 0 && processed.get() == failed.get()) {
                markChunk(chunk, ChunkStatus.FAILED, "All loans failed");
            } else {
                markChunk(chunk, ChunkStatus.COMPLETED, null);
            }

            // maybe finalize execution
            maybeFinalizeExecution(exec.getId());

        } catch (Exception e) {
            markChunk(chunk, ChunkStatus.FAILED, truncate(e.getMessage()));
            maybeFinalizeExecution(exec.getId());
        }
    }

    private ObjectNode evaluate(JsonNode modelDef,
                                PositionServiceClient.LoanRow loan,
                                AssumptionServiceClient.AssumptionBundle bundle,
                                ModelExecution exec) {
        Map<String, Object> ctx = new HashMap<>(loan.fields());
        
        // Create assumption object with keyLookup and tableLookup
        Map<String, Object> assumptionObj = new HashMap<>();
        assumptionObj.put("keyLookup", bundle.keyValues());
        assumptionObj.put("tableLookup", new LookupFunction(bundle.tables()));
        ctx.put("assumption", assumptionObj);
        
        // Add loanId from the LoanRow record (both loanId and loanNumber for compatibility)
        ctx.put("loanId", loan.loanId());
        ctx.put("loanNumber", loan.loanId());
        
        // Add previous results if this is part of a chain execution
        if (exec.getOptions() != null && exec.getOptions().has("previousResults")) {
            JsonNode previousResultsNode = exec.getOptions().get("previousResults");
            String outputPrefix = exec.getOptions().has("outputPrefix") ? 
                    exec.getOptions().get("outputPrefix").asText() : "prev_";
            
            if (previousResultsNode != null && previousResultsNode.has(loan.loanId())) {
                JsonNode loanPreviousResults = previousResultsNode.get(loan.loanId());
                log.info("  📥 Loading previous results for loan {} with prefix '{}'", loan.loanId(), outputPrefix);
                
                if (loanPreviousResults != null && loanPreviousResults.isObject()) {
                    loanPreviousResults.fields().forEachRemaining(entry -> {
                        String key = outputPrefix + entry.getKey();
                        JsonNode value = entry.getValue();
                        if (value.isNumber()) {
                            ctx.put(key, value.asDouble());
                            log.info("    ✓ {} = {}", key, value.asDouble());
                        } else if (value.isBoolean()) {
                            ctx.put(key, value.asBoolean());
                        } else {
                            ctx.put(key, value.asText());
                        }
                    });
                }
            }
        }
        
        // Flatten customFields to root level for easy access
        Object customFields = loan.fields().get("customFields");
        if (customFields instanceof Map) {
            Map<String, Object> customMap = (Map<String, Object>) customFields;
            for (Map.Entry<String, Object> entry : customMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                // Convert string numbers to actual numbers
                if (value instanceof String) {
                    String strValue = (String) value;
                    try {
                        if (strValue.contains(".")) {
                            ctx.put(key, Double.parseDouble(strValue));
                        } else {
                            ctx.put(key, Integer.parseInt(strValue));
                        }
                    } catch (NumberFormatException e) {
                        ctx.put(key, value); // keep as string if not a number
                    }
                } else {
                    ctx.put(key, value);
                }
            }
        }
        


        ObjectNode derivedBag = objectMapper.createObjectNode();
        ObjectNode outputsBag = objectMapper.createObjectNode();

        // Handle both flat and nested model definition structures
        JsonNode derived = modelDef.path("derived");
        if (!derived.isArray()) {
            derived = modelDef.path("derivedFields");
        }
        log.info("  🔄 Processing {} derived fields for loan {}", derived.isArray() ? derived.size() : 0, loan.loanId());
        if (derived.isArray()) {
            for (JsonNode d : derived) {
                String name = d.path("name").asText();
                String expr = d.path("expr").asText();
                if (expr.isEmpty()) {
                    expr = d.path("expression").asText();
                }
                Object val = formulaEngine.evaluate(expr, ctx);
                log.info("    ✓ {} = {} (from: {})", name, val, expr);
                derivedBag.set(name, toJson(val));
                ctx.put(name, val);
            }
        }

        JsonNode outputs = modelDef.path("outputs");
        if (outputs.isArray()) {
            for (JsonNode o : outputs) {
                String name = o.path("name").asText();
                String expr = o.path("expression").asText();
                Object val = formulaEngine.evaluate(expr, ctx);
                outputsBag.set(name, toJson(val));
            }
        }

        ObjectNode out = objectMapper.createObjectNode();
        out.set("derivedAttributes", derivedBag);
        out.set("results", outputsBag);
        return out;
    }

    private static Object lookup(Map<String, List<Map<String, Object>>> tables,
                                 String tableName, String keyCol, Object keyVal, String valueCol) {
        List<Map<String, Object>> table = tables.getOrDefault(tableName, Collections.emptyList());
        for (Map<String, Object> row : table) {
            Object v = row.get(keyCol);
            if (Objects.equals(String.valueOf(v), String.valueOf(keyVal))) {
                return row.get(valueCol);
            }
        }
        return null;
    }

    private JsonNode toJson(Object v) {
        if (v == null) return NullNode.instance;
        if (v instanceof JsonNode jn) return jn;
        if (v instanceof String s) return TextNode.valueOf(s);
        if (v instanceof Integer i) return IntNode.valueOf(i);
        if (v instanceof Long l) return LongNode.valueOf(l);
        if (v instanceof Float f) return FloatNode.valueOf(f);
        if (v instanceof Double d) return DoubleNode.valueOf(d);
        if (v instanceof Boolean b) return BooleanNode.valueOf(b);
        if (v instanceof Number n) return DecimalNode.valueOf(new java.math.BigDecimal(n.toString()));
        return objectMapper.valueToTree(v);
    }

    private void markChunk(ModelExecutionChunk chunk, ChunkStatus status, String msg) {
        chunk.setStatus(status);
        chunk.setErrorSummary(msg);
        chunk.setCompletedAt(LocalDateTime.now());
        chunk.setUpdatedAt(LocalDateTime.now());
        chunkRepo.save(chunk);
    }

    private void maybeFinalizeExecution(UUID executionId) {
        ModelExecution exec = executionRepo.findById(executionId).orElseThrow();
        if (exec.isCancelRequested()) {
            exec.setStatus(ExecutionStatus.CANCELED);
            exec.setCompletedAt(LocalDateTime.now());
            executionRepo.save(exec);
            return;
        }
        List<ModelExecutionChunk> chunks = chunkRepo.findByExecutionId(executionId);
        boolean anyActive = chunks.stream().anyMatch(c -> c.getStatus() == ChunkStatus.RUNNING || c.getStatus() == ChunkStatus.PENDING);
        if (anyActive) return;

        boolean anyFailed = chunks.stream().anyMatch(c -> c.getStatus() == ChunkStatus.FAILED);
        boolean anyPartial = chunks.stream().anyMatch(c -> c.getStatus() == ChunkStatus.PARTIAL_SUCCESS);

        exec.setCompletedAt(LocalDateTime.now());
        if (anyFailed && (exec.getSucceededLoans() != null && exec.getSucceededLoans() > 0)) {
            exec.setStatus(ExecutionStatus.PARTIAL_SUCCESS);
        } else if (anyFailed && (exec.getSucceededLoans() == null || exec.getSucceededLoans() == 0)) {
            exec.setStatus(ExecutionStatus.FAILED);
        } else if (anyPartial) {
            exec.setStatus(ExecutionStatus.PARTIAL_SUCCESS);
        } else {
            exec.setStatus(ExecutionStatus.COMPLETED);
        }
        executionRepo.save(exec);
        

    }
    


    private void bumpExecCounters(UUID executionId, int processed, int succeeded, int failed) {
        int attempts = 0;
        while (attempts < 3) {
            attempts++;
            try {
                ModelExecution e = executionRepo.findById(executionId).orElseThrow();
                e.setProcessedLoans(nz(e.getProcessedLoans()) + (long) processed);
                e.setSucceededLoans(nz(e.getSucceededLoans()) + (long) succeeded);
                e.setFailedLoans(nz(e.getFailedLoans()) + (long) failed);
                executionRepo.saveAndFlush(e);
                break;
            } catch (OptimisticLockingFailureException ex) {
                log.warn("Optimistic lock bumping counters for exec {}. Retrying...", executionId);
            }
        }
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    @FunctionalInterface
    public interface LookupFn {
        Object apply(String tableName, String keyCol, Object keyVal, String valueCol);
    }

    public static class LookupFunction {
        private final Map<String, List<Map<String, Object>>> tables;

        public LookupFunction(Map<String, List<Map<String, Object>>> tables) {
            this.tables = tables;
        }

        public Object lookup(String tableName, String keyCol, Object keyVal, String valueCol) {
            return ChunkExecutionWorker.lookup(tables, tableName, keyCol, keyVal, valueCol);
        }
    }

    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() > 1000 ? s.substring(0, 1000) : s;
    }
}
