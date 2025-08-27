package com.example.positionmanagementservice.service;

import com.example.positionmanagementservice.controller.PositionFileController;
import com.example.positionmanagementservice.dto.PositionFileMetaDTO;
import com.example.positionmanagementservice.entity.*;
import java.time.LocalDate;
import com.example.positionmanagementservice.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
@RequiredArgsConstructor
public class PositionFileService {

    private final PositionFileRepository positionFileRepository;
    private final LoanRepository loanRepository;
    private final PaymentScheduleRepository paymentScheduleRepository;
    private final RateScheduleRepository rateScheduleRepository;
    private final CustomFieldsRepository customFieldsRepository;
    private final FileStorageService fileStorageService;

    private final ObjectMapper objectMapper = new ObjectMapper(); // For jsonb handling
    private static final Set<String> STANDARD_SCHEDULE_COLUMNS = Set.of("loanNumber", "dueDate", "principalDue", "interestDue", "paymentType");
    private static final Set<String> STANDARD_RATE_COLUMNS = Set.of("loanNumber", "effectiveDate", "rate");
    private static final Set<String> STANDARD_LOAN_COLUMNS = Set.of(
            "loanNumber", "principal", "interestRate", "termMonths", "amortizationType", "originationDate"
    );

    @Transactional
    public UUID handleUpload(String name, LocalDate positionDate, MultipartFile zipFile) throws IOException {

        // Check uniqueness of name + date
        boolean alreadyExists = positionFileRepository
                .existsByNameAndPositionDate(name, positionDate);
        if (alreadyExists) {
            throw new IllegalArgumentException("A position file with the same name and date already exists.");
        }

        // Validate ZIP
        String originalZipName = zipFile.getOriginalFilename();
        if (originalZipName == null || !originalZipName.endsWith(".zip")) {
            throw new IllegalArgumentException("Uploaded file must be a .zip file.");
        }

        // Save using file storage service (local or cloud)
        String savedFilePath = fileStorageService.save(zipFile, originalZipName);

        // Create temp file object from saved path (for parsing CSVs)
        File tempZip = new File(savedFilePath);


        // Save metadata
        PositionFile positionFile = new PositionFile();
        positionFile.setName(name);
        positionFile.setZipFileName(originalZipName);
        positionFile.setPositionDate(positionDate);
        positionFile.setOriginalFilePath(tempZip.getAbsolutePath());
        positionFile.setUploadedAt(LocalDateTime.now());
        positionFile = positionFileRepository.save(positionFile);

        // Parse CSVs
        Map<String, List<CSVRecord>> parsedCsvs = extractCsvFiles(tempZip);

        if (!parsedCsvs.containsKey("loan.csv")) {
            throw new IllegalArgumentException("Missing required file: loan.csv inside ZIP.");
        }

        try {
            // 1. Parse loans
            parseLoanCsv(parsedCsvs.get("loan.csv"), positionFile);

            // 2. Build loan map
            Map<String, Loan> loanMap = loanRepository.findAllById_PositionFileId(positionFile.getId()).stream()
                    .collect(Collectors.toMap(Loan::getLoanNumber, Function.identity()));

            // 3. Remaining files
            if (parsedCsvs.containsKey("paymentschedule.csv")) {
                parsePaymentSchedule(parsedCsvs.get("paymentschedule.csv"), loanMap);
            }

            if (parsedCsvs.containsKey("rateschedule.csv")) {
                parseRateSchedule(parsedCsvs.get("rateschedule.csv"), loanMap);
            }

            if (parsedCsvs.containsKey("customfields.csv")) {
                parseCustomFields(parsedCsvs.get("customfields.csv"), loanMap);
            }

        } catch (Exception ex) {
            throw new RuntimeException("Parsing failed: " + ex.getMessage(), ex);
        }
        
        return positionFile.getId();
    }


    public List<PositionFile> getAll() {
        return positionFileRepository.findAll();
    }

    // --- Helper Methods ---

