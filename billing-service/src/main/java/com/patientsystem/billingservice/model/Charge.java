package com.patientsystem.billingservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "charge", indexes = {
    @Index(columnList = "billing_account_id")
})
public class Charge {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID billingAccountId;

    private String treatmentId;
    private String treatmentName;
    private String treatmentCategory;

    private BigDecimal price;
    private LocalDateTime timestamp;
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getBillingAccountId() { return billingAccountId; }
    public void setBillingAccountId(UUID billingAccountId) { this.billingAccountId = billingAccountId; }

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
