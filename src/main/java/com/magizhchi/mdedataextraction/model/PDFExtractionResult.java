package com.magizhchi.mdedataextraction.model;

import java.util.Map;

public class PDFExtractionResult {
    private String contentType;
    private Map<String, Map<String, String>> paragraphData;
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public Map<String, Map<String, String>> getParagraphData() {
        return paragraphData;
    }
    
    public void setParagraphData(Map<String, Map<String, String>> paragraphData) {
        this.paragraphData = paragraphData;
    }
}