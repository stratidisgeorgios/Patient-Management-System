package com.patientsystem.treatmentservice.repository;
import com.patientsystem.treatmentservice.model.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface TreatmentRepository extends JpaRepository<Treatment, UUID> {
    boolean existsByName(String name);
    boolean existsByCategory_Id(UUID categoryId);
}
