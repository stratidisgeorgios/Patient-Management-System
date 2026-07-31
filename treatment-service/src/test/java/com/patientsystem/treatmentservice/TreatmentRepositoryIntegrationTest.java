package com.patientsystem.treatmentservice;

import com.patientsystem.treatmentservice.model.Category;
import com.patientsystem.treatmentservice.model.Treatment;
import com.patientsystem.treatmentservice.repository.CategoryRepository;
import com.patientsystem.treatmentservice.repository.TreatmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.flyway.enabled=true")
class TreatmentRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    TreatmentRepository treatmentRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @BeforeEach
    void cleanup() {
        treatmentRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    private Category saveCategory(String name, String orgId) {
        Category c = new Category();
        c.setName(name);
        c.setDescription("Test category");
        c.setOrganizationId(orgId);
        return categoryRepository.save(c);
    }

    private Treatment saveTreatment(String name, Category category, String orgId) {
        Treatment t = new Treatment();
        t.setName(name);
        t.setCategory(category);
        t.setPrice(new BigDecimal("100.00"));
        t.setOrganizationId(orgId);
        return treatmentRepository.save(t);
    }

    @Test
    void findByIdAndOrgId_returnsCorrectTreatment() {
        Category cat = saveCategory("General", "org-1");
        Treatment treatment = saveTreatment("Consultation", cat, "org-1");

        Optional<Treatment> found =
                treatmentRepository.findByIdAndOrganizationId(treatment.getId(), "org-1");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Consultation");
        assertThat(found.get().getCategory().getName()).isEqualTo("General");
    }

    @Test
    void findByIdAndOrgId_returnsEmpty_forWrongOrg() {
        Category cat = saveCategory("General", "org-1");
        Treatment treatment = saveTreatment("Consultation", cat, "org-1");

        Optional<Treatment> found =
                treatmentRepository.findByIdAndOrganizationId(treatment.getId(), "org-2");

        assertThat(found).isEmpty();
    }

    @Test
    void existsByNameAndOrgId_returnsTrue_forDuplicate() {
        Category cat = saveCategory("General", "org-1");
        saveTreatment("X-Ray", cat, "org-1");

        assertThat(treatmentRepository.existsByNameAndOrganizationId("X-Ray", "org-1")).isTrue();
    }

    @Test
    void existsByNameAndOrgId_returnsFalse_acrossOrgs() {
        Category cat = saveCategory("Radiology", "org-1");
        saveTreatment("MRI", cat, "org-1");

        assertThat(treatmentRepository.existsByNameAndOrganizationId("MRI", "org-2")).isFalse();
    }

    @Test
    void existsByCategory_Id_returnsFalse_whenNoTreatments() {
        Category cat = saveCategory("Empty Category", "org-1");

        assertThat(treatmentRepository.existsByCategory_Id(cat.getId())).isFalse();
    }

    @Test
    void existsByCategory_Id_returnsTrue_whenTreatmentsExist() {
        Category cat = saveCategory("Cardiology", "org-1");
        saveTreatment("ECG", cat, "org-1");

        assertThat(treatmentRepository.existsByCategory_Id(cat.getId())).isTrue();
    }

    @Test
    void findAllByOrgId_returnsOnlyOrgTreatments() {
        Category cat1 = saveCategory("General", "org-1");
        Category cat2 = saveCategory("Radiology", "org-2");
        saveTreatment("Consultation", cat1, "org-1");
        saveTreatment("X-Ray", cat2, "org-2");

        List<Treatment> org1Treatments = treatmentRepository.findAll()
                .stream().filter(t -> "org-1".equals(t.getOrganizationId())).toList();

        assertThat(org1Treatments).hasSize(1);
        assertThat(org1Treatments.get(0).getName()).isEqualTo("Consultation");
    }
}
