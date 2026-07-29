package com.patientsystem.patientservice.dto;

public class PresignedUrlResponseDTO {
    private String presignedUrl;
    private String s3Key;

    public PresignedUrlResponseDTO(String presignedUrl, String s3Key) {
        this.presignedUrl = presignedUrl;
        this.s3Key = s3Key;
    }

    public String getPresignedUrl() { return presignedUrl; }
    public String getS3Key() { return s3Key; }
}
