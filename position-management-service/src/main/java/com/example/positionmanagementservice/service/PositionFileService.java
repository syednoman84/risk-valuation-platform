package com.example.positionmanagementservice.service;

import com.example.positionmanagementservice.entity.*;
import com.example.positionmanagementservice.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class PositionFileService {

    private final PositionFileRepository positionFileRepository;
    private final LoanRepository loanRepository;
    private final PaymentScheduleRepository paymentScheduleRepository;
    private final RateScheduleRepository rateScheduleRepository;
    private final CustomFieldRepository customFieldRepository;
    private final FileStorageService fileStorageService;

    private final ObjectMapper objectMapper = new ObjectMapper(); // For jsonb handling
    private static final Set<String> STANDARD_SCHEDULE_COLUMNS = Set.of("loanNumber", "dueDate", "principalDue", "interestDue", "paymentType");
    private static final Set<String> STANDARD_RATE_COLUMNS = Set.of("loanNumber", "effectiveDate", "rate");
    private static final Set<String> STANDARD_LOAN_COLUMNS = Set.of(
            "loanNumber", "principal", "interestRate", "termMonths", "amortizationType", "originationDate"
    );

    public void handleUpload(String name, LocalDate positionDate, MultipartFile zipFile) throws IOException {

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
            Map<String, Loan> loanMap = loanRepository.findAllByPositionFile(positionFile).stream()
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
            // Cleanup on failure
            positionFileRepository.delete(positionFile);
            throw new RuntimeException("Parsing failed: " + ex.getMessage(), ex);
        }
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
        for (CSVRecord row : records) {
            Loan loan = new Loan();
            loan.setPositionFile(positionFile);

            Map<String, Object> extra = new HashMap<>();

            for (Map.Entry<String, String> entry : row.toMap().entrySet()) {
                String column = entry.getKey();
                String value = entry.getValue();

                if (STANDARD_LOAN_COLUMNS.contains(column)) {
                    switch (column) {
                        case "loanNumber" -> loan.setLoanNumber(value);
                        case "principal" -> loan.setPrincipal(new BigDecimal(value));
                        case "interestRate" -> loan.setInterestRate(new BigDecimal(value));
                        case "termMonths" -> loan.setTermMonths(Integer.parseInt(value));
                        case "amortizationType" -> loan.setAmortizationType(value);
                        case "originationDate" -> loan.setOriginationDate(LocalDate.parse(value));
                    }
                } else {
                    extra.put(column, value);
                }
            }

            loan.setExtraFields(extra);
            loanRepository.save(loan);
        }
    }

    private void parsePaymentSchedule(List<CSVRecord> records, Map<String, Loan> loanMap) {
        for (CSVRecord row : records) {
            String loanNumber = row.get("loanNumber");
            Loan loan = loanMap.get(loanNumber);
            if (loan == null) continue;

            PaymentSchedule ps = new PaymentSchedule();
            ps.setLoan(loan);

            Map<String, Object> extra = new HashMap<>();
            for (Map.Entry<String, String> entry : row.toMap().entrySet()) {
                String column = entry.getKey();
                String value = entry.getValue();

                if (STANDARD_SCHEDULE_COLUMNS.contains(column)) {
                    switch (column) {
                        case "dueDate" -> ps.setDueDate(LocalDate.parse(value));
                        case "principalDue" -> ps.setPrincipalDue(new BigDecimal(value));
                        case "interestDue" -> ps.setInterestDue(new BigDecimal(value));
                        case "paymentType" -> ps.setPaymentType(value);
                    }
                } else if (!column.equals("loanNumber")) {
                    extra.put(column, value);
                }
            }

            ps.setExtraFields(extra);
            paymentScheduleRepository.save(ps);
        }
    }

    private void parseRateSchedule(List<CSVRecord> records, Map<String, Loan> loanMap) {
        for (CSVRecord row : records) {
            String loanNumber = row.get("loanNumber");
            Loan loan = loanMap.get(loanNumber);
            if (loan == null) continue;

            RateSchedule rs = new RateSchedule();
            rs.setLoan(loan);

            Map<String, Object> extra = new HashMap<>();
            for (Map.Entry<String, String> entry : row.toMap().entrySet()) {
                String column = entry.getKey();
                String value = entry.getValue();

                if (STANDARD_RATE_COLUMNS.contains(column)) {
                    switch (column) {
                        case "effectiveDate" -> rs.setEffectiveDate(LocalDate.parse(value));
                        case "rate" -> rs.setRate(new BigDecimal(value));
                    }
                } else if (!column.equals("loanNumber")) {
                    extra.put(column, value);
                }
            }

            rs.setExtraFields(extra);
            rateScheduleRepository.save(rs);
        }
    }

    private void parseCustomFields(List<CSVRecord> records, Map<String, Loan> loanMap) {
        for (CSVRecord row : records) {
            String loanNumber = row.get("loanNumber");
            Loan loan = loanMap.get(loanNumber);
            if (loan == null) continue;

            for (Map.Entry<String, String> entry : row.toMap().entrySet()) {
                String field = entry.getKey();
                if (field.equals("loanNumber")) continue;

                CustomField cf = new CustomField();
                cf.setLoan(loan);
                cf.setFieldName(field);
                cf.setFieldValue(entry.getValue());

                customFieldRepository.save(cf);
            }
        }
    }

    public PositionFile getById(UUID id) {
        return positionFileRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Position file not found."));
    }

    @Transactional
    public void deleteById(UUID id) {
        PositionFile file = getById(id);

        // Check if a Position File is locked meaning used in an execution then do not allow the deletion
        if (file.isLocked()) {
            throw new IllegalStateException("Cannot delete a position file that is locked (used in model execution).");
        }

        // Delete dependent data first
        List<Loan> loans = loanRepository.findAllByPositionFile(file);
        for (Loan loan : loans) {
            customFieldRepository.deleteAllByLoan(loan);
            paymentScheduleRepository.deleteAllByLoan(loan);
            rateScheduleRepository.deleteAllByLoan(loan);
        }
        loanRepository.deleteAll(loans);

        // Delete file metadata
        positionFileRepository.delete(file);

        // Optionally delete the ZIP file from disk
        try {
            Files.deleteIfExists(Paths.get(file.getOriginalFilePath()));
        } catch (IOException e) {
            System.err.println("Warning: Failed to delete ZIP from disk: " + e.getMessage());
        }
    }

}

