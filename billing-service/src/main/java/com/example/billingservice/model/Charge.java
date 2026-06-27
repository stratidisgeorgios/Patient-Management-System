package com.example.billingservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;

@Entity
public class Charge {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String billingAccountId;
    
    @ManyToOne
    @JoinColumn(name = "treatment_catalog_id")
    private TreatmentCatalog treatmentCatalog;

    private BigDecimal price;
    private LocalDateTime timestamp;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBillingAccountId() { return billingAccountId; }
    public void setBillingAccountId(String billingAccountId) { this.billingAccountId = billingAccountId; }

    public TreatmentCatalog getTreatmentCatalog() { return treatmentCatalog; }
    public void setTreatmentCatalog(TreatmentCatalog treatmentCatalog) { this.treatmentCatalog = treatmentCatalog; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
