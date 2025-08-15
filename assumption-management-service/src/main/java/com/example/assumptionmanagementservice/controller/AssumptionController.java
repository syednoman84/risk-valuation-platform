package com.example.assumptionmanagementservice.controller;

import com.example.assumptionmanagementservice.dto.AssumptionSetDto;
import com.example.assumptionmanagementservice.entity.AssumptionSet;
import com.example.assumptionmanagementservice.service.AssumptionService;
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
    public ResponseEntity<Map<String, UUID>> upload(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Map<String, String> keyValues,
            @RequestParam(required = false) Map<String, MultipartFile> files
    ) throws IOException {
        UUID id = assumptionService.createAssumptionSet(name, description, keyValues, files != null ? files : Map.of());
        return ResponseEntity.ok(Map.of("id", id));
    }

    @GetMapping
    public List<AssumptionSet> getAll() {
        return assumptionService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssumptionSetDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(assumptionService.getDtoById(id));
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

