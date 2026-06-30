package com.patientsystem.billingservice.repository;

import com.patientsystem.billingservice.model.Charge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChargeRepository extends JpaRepository<Charge, UUID> {
    List<Charge> findAllByBillingAccountId(UUID billingAccountId);
}
