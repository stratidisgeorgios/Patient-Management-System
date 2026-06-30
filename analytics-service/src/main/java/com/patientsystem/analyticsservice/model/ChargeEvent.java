package com.patientsystem.analyticsservice.model;

import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@IdClass(TimeSeriesId.class)
public class ChargeEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String patientId;
    private String treatmentName;
    private String category;
    private String price;
    @Id
    private LocalDateTime timestamp;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getTreatmentName() { return treatmentName; }
    public void setTreatmentName(String treatmentName) { this.treatmentName = treatmentName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
