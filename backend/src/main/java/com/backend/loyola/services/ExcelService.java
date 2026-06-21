package com.backend.loyola.services;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;


import com.backend.loyola.ia.IAconfig;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelService {

    public List<String> parseNames(Path filePath) throws IOException {
        List<String> result = new ArrayList<>();

        try (InputStream is = new FileInputStream(filePath.toFile());
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            int targetCol = -1;
            int startRow = -1;

            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.STRING
                            && "APELLIDO Y NOMBRE".equals(cell.getStringCellValue().trim().toUpperCase())) {
                        targetCol = cell.getColumnIndex();
                        startRow = cell.getRowIndex() + 1;
                        break;
                    }
                }
                if (targetCol != -1) break;
            }

            if (targetCol == -1) {
                throw new RuntimeException("Cell with 'APELLIDO Y NOMBRE' not found");
            }

            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell cell = row.getCell(targetCol);
                if (cell == null) continue;

                cell.setCellType(CellType.STRING);
                String raw = cell.getStringCellValue();
                if (raw == null || raw.trim().isEmpty()) continue;

                result.add(formatName(raw.trim()));
            }
        }

        return result;
    }

    private String formatName(String fullName) {
        String[] parts = fullName.split("\\s+");
        if (parts.length == 0) return "";

        StringBuilder sb = new StringBuilder();
        sb.append(capitalize(parts[0]));

        for (int i = 1; i < parts.length; i++) {
            sb.append(" ");
            if (i == 1 && parts.length != 2) {
                sb.append(Character.toUpperCase(parts[i].charAt(0))).append(".");
            } else if (i == parts.length - 1 && parts.length >= 4) {
                sb.append(Character.toUpperCase(parts[i].charAt(0))).append(".");
            } else if (parts.length == 2) {
                sb.append(capitalize(parts[i]));
            } else {
                sb.append(capitalize(parts[i]));
            }
        }

        if (sb.charAt(sb.length() - 1) != '.') {
            sb.append(".");
        }
        sb.append(",");
        return sb.toString();
    }

    public String getMomento(Path filePath) throws IOException {
        try (InputStream is = new FileInputStream(filePath.toFile());
             Workbook workbook = new XSSFWorkbook(is)) {

            for (Row row : workbook.getSheetAt(0)) {
                for (Cell cell : row) {
                    if (cell.getCellType() != CellType.STRING) continue;
                    String val = cell.getStringCellValue().trim().toUpperCase();
                    if (val.startsWith("MOMENTO PEDAGOGICO:")) {
                        String num = val.substring(val.indexOf(":") + 1).trim();
                        return switch (num) {
                            case "I" -> "primer momento";
                            case "II" -> "segundo momento";
                            case "III" -> "tercer momento";
                            default -> throw new RuntimeException("Unknown momento: " + num);
                        };
                    }
                }
            }
        }
        throw new RuntimeException("Cell with 'MOMENTO PEDAGOGICO:' not found");
    }

    public byte[] generateDocx(Path filePath) throws IOException {
        List<String> names = parseNames(filePath);
        List<String> rawNames = parseRawNames(filePath);
        String momento = getMomento(filePath);
        List<String> literals = getLiterals(filePath);

        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (int i = 0; i < names.size(); i++) {
                if (i > 0) {
                    XWPFParagraph breakP = doc.createParagraph();
                    breakP.setPageBreak(true);
                }

                XWPFParagraph p1 = doc.createParagraph();
                p1.createRun().setText(names.get(i) + " durante el " + momento + ",");

                String rawName = i < rawNames.size() ? rawNames.get(i) : "";
                String paragraph = generateStudentParagraph(filePath, rawName);
                XWPFParagraph p2 = doc.createParagraph();
                p2.createRun().setText(paragraph);
            }

            doc.write(out);
            return out.toByteArray();
        }
    }

    public List<String> parseRawNames(Path filePath) throws IOException {
        List<String> result = new ArrayList<>();
        try (InputStream is = new FileInputStream(filePath.toFile());
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            int startRow = -1;
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.STRING
                            && "APELLIDO Y NOMBRE".equals(cell.getStringCellValue().trim().toUpperCase())) {
                        startRow = cell.getRowIndex() + 1;
                        break;
                    }
                }
                if (startRow != -1) break;
            }
            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Cell cell = row.getCell(0);
                if (cell == null) continue;
                cell.setCellType(CellType.STRING);
                String raw = cell.getStringCellValue();
                if (raw == null || raw.trim().isEmpty()) continue;
                result.add(raw.trim());
            }
        }
        return result;
    }

    public List<String> getLiterals(Path filePath) throws IOException {
        List<String> result = new ArrayList<>();

        try (InputStream is = new FileInputStream(filePath.toFile());
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            int literalCol = -1;
            int headerRow = -1;

            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.STRING
                            && "APELLIDO Y NOMBRE".equals(cell.getStringCellValue().trim().toUpperCase())) {
                        headerRow = cell.getRowIndex();
                    }
                    if (cell.getCellType() == CellType.STRING
                            && "LITERAL".equals(cell.getStringCellValue().trim().toUpperCase())) {
                        literalCol = cell.getColumnIndex();
                        headerRow = Math.max(headerRow, cell.getRowIndex());
                    }
                }
                if (headerRow != -1 && literalCol != -1) break;
            }

            if (literalCol == -1) throw new RuntimeException("Cell with 'LITERAL' not found");

            for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell cell = row.getCell(literalCol);
                if (cell == null) continue;

                cell.setCellType(CellType.STRING);
                String raw = cell.getStringCellValue();
                if (raw == null || raw.trim().isEmpty()) continue;

                result.add(switch (raw.trim().toUpperCase()) {
                    case "A" -> "excelente";
                    case "B" -> "muy satisfactorio";
                    case "C" -> "satisfactorio";
                    case "D" -> "poco satisfactorio";
                    default -> throw new RuntimeException("Unknown literal: " + raw);
                });
            }
        }

        return result;
    }

    public String generateStudentParagraph(Path filePath, String studentName) throws IOException {
        try (InputStream is = new FileInputStream(filePath.toFile());
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            // ponytail: hardcoded areas and column ranges based on Excel layout
            Map<String, List<Integer>> areas = Map.of(
                    "LENGUAJE Y COMUNICACIÓN", range(2, 18),
                    "MATEMÁTICA", range(19, 26),
                    "CIENCIAS NATURALES", range(27, 31),
                    "CIENCIAS SOCIALES", range(32, 37),
                    "ESTÉTICA", range(38, 40),
                    "IOV", range(51, 53)
            );

            Map<String, List<String>> indicators = new LinkedHashMap<>();
            Row headerRow = sheet.getRow(7);
            Row indicatorRow = sheet.getRow(8);

            for (var entry : areas.entrySet()) {
                List<String> inds = new ArrayList<>();
                for (int col : entry.getValue()) {
                    Cell c = indicatorRow.getCell(col);
                    if (c != null) {
                        c.setCellType(CellType.STRING);
                        String v = c.getStringCellValue();
                        if (v != null && !v.trim().isEmpty()) inds.add(v.trim());
                    }
                }
                indicators.put(entry.getKey(), inds);
            }

            // Find first student row dynamically
            Row studentRow = null;
            for (Row r : sheet) {
                Cell n = r.getCell(0);
                if (n == null) continue;
                n.setCellType(CellType.STRING);
                String v = n.getStringCellValue();
                if (v != null && v.trim().equalsIgnoreCase(studentName.trim())) {
                    studentRow = r;
                    break;
                }
            }
            if (studentRow == null) throw new RuntimeException("Student not found: " + studentName);

            // Build data map for AI
            Map<String, Map<String, String>> studentData = new LinkedHashMap<>();
            for (var entry : areas.entrySet()) {
                Map<String, String> indGrades = new LinkedHashMap<>();
                List<Integer> cols = entry.getValue();
                List<String> inds = indicators.get(entry.getKey());
                for (int j = 0; j < cols.size() && j < inds.size(); j++) {
                    Cell g = studentRow.getCell(cols.get(j));
                    String grade = "";
                    if (g != null) {
                        g.setCellType(CellType.STRING);
                        String v = g.getStringCellValue();
                        if (v != null) grade = v.trim();
                    }
                    indGrades.put(inds.get(j), grade);
                }
                studentData.put(entry.getKey(), indGrades);
            }

            // Build prompt for AI
            StringBuilder prompt = new StringBuilder();
            prompt.append("Genera un informe pedagógico de un párrafo para el estudiante ")
                    .append(studentName).append(". ")
                    .append("Utiliza los siguientes datos de rendimiento por área e indicador:\n\n");

            for (var area : studentData.entrySet()) {
                prompt.append("Área: ").append(area.getKey()).append("\n");
                for (var ind : area.getValue().entrySet()) {
                    prompt.append("  - ").append(ind.getKey())
                            .append(" → ").append(formatGrade(ind.getValue())).append("\n");
                }
                prompt.append("\n");
            }

            prompt.append("""
                    El informe debe seguir esta estructura:
                    1. Comenzar con "Su rendimiento fue [excelente/muy satisfactorio/satisfactorio/poco satisfactorio] según su literal."
                    2. Mencionar logros específicos en Lenguaje y Comunicación citando textualmente 2 indicadores donde obtuvo LT.
                    3. Mencionar logros en Matemática citando textualmente 2 indicadores donde obtuvo LT.
                    4. Mencionar las demás áreas donde su rendimiento fue bueno.
                    5. Si hay áreas con LP o EP, mencionar recomendaciones breves y citar los indicadores en los que tuvo problemas.
                    6. Terminar con una frase motivacional entre comillas.
                    7. No menciones los indicadores como LP, LT, EP sino el texto del indicador.
                    8. A los indicadores que cites, la primera palabra que corresponde a un verbo debe de ir en preterito dando coherencia con la palabra anterior. 
                    Usar tono profesional, primera persona del plural.
                    """);

            return new IAconfig().generateParagraph(prompt.toString());
        }
    }

    private String formatGrade(String grade) {
        return switch (grade) {
            case "LT" -> "logrado totalmente";
            case "LP" -> "logrado parcialmente";
            case "EP" -> "en proceso";
            case "I" -> "insuficiente";
            default -> grade;
        };
    }

    private List<Integer> range(int from, int to) {
        List<Integer> r = new ArrayList<>();
        for (int i = from; i <= to; i++) r.add(i);
        return r;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT);
    }
}
