package com.example.modelmanagementservice.controller;

import com.example.modelmanagementservice.dto.CreateModelRequest;
import com.example.modelmanagementservice.entity.LoanModel;
import com.example.modelmanagementservice.service.LoanModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class LoanModelController {

    private final LoanModelService service;

    @PostMapping
    public ResponseEntity<LoanModel> create(@Valid @RequestBody CreateModelRequest request){        return ResponseEntity.ok(service.createModel(
                request.getName(),
                request.getDescription(),
                request.getJsonDefinition()));
    }


    @GetMapping("/{name}")
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
}

