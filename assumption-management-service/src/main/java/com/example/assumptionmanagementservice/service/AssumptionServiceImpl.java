package com.example.assumptionmanagementservice.service;

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

        // Saving Assumption Set
        AssumptionSet set = new AssumptionSet();
        set.setName(name);
        set.setDescription(description);
        set.setCreatedAt(LocalDateTime.now());
        set.setUpdatedAt(LocalDateTime.now());
        set = setRepository.save(set);


        for (Map.Entry<String, String> entry : keyValues.entrySet()) {
            AssumptionValue val = new AssumptionValue();
            val.setAssumptionSet(set);
            val.setKey(entry.getKey());
            val.setValue(entry.getValue());
            valueRepository.save(val);
        }

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
    public AssumptionSet getById(UUID id) {
        return setRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Assumption set not found"));
    }

    @Override
    public List<AssumptionSet> getAll() {
        return setRepository.findAll();
    }

    @Override
    public AssumptionSetDto getDtoById(UUID id) {
        AssumptionSet set = assumptionSetRepository.findWithValuesById(id)
                .orElseThrow(() -> new NoSuchElementException("Assumption Set not found"));

        AssumptionSetDto dto = new AssumptionSetDto();
        dto.setId(set.getId());
        dto.setName(set.getName());
        dto.setDescription(set.getDescription());
        dto.setLocked(set.isLocked());

        List<AssumptionValueDto> kvDtos = Optional.ofNullable(set.getKeyValues())
                .orElse(Collections.emptyList())
                .stream()
                .map(value -> {
                    AssumptionValueDto v = new AssumptionValueDto();
                    v.setKey(value.getKey());
                    v.setValue(value.getValue());
                    return v;
                }).collect(Collectors.toList());

        dto.setKeyValues(kvDtos);


        return dto;
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

