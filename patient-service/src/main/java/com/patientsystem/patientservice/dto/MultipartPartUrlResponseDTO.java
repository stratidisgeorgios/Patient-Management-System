package com.patientsystem.patientservice.dto;

public class MultipartPartUrlResponseDTO {
    private String presignedUrl;

    public MultipartPartUrlResponseDTO(String presignedUrl) {
        this.presignedUrl = presignedUrl;
    }

    public String getPresignedUrl() { return presignedUrl; }
}
