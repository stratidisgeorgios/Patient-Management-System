package com.patientsystem.patientservice.dto;

import java.util.Map;

public class ImportStartRequestDTO {
    private String s3Key;
    private Map<String, String> mapping;
    private int totalRows;

    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }
    public Map<String, String> getMapping() { return mapping; }
    public void setMapping(Map<String, String> mapping) { this.mapping = mapping; }
    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
}