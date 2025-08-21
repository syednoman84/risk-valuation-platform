package com.example.modelexecutionservice.config;

import com.example.modelexecutionservice.engine.DefaultFormulaEngine;
import com.example.modelexecutionservice.engine.FormulaEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineConfig {

    @Bean
    public FormulaEngine formulaEngine() {
        return new DefaultFormulaEngine();
    }
}

