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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        String momento = getMomento(filePath);
        List<String> literals = getLiterals(filePath);
        IAconfig ia = new IAconfig();

        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (int i = 0; i < names.size(); i++) {
                if (i > 0) {
                    XWPFParagraph breakP = doc.createParagraph();
                    breakP.setPageBreak(true);
                }

                XWPFParagraph p1 = doc.createParagraph();
                p1.createRun().setText(names.get(i) + " durante el " + momento + ",");

                String literal = i < literals.size() ? literals.get(i) : "";
                String feedback = ia.generateFeedback(names.get(i), momento, literal);
                XWPFParagraph p2 = doc.createParagraph();
                p2.createRun().setText(feedback);
            }

            doc.write(out);
            return out.toByteArray();
        }
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

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT);
    }
}
