package com.example.modelmanagementservice.service;

import com.example.modelmanagementservice.entity.LoanModel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanModelService {
    LoanModel createModel(String name, String description, String jsonDefinition);
    List<LoanModel> getAllVersions(String name);
    Optional<LoanModel> getModel(String name, int version);
    Optional<LoanModel> getLatestActiveModel(String name);
}

