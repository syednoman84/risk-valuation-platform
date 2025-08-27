package com.example.assumptionmanagementservice.controller;

import com.example.assumptionmanagementservice.dto.AssumptionSetDto;
import com.example.assumptionmanagementservice.entity.AssumptionSet;
import com.example.assumptionmanagementservice.service.AssumptionService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/assumptions")
@RequiredArgsConstructor
public class AssumptionController {

    private final AssumptionService assumptionService;

    @PostMapping
    public ResponseEntity<AssumptionSetDto> upload(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Map<String, String> keyValues,
            @RequestParam(required = false) Map<String, MultipartFile> files
    ) throws IOException {
        AssumptionSetDto metadata = assumptionService.createAssumptionSetAndReturnDto(name, description, keyValues, files != null ? files : Map.of());
        return ResponseEntity.ok(metadata);
    }

    @GetMapping
    public ResponseEntity<List<AssumptionSetDto>> getAll() {
        return ResponseEntity.ok(assumptionService.getAllDtos());
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean hardcoded) throws IOException {
        if (hardcoded) {
            return ResponseEntity.ok(Map.of(
                "id", UUID.fromString("00000000-0000-0000-0000-000000000000"),
                "name", "Mock Assumption Set",
                "description", "Hardcoded test data",
                "locked", false,
                "createdAt", java.time.LocalDateTime.now(),
                "updatedAt", java.time.LocalDateTime.now(),
                "keyValues", Map.of(
                    "base_annual_rate", "0.08",
                    "default_term_months", "120"
                ),
                "tables", Map.of(
                    "credit_adjustments", List.of(
                        Map.of("credit_score", "650", "rate_adjustment", "0.5")
                    ),
                    "default_matrix", List.of(
                        Map.of("ltv_bucket", "low", "short_term", "0.01")
                    )
                )
            ));
        }
        return ResponseEntity.ok(assumptionService.getFormattedAssumptionData(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable UUID id) {
        assumptionService.delete(id);
        return ResponseEntity.ok("Assumption set deleted");
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) throws IOException {
        ByteArrayResource zip = assumptionService.downloadZip(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=assumption_set_" + id + ".zip")
                .contentLength(zip.contentLength())
                .body(zip);
    }

}

