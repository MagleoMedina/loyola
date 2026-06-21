package com.backend.loyola.controllers;

import com.backend.loyola.ia.IAconfig;
import com.backend.loyola.services.ExcelService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
public class ExcelController {

    private final ExcelService excelService;

    ExcelController(ExcelService excelService) {
        this.excelService = excelService;
    }

    @GetMapping("/parse-names")
    public ResponseEntity<?> parseNames() {
        try {
            List<String> names = excelService.parseNames(Path.of("../notas.xlsx"));
            return ResponseEntity.ok(names);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing file: " + e.getMessage());
        }
    }

    @GetMapping("/momento")
    public ResponseEntity<?> getMomento() {
        try {
            String momento = excelService.getMomento(Path.of("../notas.xlsx"));
            return ResponseEntity.ok(momento);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/literals")
    public ResponseEntity<?> getLiterals() {
        try {
            List<String> literals = excelService.getLiterals(Path.of("../notas.xlsx"));
            return ResponseEntity.ok(literals);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/generate-docx")
    public ResponseEntity<?> generateDocx() {
        try {
            byte[] data = excelService.generateDocx(Path.of("../notas.xlsx"));
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
            String report = excelService.generateStudentParagraph(Path.of("../notas.xlsx"), name);
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

            Path filePath = Path.of("../notas.xlsx");
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
