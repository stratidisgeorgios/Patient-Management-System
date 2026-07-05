package com.patientsystem.billingservice.dto;

public class ChargeResponseDTO {
    private String id;
    private String treatmentId;
    private String treatmentName;
    private String treatmentCategory;
    private String price;
    private String timestamp;

    public ChargeResponseDTO(String id, String treatmentId, String treatmentName, String treatmentCategory, String price, String timestamp) {
        this.id = id;
        this.treatmentId = treatmentId;
        this.treatmentName = treatmentName;
        this.treatmentCategory = treatmentCategory;
        this.price = price;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getTreatmentId() { return treatmentId; }
    public String getTreatmentName() { return treatmentName; }
    public String getTreatmentCategory() { return treatmentCategory; }
    public String getPrice() { return price; }
    public String getTimestamp() { return timestamp; }
}
