package com.example.modelmanagementservice.controller;

import com.example.modelmanagementservice.dto.CreateModelRequest;
import com.example.modelmanagementservice.dto.ModelDetailsDTO;
import com.example.modelmanagementservice.entity.LoanModel;
import com.example.modelmanagementservice.service.LoanModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class LoanModelController {

    private final LoanModelService service;

    @PostMapping
    public ResponseEntity<LoanModel> create(@Valid @RequestBody CreateModelRequest request) {
        return ResponseEntity.ok(
                service.createModel(
                        request.getName(),
                        request.getDescription(),
                        request.getModelDefinition()
                )
        );
    }

    @GetMapping("/all")
    public ResponseEntity<List<LoanModel>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }


    @GetMapping("/by-name/{name}")
    public ResponseEntity<List<LoanModel>> getAllVersions(@PathVariable String name) {
        return ResponseEntity.ok(service.getAllVersions(name));
    }

    @GetMapping("/{name}/version/{version}")
    public ResponseEntity<LoanModel> getByVersion(
            @PathVariable String name,
            @PathVariable int version) {
        return service.getModel(name, version)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{name}/latest")
    public ResponseEntity<LoanModel> getLatest(@PathVariable String name) {
        return service.getLatestActiveModel(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // HEAD /models/{id}
    @RequestMapping(path = "/{id:[0-9a-fA-F\\-]{36}}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> headById(@PathVariable UUID id) {
        return service.existsById(id) ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }


    // GET /api/models/{id}
    @GetMapping(value = "/{id:[0-9a-fA-F\\-]{36}}", produces = "application/json")
    public ModelDetailsDTO getById(@PathVariable UUID id) {
        return ModelDetailsDTO.from(service.getById(id));
    }


}