    private Map<String, List<CSVRecord>> extractCsvFiles(File zipFile) throws IOException {
        Map<String, List<CSVRecord>> csvDataMap = new HashMap<>();

        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String fileName = Paths.get(entry.getName()).getFileName().toString();

                if (!fileName.endsWith(".csv")) continue;

                try (InputStream stream = zip.getInputStream(entry);
                     BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {

                    CSVParser parser = CSVFormat.DEFAULT
                            .withFirstRecordAsHeader()
                            .withTrim()
                            .parse(br);

                    List<CSVRecord> records = parser.getRecords();
                    csvDataMap.put(fileName, records);
                }
            }
        }

        return csvDataMap;
    }


    private void parseLoanCsv(List<CSVRecord> records, PositionFile positionFile) {
        if (records == null || records.isEmpty()) return;

        // Ensure we hold a managed reference in this method’s context
        UUID fileId = positionFile.getId();
        PositionFile pfRef = positionFileRepository.getReferenceById(fileId);

        List<Loan> toSave = new ArrayList<>(records.size());
        for (CSVRecord row : records) {
            String loanNumber = null;
            Loan loan = new Loan();

            Map<String, String> map = row.toMap();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String column = entry.getKey();
                String value = entry.getValue();

                if (STANDARD_LOAN_COLUMNS.contains(column)) {
                    switch (column) {
                        case "loanNumber" -> loanNumber = nz(value).trim();
                        case "principal" -> loan.setPrincipal(safeDecimal(value));
                        case "interestRate" -> loan.setInterestRate(safeDecimal(value));
                        case "termMonths" -> loan.setTermMonths(parseIntSafe(value));
                        case "amortizationType" -> loan.setAmortizationType(nz(value).trim());
                        case "originationDate" -> loan.setOriginationDate(safeDate(value));
                    }
                }
            }

            // minimal validation
            if (isBlank(loanNumber)) {
                continue;
            }

            // Set composite key
            LoanId loanId = new LoanId(fileId, loanNumber);
            loan.setId(loanId);

            toSave.add(loan);
        }

        if (!toSave.isEmpty()) {
            loanRepository.saveAll(toSave);
        }
    }

    private void parsePaymentSchedule(
            List<CSVRecord> records,
            Map<String, Loan> loanMap // key = loanNumber, value = Loan (with positionFile set)
    ) {
        if (records == null || records.isEmpty()) return;

        List<PaymentSchedule> toSave = new ArrayList<>(records.size());

        for (CSVRecord row : records) {
            String loanNumber = trimToNull(firstNonBlank(row, "loanNumber", "loan_number"));
            if (loanNumber == null) continue;

            // Anchor to the loan from THIS file
            Loan loan = loanMap.get(loanNumber);
            if (loan == null) continue;

            PaymentSchedule ps = new PaymentSchedule();
            
            String startDateStr = firstNonBlank(row, "startDate", "start_date");
            LocalDate startDate = isNotBlank(startDateStr) ? safeDate(startDateStr) : LocalDate.now();
            
            // Set composite key
            PaymentScheduleId psId = new PaymentScheduleId(loan.getPositionFileId(), loanNumber, startDate);
            ps.setId(psId);

            String endDateStr = firstNonBlank(row, "endDate", "end_date");
            if (isNotBlank(endDateStr)) ps.setEndDate(safeDate(endDateStr));

            String monthlyPaymentStr = firstNonBlank(row, "monthlyPayment", "monthly_payment");
            if (isNotBlank(monthlyPaymentStr)) ps.setMonthlyPayment(safeDecimal(monthlyPaymentStr));

            String interestPaymentStr = firstNonBlank(row, "interestPayment", "interest_payment");
            if (isNotBlank(interestPaymentStr)) ps.setInterestPayment(safeDecimal(interestPaymentStr));

            String principalPaymentStr = firstNonBlank(row, "principalPayment", "principal_payment");
            if (isNotBlank(principalPaymentStr)) ps.setPrincipalPayment(safeDecimal(principalPaymentStr));

            String paymentType = firstNonBlank(row, "paymentType", "payment_type", "type");
            if (isNotBlank(paymentType)) ps.setPaymentType(paymentType.trim());

            toSave.add(ps);
        }

        if (!toSave.isEmpty()) {
            paymentScheduleRepository.saveAll(toSave);
        }
    }

    private void parseRateSchedule(
            List<CSVRecord> records,
            Map<String, Loan> loanMap
    ) {
        if (records == null || records.isEmpty()) return;

        List<RateSchedule> toSave = new ArrayList<>(records.size());

        for (CSVRecord row : records) {
            String loanNumber = trimToNull(firstNonBlank(row, "loanNumber", "loan_number"));
            if (loanNumber == null) continue;

            Loan loan = loanMap.get(loanNumber);
            if (loan == null) continue;

            RateSchedule rs = new RateSchedule();
            
            String effDate = firstNonBlank(row, "effectiveDate", "effective_date");
            LocalDate effectiveDate = isNotBlank(effDate) ? safeDate(effDate) : LocalDate.now();
            
            // Set composite key
            RateScheduleId rsId = new RateScheduleId(loan.getPositionFileId(), loanNumber, effectiveDate);
            rs.setId(rsId);

            String rateStr = firstNonBlank(row, "rate", "interestRate", "interest_rate");
            if (isNotBlank(rateStr)) rs.setRate(safeDecimal(rateStr));

            toSave.add(rs);
        }

        if (!toSave.isEmpty()) {
            rateScheduleRepository.saveAll(toSave);
        }
    }

    private void parseCustomFields(
            List<CSVRecord> records,
            Map<String, Loan> loanMap
    ) {
        if (records == null || records.isEmpty()) return;

        Map<String, Map<String, Object>> byLoanNumber = new LinkedHashMap<>();

        for (CSVRecord row : records) {
            String loanNumber = trimToNull(firstNonBlank(row, "loanNumber", "loan_number"));
            if (loanNumber == null) continue;

            Loan loan = loanMap.get(loanNumber);
            if (loan == null) continue;

            Map<String, Object> fields = byLoanNumber.computeIfAbsent(loanNumber, k -> new LinkedHashMap<>());

            for (Map.Entry<String, String> e : row.toMap().entrySet()) {
                String col = e.getKey();
                if (col == null) continue;
                String colNorm = col.trim();
                if (colNorm.equalsIgnoreCase("loanNumber") || colNorm.equalsIgnoreCase("loan_number")) continue;
                fields.put(colNorm, e.getValue());
            }
        }

        if (byLoanNumber.isEmpty()) return;

        List<CustomFields> toSave = new ArrayList<>(byLoanNumber.size());
        for (Map.Entry<String, Map<String, Object>> entry : byLoanNumber.entrySet()) {
            String loanNumber = entry.getKey();
            Map<String, Object> fields = entry.getValue();

            Loan loan = loanMap.get(loanNumber);
            if (loan == null) continue;

            UUID fileId = loan.getPositionFileId();

            CustomFields cf = customFieldsRepository
                    .findById_PositionFileIdAndId_LoanNumber(fileId, loanNumber)
                    .orElseGet(CustomFields::new);

            // Set composite key
            CustomFieldId cfId = new CustomFieldId(fileId, loanNumber);
            cf.setId(cfId);
            cf.setFields(fields);

            toSave.add(cf);
        }

        if (!toSave.isEmpty()) {
            customFieldsRepository.saveAll(toSave);
        }
    }


    public PositionFile getById(UUID id) {
        return positionFileRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Position file not found."));
    }

    public long getCustomFieldsCount(UUID positionFileId) {
        return customFieldsRepository.countById_PositionFileId(positionFileId);
    }

    @Transactional
    public void deleteById(UUID id) {
        PositionFile file = getById(id);

        if (file.isLocked()) {
            throw new IllegalStateException("Cannot delete a position file that is locked (used in model execution).");
        }

        // Delete children first (order matters due to FKs)
        customFieldsRepository.deleteById_PositionFileId(file.getId());
        paymentScheduleRepository.deleteById_PositionFileId(file.getId());
        rateScheduleRepository.deleteById_PositionFileId(file.getId());

        // Delete loans
        loanRepository.deleteById_PositionFileId(file.getId());

        // Delete the PositionFile row
        positionFileRepository.delete(file);

        // Best-effort: remove ZIP from disk
        try {
            Files.deleteIfExists(Paths.get(file.getOriginalFilePath()));
        } catch (IOException e) {
            System.err.println("Warning: Failed to delete ZIP from disk: " + e.getMessage());
        }
    }

    public boolean exists(UUID id) {
        return positionFileRepository.existsById(id);
    }

    public long getLoanCount(UUID positionFileId) {
        ensureExists(positionFileId);
        return loanRepository.countById_PositionFileId(positionFileId);
    }

    public long getPaymentScheduleCount(UUID positionFileId) {
        ensureExists(positionFileId);
        return paymentScheduleRepository.countById_PositionFileId(positionFileId);
    }

    public long getRateScheduleCount(UUID positionFileId) {
        ensureExists(positionFileId);
        return rateScheduleRepository.countById_PositionFileId(positionFileId);
    }

    public long getCustomFieldCount(UUID positionFileId) {
        ensureExists(positionFileId);
        return customFieldsRepository.countById_PositionFileId(positionFileId);
    }

    public PositionFileMetaDTO getMetadata(UUID positionFileId) {
        PositionFile pf = positionFileRepository.findById(positionFileId)
                .orElseThrow(() -> new NoSuchElementException("Position file not found: " + positionFileId));

        long loans = loanRepository.countById_PositionFileId(positionFileId);
        long schedules = paymentScheduleRepository.countById_PositionFileId(positionFileId);
        long rateRows = rateScheduleRepository.countById_PositionFileId(positionFileId);
        long customFields = customFieldsRepository.countById_PositionFileId(positionFileId);

        return new PositionFileMetaDTO(
                pf.getId(),
                pf.getName(),
                pf.getZipFileName(),
                pf.getOriginalFilePath(),
                loans, schedules, rateRows, customFields
        );
    }

    private void ensureExists(UUID id) {
        if (!exists(id)) throw new NoSuchElementException("Position file not found: " + id);
    }

    public List<PositionFileController.LoanRowDto> fetchLoansSlice(UUID positionFileId, long offset, int limit) {
        if (limit <= 0) throw new IllegalArgumentException("limit must be > 0");

        int page = Math.floorDiv((int) offset, Math.max(1, limit));
        Pageable pageable = PageRequest.of(page, limit, Sort.by("id.loanNumber").ascending());

        var loanPage = loanRepository.findById_PositionFileId(positionFileId, pageable);
        var loans = loanPage.getContent();
        if (loans.isEmpty()) return List.of();

        // Collect loanNumbers in this slice
        Set<String> loanNumbers = loans.stream()
                .map(Loan::getLoanNumber)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // ---- Bulk fetch related data for this slice ----

        // Payment Schedules, grouped by loanNumber
        Map<String, List<PaymentSchedule>> psByLoan = paymentScheduleRepository
                .findById_PositionFileIdAndId_LoanNumberIn(positionFileId, loanNumbers)
                .stream()
                .collect(Collectors.groupingBy(PaymentSchedule::getLoanNumber));

        // Rate Schedules, grouped by loanNumber
        Map<String, List<RateSchedule>> rsByLoan = rateScheduleRepository
                .findById_PositionFileIdAndId_LoanNumberIn(positionFileId, loanNumbers)
                .stream()
                .collect(Collectors.groupingBy(RateSchedule::getLoanNumber));

        // Custom Fields (1 row per loanNumber), map by loanNumber
        Map<String, CustomFields> cfByLoan = customFieldsRepository
                .findById_PositionFileIdAndId_LoanNumberIn(positionFileId, loanNumbers)
                .stream()
                .collect(Collectors.toMap(CustomFields::getLoanNumber, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        // ---- Assemble DTOs ----
        return loans.stream()
                .map(loan -> {
                    String loanId = (loan.getLoanNumber() != null && !loan.getLoanNumber().isBlank())
                            ? loan.getLoanNumber()
                            : loan.getId().getLoanNumber();

                    Map<String, Object> fields = new LinkedHashMap<>();
                    // loan.csv (standard)
                    fields.put("principal", loan.getPrincipal());
                    fields.put("interestRate", loan.getInterestRate());
                    fields.put("termMonths", loan.getTermMonths());
                    fields.put("amortizationType", loan.getAmortizationType());
                    fields.put("originationDate", loan.getOriginationDate());

                    // paymentschedule.csv → list of objects
                    List<Map<String, Object>> paymentScheduleList = psByLoan.getOrDefault(loan.getLoanNumber(), List.of())
                            .stream()
                            .map(ps -> {
                                Map<String, Object> m = new LinkedHashMap<>();
                                m.put("startDate", ps.getStartDate());
                                m.put("endDate", ps.getEndDate());
                                m.put("monthlyPayment", ps.getMonthlyPayment());
                                m.put("interestPayment", ps.getInterestPayment());
                                m.put("principalPayment", ps.getPrincipalPayment());
                                m.put("paymentType", ps.getPaymentType());
                                return m;
                            })
                            .toList();
                    fields.put("paymentSchedule", paymentScheduleList);

                    // rateschedule.csv → list of objects
                    List<Map<String, Object>> rateScheduleList = rsByLoan.getOrDefault(loan.getLoanNumber(), List.of())
                            .stream()
                            .map(rs -> {
                                Map<String, Object> m = new LinkedHashMap<>();
                                m.put("effectiveDate", rs.getEffectiveDate());
                                m.put("rate", rs.getRate());
                                return m;
                            })
                            .toList();
                    fields.put("rateSchedule", rateScheduleList);

                    // customfields.csv → single JSON map per loan (as stored)
                    Map<String, Object> custom = Optional.ofNullable(cfByLoan.get(loan.getLoanNumber()))
                            .map(CustomFields::getFields)
                            .orElse(Map.of());
                    fields.put("customFields", custom);

                    return new PositionFileController.LoanRowDto(loanId, fields);
                })
                .toList();
    }

    /*public List<PositionFileController.LoanRowDto> fetchLoansSlice(UUID positionFileId, long offset, int limit) {
        if (limit <= 0) throw new IllegalArgumentException("limit must be > 0");

        int page = (int) Math.floorDiv(offset, Math.max(1, limit));
        Pageable pageable = PageRequest.of(page, limit, Sort.by("loanNumber").ascending());

        var pageResult = loanRepository.findByPositionFile_Id(positionFileId, pageable);

        return pageResult.getContent().stream()
                .map(loan -> {
                    // choose external id
                    String loanId = (loan.getLoanNumber() != null && !loan.getLoanNumber().isBlank())
                            ? loan.getLoanNumber()
                            : loan.getId().toString();

                    // merge standard columns + extraFields into one map
                    Map<String, Object> fields = new java.util.LinkedHashMap<>();
                    fields.put("principal", loan.getPrincipal());
                    fields.put("interestRate", loan.getInterestRate());
                    fields.put("termMonths", loan.getTermMonths());
                    fields.put("amortizationType", loan.getAmortizationType());
                    fields.put("originationDate", loan.getOriginationDate()); // ISO string via Jackson

                    return new PositionFileController.LoanRowDto(loanId, fields);
                })
                .toList();
    }*/

    /* ---------- helpers ---------- */
    private static String firstNonBlank(CSVRecord row, String... candidates) {
        for (String c : candidates) {
            String v = nullSafeTrim(row.get(c));
            if (isNotBlank(v)) return v;
        }
        return null;
    }

    private static String nullSafeTrim(String s) {
        return s == null ? null : s.trim();
    }

    private static java.time.LocalDate safeDate(String s) {
        // If your dates are yyyy-MM-dd, this is fine; otherwise plug in a DateTimeFormatter
        return java.time.LocalDate.parse(s);
    }

    private static java.math.BigDecimal safeDecimal(String s) {
        return new java.math.BigDecimal(s);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    // return non-null string, replacing null with empty
    private static String nz(String s) {
        return (s == null) ? "" : s;
    }

    // parse int safely, return null if blank or bad
    private static Integer parseIntSafe(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null; // or log/warn and return default
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }


}

