package com.magizhchi.mdedataextraction.controller;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.magizhchi.mdedataextraction.model.ExtractionRequest;
import com.magizhchi.mdedataextraction.service.DataExtraction;
import com.magizhchi.mdedataextraction.service.DataExtractionFactory;
import com.magizhchi.mdedataextraction.service.PDFDataExtraction;

@RestController
@RequestMapping("/api/extract")
public class DataExtractionController {
    
    @Autowired
    private DataExtractionFactory factory;
    
    @PostMapping("/file")
    public ResponseEntity<String> extractFromFile(@RequestBody ExtractionRequest request) {
        DataExtraction extractor = factory.getExtractor(request.getFileType());
        String result = extractor.extract(request.getFilePath(), request.getFileName());
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }
    
    @PostMapping("/file/download")
    public ResponseEntity<String> extractFromFileWithDownload(@RequestBody ExtractionRequest request) {
        try {
            if ("pdf".equals(request.getFileType())) {
                PDFDataExtraction pdfExtractor = (PDFDataExtraction) factory.getExtractor("pdf");
                String result = pdfExtractor.extractWithDownloadUrl(request.getFilePath(), request.getFileName());
                
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(result);
            } else {
                return ResponseEntity.badRequest().body("{\"error\":\"Download feature only supported for PDF files\"}");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("{\"error\":\"Failed to process file: " + e.getMessage() + "\"}");
        }
    }
    
    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadJsonFile(@PathVariable String fileName, @RequestParam String filePath) {
        try {
            File file = new File(filePath, fileName);
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}