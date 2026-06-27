package com.example.billingservice.repository;
import com.example.billingservice.model.TreatmentCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TreatmentCatalogRepository extends JpaRepository<TreatmentCatalog, String> {

}
