package com.backend.loyola.services;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

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

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT);
    }
}
