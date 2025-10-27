package com.magizhchi.mdedataextraction.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataExtractionFactory {
    
    @Autowired
    private PDFDataExtraction pdfDataExtraction;
    
    public DataExtraction getExtractor(String fileType) {
        switch (fileType.toLowerCase()) {
            case "pdf":
                return pdfDataExtraction;
            default:
                throw new IllegalArgumentException("Unsupported file type: " + fileType);
        }
    }
}