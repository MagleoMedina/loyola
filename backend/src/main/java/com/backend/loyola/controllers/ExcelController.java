package com.backend.loyola.controllers;

import com.backend.loyola.ia.IAconfig;
import com.backend.loyola.services.ExcelService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class ExcelController {

    private final ExcelService excelService;
    private static Path currentFilePath;

    ExcelController(ExcelService excelService) {
        this.excelService = excelService;
    }

    private Path getFilePath() {
        return currentFilePath != null ? currentFilePath : Path.of("../notas.xlsx");
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        try {
            Path tempDir = Files.createTempDirectory("loyola-");
            Path target = tempDir.resolve("notas.xlsx");
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            currentFilePath = target;
            List<String> formatted = excelService.parseNames(target);
            List<String> raw = excelService.parseRawNames(target);
            List<Map<String, String>> result = new java.util.ArrayList<>();
            for (int i = 0; i < formatted.size(); i++) {
                result.add(Map.of(
                        "formatted", formatted.get(i),
                        "raw", i < raw.size() ? raw.get(i) : ""
                ));
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(List.of("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/parse-names")
    public ResponseEntity<?> parseNames() {
        try {
            List<String> names = excelService.parseNames(getFilePath());
            return ResponseEntity.ok(names);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing file: " + e.getMessage());
        }
    }

    @GetMapping("/momento")
    public ResponseEntity<?> getMomento() {
        try {
            String momento = excelService.getMomento(getFilePath());
            return ResponseEntity.ok(momento);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/literals")
    public ResponseEntity<?> getLiterals() {
        try {
            List<String> literals = excelService.getLiterals(getFilePath());
            return ResponseEntity.ok(literals);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/generate-docx")
    public ResponseEntity<?> generateDocx() {
        try {
            byte[] data = excelService.generateDocx(getFilePath());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=notas.docx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/ia-test")
    public ResponseEntity<?> iaTest() {
        try {
            String result = new IAconfig().test();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/student-report")
    public ResponseEntity<?> studentReport(@RequestParam(defaultValue = "GONZALEZ CASTRO FABIAN MATHÍAS") String name) {
        try {
            String report = excelService.generateStudentParagraph(getFilePath(), name);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/student-report")
    public ResponseEntity<?> studentReportPost(@RequestBody Map<String, String> body) {
        try {
            String name = body.get("name");
            if (name == null || name.isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "name is required"));

            Path filePath = getFilePath();
            String momento = excelService.getMomento(filePath);
            String report = excelService.generateStudentParagraph(filePath, name);

            return ResponseEntity.ok(Map.of(
                    "name", name,
                    "report", report
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
