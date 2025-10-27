package com.magizhchi.mdedataextraction.service;

public interface DataExtraction {
    String extract(String input);
    String extract(String filePath, String fileName);
}