package com.magizhchi.mdedataextraction.controller;

import java.io.File;
import java.net.InetAddress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.magizhchi.mdedataextraction.service.DataExtraction;
import com.magizhchi.mdedataextraction.service.DataExtractionFactory;

@RestController
@RequestMapping("/api/extract")
public class DataExtractionController {
    
    @Autowired
    private DataExtractionFactory factory;
    
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file, 
            @RequestParam("fileType") String fileType,
            @RequestParam(value = "download", defaultValue = "false") boolean download) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("{\"error\":\"File is empty\"}");
            }
            
            String tempDir = "C:\\Users\\AbhiBhuvi-PC\\Desktop\\";
            String fileName = file.getOriginalFilename();
            File tempFile = new File(tempDir, fileName);
            file.transferTo(tempFile);
            
            DataExtraction extractor = factory.getExtractor(fileType);
            String result = extractor.extract(tempDir, fileName);
            
            tempFile.delete();
            
            if (download) {
            	 InetAddress inetAddress = InetAddress.getLocalHost();
                 System.out.println("IP Address: " + inetAddress.getHostAddress());
                String jsonFileName = fileName.replaceAll("\\.(pdf|xlsx|xls)$", "_extracted.json");
                String downloadUrl = "http://"+inetAddress.getHostAddress()+":8081/api/extract/download/" + jsonFileName + "?filePath=" + tempDir.replace("\\", "/");
                
                String response = "{\"message\":\"File processed successfully\",\"downloadUrl\":\"" + downloadUrl + "\",\"fileName\":\"" + jsonFileName + "\"}";
                
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response);
            } else {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(result);
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