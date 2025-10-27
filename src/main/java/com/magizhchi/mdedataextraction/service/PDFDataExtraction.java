package com.magizhchi.mdedataextraction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magizhchi.mdedataextraction.model.PDFExtractionResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Service
public class PDFDataExtraction implements DataExtraction {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String extract(String input) {
        return "PDF text extraction not supported for direct input";
    }
    
    public String extractWithDownloadUrl(String filePath, String fileName) {
        try {
            String jsonResult = extract(filePath, fileName);
            
            String jsonFileName = fileName.replace(".pdf", "_extracted.json");
            String downloadUrl = "http://localhost:8081/api/extract/download/" + jsonFileName + "?filePath=" + filePath;
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "PDF processed and JSON file created successfully");
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
            File pdfFile = new File(fullPath);
            
            if (!pdfFile.exists()) {
                return "{\"error\":\"PDF file not found\"}";
            }
            
            PDDocument document = PDDocument.load(pdfFile);
            PDFTextStripper stripper = new PDFTextStripper();
            
            Map<String, Map<String, String>> allPagesData = new HashMap<>();
            int totalPages = document.getNumberOfPages();
            
            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                stripper.setStartPage(pageNum);
                stripper.setEndPage(pageNum);
                String pageText = stripper.getText(document);
                
                Map<String, String> pageData = extractSinglePageData(pageText);
                if (!pageData.isEmpty()) {
                    allPagesData.put("page_" + pageNum, pageData);
                }
            }
            
            document.close();
            
            PDFExtractionResult result = new PDFExtractionResult();
            result.setContentType("paragraph");
            result.setParagraphData(allPagesData);
            
            String jsonResult = objectMapper.writeValueAsString(result);
            
            String jsonFileName = fileName.replace(".pdf", "_extracted.json");
            Files.write(Paths.get(filePath + "/" + jsonFileName), jsonResult.getBytes());
            
            return jsonResult;
            
        } catch (IOException e) {
            return "{\"error\":\"Failed to process PDF: " + e.getMessage() + "\"}";
        }
    }
    
    private Map<String, String> extractSinglePageData(String pageText) {
        Map<String, String> pageData = new HashMap<>();
        pageText = pageText.trim();
        
        if (pageText.isEmpty()) return pageData;
        
        String[] sections = pageText.split("\n\n");
        int lineCounter = 1;
        int paragraphCounter = 1;
        
        for (String section : sections) {
            section = section.trim();
            if (section.isEmpty()) continue;
            
            String[] lines = section.split("\n");
            
            if (lines.length == 1) {
                String key = "line_" + lineCounter;
                pageData.put(key, lines[0].trim());
                lineCounter++;
            } else {
                String key = "paragraph_" + paragraphCounter;
                StringBuilder paragraphText = new StringBuilder();
                
                for (String line : lines) {
                    paragraphText.append(line.trim()).append(" ");
                }
                
                pageData.put(key, paragraphText.toString().trim());
                paragraphCounter++;
            }
        }
        
        return pageData;
    }
}