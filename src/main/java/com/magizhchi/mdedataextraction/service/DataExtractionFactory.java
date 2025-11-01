package com.magizhchi.mdedataextraction.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataExtractionFactory {
    
    @Autowired
    private PDFDataExtraction pdfDataExtraction;
    
    @Autowired
    private PDFOCRExtraction pdfOCRExtraction;
    
    @Autowired
    private ExcelDataExtraction excelDataExtraction;
    
    public DataExtraction getExtractor(String fileType) {
        switch (fileType.toLowerCase()) {
            case "pdf":
                return pdfDataExtraction;
            case "pdf-ocr":
                return pdfOCRExtraction;
            case "excel":
            case "xlsx":
            case "xls":
                return excelDataExtraction;
            default:
                throw new IllegalArgumentException("Unsupported file type: " + fileType);
        }
    }
}