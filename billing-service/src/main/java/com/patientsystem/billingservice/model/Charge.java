package com.patientsystem.billingservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Charge {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String billingAccountId;

    private String treatmentId;
    private String treatmentName;
    private String treatmentCategory;

    private BigDecimal price;
    private LocalDateTime timestamp;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBillingAccountId() { return billingAccountId; }
    public void setBillingAccountId(String billingAccountId) { this.billingAccountId = billingAccountId; }

    public String getTreatmentId() { return treatmentId; }
    public void setTreatmentId(String treatmentId) { this.treatmentId = treatmentId; }

    public String getTreatmentName() { return treatmentName; }
    public void setTreatmentName(String treatmentName) { this.treatmentName = treatmentName; }

    public String getTreatmentCategory() { return treatmentCategory; }
    public void setTreatmentCategory(String treatmentCategory) { this.treatmentCategory = treatmentCategory; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
