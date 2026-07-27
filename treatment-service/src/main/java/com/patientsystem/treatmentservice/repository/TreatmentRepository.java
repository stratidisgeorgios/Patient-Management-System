package com.patientsystem.treatmentservice.repository;
import com.patientsystem.treatmentservice.model.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TreatmentRepository extends JpaRepository<Treatment, UUID> {
    boolean existsByNameAndOrganizationId(String name, String organizationId);
    boolean existsByCategory_Id(UUID categoryId);
    List<Treatment> findAllByOrganizationId(String organizationId);
    Optional<Treatment> findByIdAndOrganizationId(UUID id, String organizationId);
}
