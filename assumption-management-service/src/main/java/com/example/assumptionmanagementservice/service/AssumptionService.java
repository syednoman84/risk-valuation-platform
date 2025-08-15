package com.example.assumptionmanagementservice.service;

import com.example.assumptionmanagementservice.dto.AssumptionSetDto;
import com.example.assumptionmanagementservice.entity.AssumptionSet;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AssumptionService {
    UUID createAssumptionSet(String name, String description, Map<String, String> keyValues, Map<String, MultipartFile> csvFiles) throws IOException;
    AssumptionSet getById(UUID id);
    List<AssumptionSet> getAll();

    AssumptionSetDto getDtoById(UUID id);

    void delete(UUID id);
    ByteArrayResource downloadZip(UUID assumptionSetId) throws IOException;

}
