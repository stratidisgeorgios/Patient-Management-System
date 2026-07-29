package com.patientsystem.patientservice.dto;

public class CompletedPartDTO {
    private int partNumber;
    private String etag;

    public int getPartNumber() { return partNumber; }
    public void setPartNumber(int partNumber) { this.partNumber = partNumber; }
    public String getEtag() { return etag; }
    public void setEtag(String etag) { this.etag = etag; }
}
