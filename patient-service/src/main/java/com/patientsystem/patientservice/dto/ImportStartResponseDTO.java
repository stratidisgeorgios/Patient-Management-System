package com.patientsystem.patientservice.dto;

public class ImportStartResponseDTO {
    private String importJobId;

    public ImportStartResponseDTO(String importJobId) { this.importJobId = importJobId; }
    public String getImportJobId() { return importJobId; }
}