package com.example.positionmanagementservice.controller;

import com.example.positionmanagementservice.dto.PositionFileMetaDTO;
import com.example.positionmanagementservice.entity.PositionFile;
import com.example.positionmanagementservice.service.PositionFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
public class PositionFileController {

    private final PositionFileService positionFileService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadPositionFile(
            @RequestParam("name") String name,
            @RequestParam("positionDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate positionDate,
            @RequestParam("file") MultipartFile zipFile
    ) throws IOException {
        positionFileService.handleUpload(name, positionDate, zipFile);
        return ResponseEntity.ok("File uploaded and parsed successfully.");
    }

    @GetMapping
    public List<PositionFile> listAll() {
        return positionFileService.getAll();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadZip(@PathVariable UUID id) throws IOException {
        PositionFile file = positionFileService.getById(id);

        Path path = Paths.get(file.getOriginalFilePath());
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getZipFileName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(Files.size(path))
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePositionFile(@PathVariable UUID id) {
        positionFileService.deleteById(id);
        return ResponseEntity.ok("Position file deleted.");
    }

    // HEAD /api/position-files/{id}
    @RequestMapping(path = "/{id}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> head(@PathVariable UUID id) {
        return positionFileService.exists(id) ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    // GET /api/position-files/{id}/loans/count
    @GetMapping("/{id}/loans/count")
    public Map<String, Long> getLoanCount(@PathVariable UUID id) {
        return Map.of("count", positionFileService.getLoanCount(id));
    }

    // (Optional) GET /api/position-files/{id}/payment-schedules/count
    @GetMapping("/{id}/payment-schedules/count")
    public Map<String, Long> getPaymentScheduleCount(@PathVariable UUID id) {
        return Map.of("count", positionFileService.getPaymentScheduleCount(id));
    }

    // (Optional) GET /api/position-files/{id}/rate-schedules/count
    @GetMapping("/{id}/rate-schedules/count")
    public Map<String, Long> getRateScheduleCount(@PathVariable UUID id) {
        return Map.of("count", positionFileService.getRateScheduleCount(id));
    }

    @GetMapping("/{id}/custom-fields/count")
    public Map<String, Long> getCustomFieldsCount(@PathVariable UUID id) {
        return Map.of("count", positionFileService.getCustomFieldsCount(id));
    }

    @GetMapping("/{id}/custom-fields/summary")
    public Map<String, Long> getCustomFieldsSummary(@PathVariable UUID id) {
        long rows = positionFileService.getCustomFieldsCount(id);
        return Map.of(
                "rows", rows,
                "loansWithCustomFields", rows  // equal in the new schema
        );
    }


    // (Optional) GET /api/position-files/{id}/metadata
    @GetMapping("/{id}/metadata")
    public PositionFileMetaDTO getMetadata(@PathVariable UUID id) {
        return positionFileService.getMetadata(id);
    }


    @GetMapping("/{id}/loans")
    public List<LoanRowDto> getLoansSlice(
            @PathVariable UUID id,
            @RequestParam long offset,
            @RequestParam int limit
    ) {
        if (limit <= 0) throw new IllegalArgumentException("limit must be > 0");
        return positionFileService.fetchLoansSlice(id, offset, limit);
    }

    // DTO returned to Model Execution Service
    public record LoanRowDto(
            String loanId,
            Map<String, Object> fields
    ) {}


}

