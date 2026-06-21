package com.backend.loyola.controllers;

import com.backend.loyola.services.ExcelService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.List;

@RestController
public class ExcelController {

    @Autowired
    private ExcelService excelService;

    @GetMapping("/parse-names")
    public ResponseEntity<?> parseNames() {
        try {
            List<String> names = excelService.parseNames(Path.of("../notas.xlsx"));
            return ResponseEntity.ok(names);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing file: " + e.getMessage());
        }
    }
}
