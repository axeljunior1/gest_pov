package com.erp.products.util;

import com.erp.products.domain.enums.ExportFormat;
import com.erp.products.exception.BusinessException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class TabularFileHelper {

    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private TabularFileHelper() {}

    public static byte[] write(ExportFormat format, List<String> headers, List<List<String>> rows) {
        return format == ExportFormat.XLSX ? writeXlsx(headers, rows) : writeCsv(headers, rows);
    }

    public static List<String[]> read(MultipartFile file) {
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        try {
            if (name.endsWith(".xlsx")) {
                return readXlsx(file.getInputStream());
            }
            return readCsv(file.getInputStream());
        } catch (IOException e) {
            throw new BusinessException("Impossible de lire le fichier: " + e.getMessage());
        }
    }

    public static byte[] template(ExportFormat format, List<String> headers) {
        List<List<String>> example = List.of(headers.stream().map(h -> "").toList());
        return write(format, headers, example);
    }

    private static byte[] writeCsv(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(";", headers)).append('\n');
        for (List<String> row : rows) {
            sb.append(row.stream().map(TabularFileHelper::escapeCsv).reduce((a, b) -> a + ";" + b).orElse(""))
                    .append('\n');
        }
        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[UTF8_BOM.length + body.length];
        System.arraycopy(UTF8_BOM, 0, out, 0, UTF8_BOM.length);
        System.arraycopy(body, 0, out, UTF8_BOM.length, body.length);
        return out;
    }

    private static byte[] writeXlsx(List<String> headers, List<List<String>> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("data");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
            }
            int rowIdx = 1;
            for (List<String> row : rows) {
                Row dataRow = sheet.createRow(rowIdx++);
                for (int i = 0; i < row.size(); i++) {
                    dataRow.createCell(i).setCellValue(row.get(i));
                }
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Erreur generation XLSX: " + e.getMessage());
        }
    }

    private static List<String[]> readCsv(InputStream in) throws IOException {
        String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }
        List<String[]> result = new ArrayList<>();
        for (String line : content.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            result.add(parseCsvLine(line));
        }
        return result;
    }

    private static String[] parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if ((c == ';' || c == ',') && !inQuotes) {
                cells.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        cells.add(current.toString().trim());
        return cells.toArray(new String[0]);
    }

    private static List<String[]> readXlsx(InputStream in) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String[]> rows = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();
            Iterator<Row> iterator = sheet.iterator();
            while (iterator.hasNext()) {
                Row row = iterator.next();
                if (isRowEmpty(row, formatter)) {
                    continue;
                }
                int lastCell = row.getLastCellNum();
                String[] cells = new String[lastCell];
                for (int i = 0; i < lastCell; i++) {
                    Cell cell = row.getCell(i);
                    cells[i] = cell == null ? "" : formatter.formatCellValue(cell).trim();
                }
                rows.add(cells);
            }
            return rows;
        }
    }

    private static boolean isRowEmpty(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (cell != null && !formatter.formatCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public static String cell(String[] row, int index) {
        if (row == null || index >= row.length) {
            return "";
        }
        return row[index] != null ? row[index].trim() : "";
    }

    public static String contentType(ExportFormat format) {
        return format == ExportFormat.XLSX
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "text/csv";
    }

    public static String extension(ExportFormat format) {
        return format == ExportFormat.XLSX ? "xlsx" : "csv";
    }
}
