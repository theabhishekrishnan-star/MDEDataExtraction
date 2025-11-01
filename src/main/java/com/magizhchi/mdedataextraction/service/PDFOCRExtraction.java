package com.magizhchi.mdedataextraction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magizhchi.mdedataextraction.model.PDFExtractionResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Service
public class PDFOCRExtraction implements DataExtraction {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Tesseract tesseract;
    private boolean ocrAvailable = false;
    
    public PDFOCRExtraction() {
        try {
            tesseract = new Tesseract();
            String[] possiblePaths = {
                "C:/Program Files/Tesseract-OCR/tessdata",
                "C:/Program Files (x86)/Tesseract-OCR/tessdata",
                "/usr/share/tesseract-ocr/4.00/tessdata",
                "/usr/share/tessdata"
            };
            
            for (String path : possiblePaths) {
                if (new File(path).exists()) {
                    tesseract.setDatapath(path);
                    tesseract.setLanguage("eng");
                    tesseract.setPageSegMode(6);
                    tesseract.setOcrEngineMode(3);
                    tesseract.setVariable("tessedit_char_whitelist", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,!?@#$%^&*()_+-=[]{}|;':\"<>?/~` ");
                    ocrAvailable = true;
                    break;
                }
            }
        } catch (Exception e) {
            ocrAvailable = false;
        }
    }
    
    @Override
    public String extract(String input) {
        return "PDF OCR extraction not supported for direct input";
    }
    
    public String extractWithDownloadUrl(String filePath, String fileName) {
        try {
            String jsonResult = extract(filePath, fileName);
            
            String jsonFileName = fileName.replace(".pdf", "_ocr_extracted.json");
            String downloadUrl = "http://localhost:8081/api/extract/download/" + jsonFileName + "?filePath=" + filePath;
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "PDF OCR processed and JSON file created successfully");
            response.put("downloadUrl", downloadUrl);
            response.put("fileName", jsonFileName);
            
            return objectMapper.writeValueAsString(response);
            
        } catch (Exception e) {
            return "{\"error\":\"Failed to create OCR download URL: " + e.getMessage() + "\"}";
        }
    }
    
    @Override
    public String extract(String filePath, String fileName) {
        if (!ocrAvailable) {
            return "{\"error\":\"Tesseract OCR is not properly configured or installed\"}";
        }
        
        try {
            String fullPath = filePath + "/" + fileName;
            File pdfFile = new File(fullPath);
            
            if (!pdfFile.exists()) {
                return "{\"error\":\"PDF file not found\"}";
            }
            
            PDDocument document = PDDocument.load(pdfFile);
            PDFRenderer renderer = new PDFRenderer(document);
            
            Map<String, Map<String, String>> allPagesData = new HashMap<>();
            int totalPages = document.getNumberOfPages();
            
            for (int pageNum = 0; pageNum < totalPages; pageNum++) {
                try {
                    BufferedImage image = renderer.renderImageWithDPI(pageNum, 300);
                    BufferedImage processedImage = preprocessImage(image);
                    String ocrText = tesseract.doOCR(processedImage);
                    
                    Map<String, String> pageData = extractSinglePageData(ocrText);
                    if (!pageData.isEmpty()) {
                        allPagesData.put("page_" + (pageNum + 1), pageData);
                    }
                } catch (Exception e) {
                    Map<String, String> errorData = new HashMap<>();
                    errorData.put("error", "Failed to process page " + (pageNum + 1) + ": " + e.getMessage());
                    allPagesData.put("page_" + (pageNum + 1), errorData);
                }
            }
            
            document.close();
            
            PDFExtractionResult result = new PDFExtractionResult();
            result.setContentType("ocr_paragraph");
            result.setParagraphData(allPagesData);
            
            String jsonResult = objectMapper.writeValueAsString(result);
            
            String jsonFileName = fileName.replace(".pdf", "_ocr_extracted.json");
            Files.write(Paths.get(filePath + "/" + jsonFileName), jsonResult.getBytes());
            
            return jsonResult;
            
        } catch (IOException e) {
            return "{\"error\":\"Failed to process PDF with OCR: " + e.getMessage() + "\"}";
        }
    }
    
    private BufferedImage preprocessImage(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage processed = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = original.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                
                // Apply threshold for better contrast
                gray = gray > 128 ? 255 : 0;
                
                int grayRGB = (gray << 16) | (gray << 8) | gray;
                processed.setRGB(x, y, grayRGB);
            }
        }
        
        return processed;
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