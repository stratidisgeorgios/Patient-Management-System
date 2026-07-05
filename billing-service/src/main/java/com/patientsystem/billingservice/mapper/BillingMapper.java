package com.patientsystem.billingservice.mapper;

import com.patientsystem.billingservice.dto.BillingResponseDTO;
import com.patientsystem.billingservice.dto.ChargeResponseDTO;
import com.patientsystem.billingservice.model.BillingAccount;
import com.patientsystem.billingservice.model.Charge;

import java.util.List;

public class BillingMapper {

    public static ChargeResponseDTO toDTO(Charge charge) {
        return new ChargeResponseDTO(
                charge.getId().toString(),
                charge.getTreatmentId(),
                charge.getTreatmentName(),
                charge.getTreatmentCategory(),
                charge.getPrice().toString(),
                charge.getTimestamp() != null ? charge.getTimestamp().toString() : ""
        );
    }

    public static BillingResponseDTO toDTO(BillingAccount account, List<Charge> charges) {
        List<ChargeResponseDTO> chargeDTOs = charges.stream()
                .map(BillingMapper::toDTO)
                .toList();
        return new BillingResponseDTO(
                account.getPatientId(),
                account.getPatientName(),
                account.getPatientEmail(),
                account.getBalance().toString(),
                chargeDTOs
        );
    }
}
