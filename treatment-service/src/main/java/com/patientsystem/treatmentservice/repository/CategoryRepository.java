package com.patientsystem.treatmentservice.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.patientsystem.treatmentservice.model.Category;
import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    boolean existsByNameAndOrganizationId(String name, String organizationId);
    List<Category> findAllByOrganizationId(String organizationId);
}
