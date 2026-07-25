package com.patientsystem.patientservice.dto;

import java.util.Map;

public class ImportSqsMessage {
    private String jobId;
    private String s3Key;
    private Map<String, String> mapping;
    private String organizationId;

    public ImportSqsMessage() {}

    public ImportSqsMessage(String jobId, String s3Key, Map<String, String> mapping, String organizationId) {
        this.jobId = jobId;
        this.s3Key = s3Key;
        this.mapping = mapping;
        this.organizationId = organizationId;
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }
    public Map<String, String> getMapping() { return mapping; }
    public void setMapping(Map<String, String> mapping) { this.mapping = mapping; }
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
}
