package com.example.billingservice.dto;

import java.math.BigDecimal;

public class ChargeRequest {
    private String treatmentId;
    private BigDecimal price;

    public String getTreatmentId() { return treatmentId; }
    public void setTreatmentId(String treatmentId) { this.treatmentId = treatmentId; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
