package com.example.billingservice.repository;

import com.example.billingservice.model.Charge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChargeRepository extends JpaRepository<Charge, String> {
    List<Charge> findAllByBillingAccountId(String billingAccountId);
}
