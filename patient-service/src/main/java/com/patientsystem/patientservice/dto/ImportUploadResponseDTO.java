package com.patientsystem.patientservice.dto;

import java.util.Map;

public class ImportUploadResponseDTO {
    private String s3Key;
    private Map<String, String> mapping;
    private int totalRows;

    public ImportUploadResponseDTO(String s3Key, Map<String, String> mapping, int totalRows) {
        this.s3Key = s3Key;
        this.mapping = mapping;
        this.totalRows = totalRows;
    }

    public String getS3Key() { return s3Key; }
    public Map<String, String> getMapping() { return mapping; }
    public int getTotalRows() { return totalRows; }
}