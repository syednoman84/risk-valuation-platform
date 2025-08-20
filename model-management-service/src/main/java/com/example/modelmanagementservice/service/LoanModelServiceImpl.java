package com.example.modelmanagementservice.service;

import com.example.modelmanagementservice.entity.LoanModel;
import com.example.modelmanagementservice.repository.LoanModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanModelServiceImpl implements LoanModelService {

    private final LoanModelRepository repository;

    @Override
    public LoanModel createModel(String name, String description, String jsonDefinition) {
        List<LoanModel> existingVersions = repository.findAllByName(name);
        int newVersion = existingVersions.stream()
                .mapToInt(LoanModel::getVersion)
                .max()
                .orElse(0) + 1;

        LoanModel model = LoanModel.builder()
                .name(name)
                .version(newVersion)
                .active(true)
                .description(description)
                .modelDefinition(jsonDefinition)  // ✅ store raw JSON directly
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return repository.save(model);
    }

    @Override
    public List<LoanModel> getAllVersions(String name) {
        return repository.findAllByName(name);
    }

    @Override
    public Optional<LoanModel> getModel(String name, int version) {
        return repository.findByNameAndVersion(name, version);
    }

    @Override
    public Optional<LoanModel> getLatestActiveModel(String name) {
        return repository.findFirstByNameAndActiveTrueOrderByVersionDesc(name);
    }

    public LoanModel getByIdAndVersion(UUID modelId, int version) {
        return repository.findByIdAndVersion(modelId, version)
                .orElseThrow(() -> new NoSuchElementException("Model not found for id=" + modelId + " v=" + version));
    }

    public boolean existsByIdAndVersion(UUID modelId, int version) {
        return repository.findByIdAndVersion(modelId, version).isPresent();
    }

    public boolean existsById(UUID id) { return repository.existsById(id); }

    public LoanModel getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Model not found: " + id));
    }

}
