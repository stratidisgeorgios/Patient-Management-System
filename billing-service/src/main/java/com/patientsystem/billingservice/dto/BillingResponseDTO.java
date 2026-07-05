package com.patientsystem.billingservice.dto;

import java.util.List;

public class BillingResponseDTO {
    private String patientId;
    private String patientName;
    private String patientEmail;
    private String balance;
    private List<ChargeResponseDTO> charges;

    public BillingResponseDTO(String patientId, String patientName, String patientEmail, String balance, List<ChargeResponseDTO> charges) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientEmail = patientEmail;
        this.balance = balance;
        this.charges = charges;
    }

    public String getPatientId() { return patientId; }
    public String getPatientName() { return patientName; }
    public String getPatientEmail() { return patientEmail; }
    public String getBalance() { return balance; }
    public List<ChargeResponseDTO> getCharges() { return charges; }
}
