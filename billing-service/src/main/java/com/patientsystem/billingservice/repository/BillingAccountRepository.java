package com.patientsystem.billingservice.repository;

import com.patientsystem.billingservice.model.BillingAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingAccountRepository extends JpaRepository<BillingAccount, String> {
    Optional<BillingAccount> findByPatientId(String patientId);
}
