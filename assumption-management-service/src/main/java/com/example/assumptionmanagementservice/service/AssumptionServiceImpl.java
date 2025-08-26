package com.example.assumptionmanagementservice.service;

import com.example.assumptionmanagementservice.dto.AssumptionFileDto;
import com.example.assumptionmanagementservice.dto.AssumptionSetDto;
import com.example.assumptionmanagementservice.dto.AssumptionValueDto;
import com.example.assumptionmanagementservice.entity.AssumptionFile;
import com.example.assumptionmanagementservice.entity.AssumptionSet;
import com.example.assumptionmanagementservice.entity.AssumptionValue;
import com.example.assumptionmanagementservice.repository.AssumptionFileRepository;
import com.example.assumptionmanagementservice.repository.AssumptionSetRepository;
import com.example.assumptionmanagementservice.repository.AssumptionValueRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssumptionServiceImpl implements AssumptionService {

    private final AssumptionSetRepository setRepository;
    private final AssumptionValueRepository valueRepository;
    private final AssumptionFileRepository fileRepository;
    private final AssumptionSetRepository assumptionSetRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public UUID createAssumptionSet(String name, String description, Map<String, String> keyValues, Map<String, MultipartFile> csvFiles) throws IOException {
        if (setRepository.existsByName(name)) {
            throw new IllegalArgumentException("Assumption set with name '" + name + "' already exists.");
        }

        // Save Assumption Set
        AssumptionSet set = new AssumptionSet();
        set.setName(name);
        set.setDescription(description);
        set.setCreatedAt(LocalDateTime.now());
        set.setUpdatedAt(LocalDateTime.now());
        set = setRepository.save(set);

        // Save key-values (excluding reserved ones)
        for (Map.Entry<String, String> entry : keyValues.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.equalsIgnoreCase("name") || key.equalsIgnoreCase("description")) {
                log.warn("Skipping reserved key '{}' during assumption set creation.", key);
                continue;
            }

            AssumptionValue val = new AssumptionValue();
            val.setAssumptionSet(set);
            val.setKey(key);
            val.setValue(value);
            valueRepository.save(val);
        }

        // Save associated CSV files
        for (Map.Entry<String, MultipartFile> entry : csvFiles.entrySet()) {
            String key = entry.getKey();
            MultipartFile file = entry.getValue();

            String storedPath = fileStorageService.save(file, set.getName(), key);
            AssumptionFile assumptionFile = new AssumptionFile();
            assumptionFile.setAssumptionSet(set);
            assumptionFile.setKey(key);
            assumptionFile.setOriginalFileName(file.getOriginalFilename());
            assumptionFile.setFilePath(storedPath);
            assumptionFile.setUploadedAt(LocalDateTime.now());

            fileRepository.save(assumptionFile);
        }

        return set.getId();
    }

    @Override
    public List<AssumptionSetDto> getAllDtos() {
        List<AssumptionSet> sets = assumptionSetRepository.findAll();

        return sets.stream()
                .map(set -> {
                    AssumptionSetDto dto = new AssumptionSetDto();
                    dto.setId(set.getId());
                    dto.setName(set.getName());
                    dto.setDescription(set.getDescription());
                    dto.setLocked(set.isLocked());
                    dto.setCreatedAt(set.getCreatedAt());
                    dto.setUpdatedAt(set.getUpdatedAt());

                    // Map key-values
                    List<AssumptionValueDto> kvDtos = Optional.ofNullable(set.getKeyValues())
                            .orElse(Collections.emptySet())
                            .stream()
                            .map(value -> {
                                AssumptionValueDto v = new AssumptionValueDto();
                                v.setId(value.getId());
                                v.setKey(value.getKey());
                                v.setValue(value.getValue());
                                v.setCreatedAt(value.getCreatedAt());
                                return v;
                            }).collect(Collectors.toList());

                    dto.setTextBasedAssumptions(kvDtos);

                    // Map files
                    List<AssumptionFileDto> fileDtos = Optional.ofNullable(set.getFiles())
                            .orElse(Collections.emptySet())
                            .stream()
                            .map(file -> {
                                AssumptionFileDto f = new AssumptionFileDto();
                                f.setId(file.getId());
                                f.setKey(file.getKey());
                                f.setOriginalFileName(file.getOriginalFileName());
                                f.setFilePath(file.getFilePath());
                                f.setUploadedAt(file.getUploadedAt());
                                return f;
                            }).collect(Collectors.toList());

                    dto.setFileBasedAssumptions(fileDtos);

                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public AssumptionSet getById(UUID id) {
        return setRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Assumption set not found"));
    }

    @Override
    public AssumptionSetDto getDtoById(UUID id) {
        AssumptionSet set = assumptionSetRepository.findWithValuesAndFilesById(id)
                .orElseThrow(() -> new NoSuchElementException("Assumption Set not found"));

        AssumptionSetDto dto = new AssumptionSetDto();
        dto.setId(set.getId());
        dto.setName(set.getName());
        dto.setDescription(set.getDescription());
        dto.setLocked(set.isLocked());
        dto.setCreatedAt(set.getCreatedAt());
        dto.setUpdatedAt(set.getUpdatedAt());

        // Key-values
        List<AssumptionValueDto> kvDtos = Optional.ofNullable(set.getKeyValues())
                .orElse(Collections.emptySet())
                .stream()
                .map(value -> {
                    AssumptionValueDto v = new AssumptionValueDto();
                    v.setId(value.getId());
                    v.setKey(value.getKey());
                    v.setValue(value.getValue());
                    v.setCreatedAt(value.getCreatedAt());
                    return v;
                }).collect(Collectors.toList());

        dto.setTextBasedAssumptions(kvDtos);

        // Files
        List<AssumptionFileDto> fileDtos = Optional.ofNullable(set.getFiles())
                .orElse(Collections.emptySet())
                .stream()
                .map(file -> {
                    AssumptionFileDto f = new AssumptionFileDto();
                    f.setId(file.getId());
                    f.setKey(file.getKey());
                    f.setOriginalFileName(file.getOriginalFileName());
                    f.setFilePath(file.getFilePath());
                    f.setUploadedAt(file.getUploadedAt());
                    return f;
                }).collect(Collectors.toList());

        dto.setFileBasedAssumptions(fileDtos);

        return dto;
    }

    @Override
    public Map<String, Object> getFormattedAssumptionData(UUID id) throws IOException {
        AssumptionSet set = getById(id);
        
        // Get key-values
        Map<String, String> keyValues = valueRepository.findAllByAssumptionSet(set)
                .stream()
                .collect(Collectors.toMap(AssumptionValue::getKey, AssumptionValue::getValue));
        
        // Get tables from CSV files
        Map<String, List<Map<String, String>>> tables = new HashMap<>();
        List<AssumptionFile> files = fileRepository.findAllByAssumptionSet(set);
        
        for (AssumptionFile file : files) {
            Path filePath = Paths.get(file.getFilePath());
            if (Files.exists(filePath)) {
                List<Map<String, String>> csvData = parseCsvFile(filePath);
                tables.put(file.getKey(), csvData);
            }
        }
        
        return Map.of(
            "id", set.getId(),
            "name", set.getName(),
            "description", set.getDescription(),
            "locked", set.isLocked(),
            "createdAt", set.getCreatedAt(),
            "updatedAt", set.getUpdatedAt(),
            "keyValues", keyValues,
            "tables", tables
        );
    }
    
    private List<Map<String, String>> parseCsvFile(Path filePath) throws IOException {
        List<Map<String, String>> result = new ArrayList<>();
        List<String> lines = Files.readAllLines(filePath);
        
        if (lines.isEmpty()) return result;
        
        String[] headers = lines.get(0).split(",");
        for (int i = 1; i < lines.size(); i++) {
            String[] values = lines.get(i).split(",");
            Map<String, String> row = new HashMap<>();
            for (int j = 0; j < Math.min(headers.length, values.length); j++) {
                row.put(headers[j].trim(), values[j].trim());
            }
            result.add(row);
        }
        
        return result;
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        AssumptionSet set = getById(id);

        if (set.isLocked()) {
            throw new IllegalStateException("Cannot delete a locked assumption set.");
        }

        // Delete from DB
        valueRepository.deleteAllByAssumptionSet(set);
        fileRepository.deleteAllByAssumptionSet(set);
        setRepository.delete(set);

        // Delete folder
        Path setDir = Paths.get("assumption_files", set.getName());
        try {
            FileSystemUtils.deleteRecursively(setDir);
            log.info("Deleted assumption set folder: {}", setDir.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Failed to delete folder for assumption set '{}': {}", set.getName(), e.getMessage());
        }
    }


    @Override
    public ByteArrayResource downloadZip(UUID assumptionSetId) throws IOException {
        AssumptionSet set = getById(assumptionSetId);
        String setName = set.getName();

        List<AssumptionFile> files = fileRepository.findAllByAssumptionSet(set);
        List<AssumptionValue> values = valueRepository.findAllByAssumptionSet(set);

        Path tempZip = Files.createTempFile(setName + "_", ".zip");

        try (FileOutputStream fos = new FileOutputStream(tempZip.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            // Add all CSV files
            for (AssumptionFile file : files) {
                Path filePath = Paths.get(file.getFilePath());
                if (Files.exists(filePath)) {
                    zos.putNextEntry(new ZipEntry(file.getKey() + ".csv"));
                    Files.copy(filePath, zos);
                    zos.closeEntry();
                }
            }

            // Add key_values.txt
            zos.putNextEntry(new ZipEntry("key_values.txt"));
            for (AssumptionValue val : values) {
                String line = val.getKey() + " = " + val.getValue() + "\n";
                zos.write(line.getBytes());
            }
            zos.closeEntry();
        }

        byte[] zipBytes = Files.readAllBytes(tempZip);
        Files.deleteIfExists(tempZip);
        return new ByteArrayResource(zipBytes);
    }

}

