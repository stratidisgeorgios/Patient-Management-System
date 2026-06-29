package com.patientsystem.billingservice.dto;

import java.math.BigDecimal;
import java.util.List;

public class BillingResponseDTO {
    private String patientId;
    private String patientName;
    private String patientEmail;
    private BigDecimal balance;
    private List<ChargeResponseDTO> charges;

    public BillingResponseDTO(String patientId, String patientName, String patientEmail, BigDecimal balance, List<ChargeResponseDTO> charges) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientEmail = patientEmail;
        this.balance = balance;
        this.charges = charges;
    }

    public String getPatientId() { return patientId; }
    public String getPatientName() { return patientName; }
    public String getPatientEmail() { return patientEmail; }
    public BigDecimal getBalance() { return balance; }
    public List<ChargeResponseDTO> getCharges() { return charges; }
}
