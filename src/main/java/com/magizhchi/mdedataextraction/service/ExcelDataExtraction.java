package com.magizhchi.mdedataextraction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Service
public class ExcelDataExtraction implements DataExtraction {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String extract(String input) {
        return "Excel text extraction not supported for direct input";
    }
    
    public String extractWithDownloadUrl(String filePath, String fileName) {
        try {
            String jsonResult = extract(filePath, fileName);
            
            String jsonFileName = fileName.replaceAll("\\.(xlsx|xls)$", "_extracted.json");
            String downloadUrl = "http://localhost:8081/api/extract/download/" + jsonFileName + "?filePath=" + filePath;
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Excel processed and JSON file created successfully");
            response.put("downloadUrl", downloadUrl);
            response.put("fileName", jsonFileName);
            
            return objectMapper.writeValueAsString(response);
            
        } catch (Exception e) {
            return "{\"error\":\"Failed to create download URL: " + e.getMessage() + "\"}";
        }
    }
    
    @Override
    public String extract(String filePath, String fileName) {
        try {
            String fullPath = filePath + "/" + fileName;
            File excelFile = new File(fullPath);
            
            if (!excelFile.exists()) {
                return "{\"error\":\"Excel file not found\"}";
            }
            
            FileInputStream fis = new FileInputStream(excelFile);
            Workbook workbook;
            
            if (fileName.toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else if (fileName.toLowerCase().endsWith(".xls")) {
                workbook = new HSSFWorkbook(fis);
            } else {
                return "{\"error\":\"Unsupported file format. Only .xlsx and .xls are supported\"}";
            }
            
            Map<String, List<Map<String, String>>> allSheetsData = new HashMap<>();
            
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                String sheetName = sheet.getSheetName();
                
                List<Map<String, String>> sheetData = extractSheetData(sheet);
                if (!sheetData.isEmpty()) {
                    allSheetsData.put(sheetName, sheetData);
                }
            }
            
            workbook.close();
            fis.close();
            
            Map<String, Object> result = new HashMap<>();
            result.put("contentType", "excel_sheets");
            result.put("sheetsData", allSheetsData);
            
            String jsonResult = objectMapper.writeValueAsString(result);
            
            String jsonFileName = fileName.replaceAll("\\.(xlsx|xls)$", "_extracted.json");
            Files.write(Paths.get(filePath + "/" + jsonFileName), jsonResult.getBytes());
            
            return jsonResult;
            
        } catch (IOException e) {
            return "{\"error\":\"Failed to process Excel: " + e.getMessage() + "\"}";
        }
    }
    
    private List<Map<String, String>> extractSheetData(Sheet sheet) {
        List<Map<String, String>> sheetData = new ArrayList<>();
        
        if (sheet.getPhysicalNumberOfRows() == 0) {
            return sheetData;
        }
        
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            return sheetData;
        }
        
        List<String> headers = new ArrayList<>();
        for (Cell cell : headerRow) {
            headers.add(getCellValueAsString(cell));
        }
        
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            
            Map<String, String> rowData = new HashMap<>();
            for (int cellIndex = 0; cellIndex < headers.size(); cellIndex++) {
                Cell cell = row.getCell(cellIndex);
                String cellValue = getCellValueAsString(cell);
                rowData.put(headers.get(cellIndex), cellValue);
            }
            
            sheetData.add(rowData);
        }
        
        return sheetData;
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}