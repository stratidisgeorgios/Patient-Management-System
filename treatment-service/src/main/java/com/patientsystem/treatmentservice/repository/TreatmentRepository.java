package com.patientsystem.treatmentservice.repository;
import com.patientsystem.treatmentservice.model.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TreatmentRepository extends JpaRepository<Treatment, String> {
    boolean existsByName(String name);
    boolean existsByCategory_Id(String categoryId);
}
