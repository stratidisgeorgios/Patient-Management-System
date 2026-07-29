package com.patientsystem.patientservice.dto;

public class MultipartInitiateResponseDTO {
    private String uploadId;
    private String s3Key;

    public MultipartInitiateResponseDTO(String uploadId, String s3Key) {
        this.uploadId = uploadId;
        this.s3Key = s3Key;
    }

    public String getUploadId() { return uploadId; }
    public String getS3Key() { return s3Key; }
}
