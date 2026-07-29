package com.patientsystem.patientservice.dto;

import java.util.List;

public class CompleteMultipartRequestDTO {
    private String s3Key;
    private String uploadId;
    private List<CompletedPartDTO> parts;

    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }
    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }
    public List<CompletedPartDTO> getParts() { return parts; }
    public void setParts(List<CompletedPartDTO> parts) { this.parts = parts; }
}
