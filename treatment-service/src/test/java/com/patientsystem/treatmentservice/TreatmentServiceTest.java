package com.patientsystem.treatmentservice;

import com.patientsystem.treatmentservice.dto.TreatmentRequestDTO;
import com.patientsystem.treatmentservice.kafka.KafkaProducer;
import com.patientsystem.treatmentservice.model.Category;
import com.patientsystem.treatmentservice.model.Treatment;
import com.patientsystem.treatmentservice.repository.CategoryRepository;
import com.patientsystem.treatmentservice.repository.TreatmentRepository;
import com.patientsystem.treatmentservice.service.CategoryService;
import com.patientsystem.treatmentservice.service.TreatmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceTest {

    @Mock
    TreatmentRepository treatmentRepository;
    @Mock
    CategoryRepository categoryRepository;
    @Mock
    KafkaProducer kafkaProducer;

    @InjectMocks
    TreatmentService treatmentService;
    @InjectMocks
    CategoryService categoryService;

    private Category sampleCategory(UUID id, String orgId) {
        Category c = new Category();
        c.setId(id);
        c.setName("General");
        c.setOrganizationId(orgId);
        return c;
    }

    private TreatmentRequestDTO sampleRequest(UUID categoryId) {
        TreatmentRequestDTO req = new TreatmentRequestDTO();
        req.setName("Consultation");
        req.setCategory(categoryId.toString());
        req.setPrice("150.00");
        return req;
    }

    // ── TreatmentService tests ──

    @Test
    void createTreatment_throws_onDuplicateName() {
        UUID catId = UUID.randomUUID();
        when(treatmentRepository.existsByNameAndOrganizationId("Consultation", "org-1")).thenReturn(true);

        assertThatThrownBy(() -> treatmentService.createTreatment(sampleRequest(catId), "org-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void createTreatment_throws_whenCategoryNotFound() {
        UUID catId = UUID.randomUUID();
        when(treatmentRepository.existsByNameAndOrganizationId("Consultation", "org-1")).thenReturn(false);
        when(categoryRepository.findById(catId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> treatmentService.createTreatment(sampleRequest(catId), "org-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void createTreatment_savesAndPublishesEvent() {
        UUID catId = UUID.randomUUID();
        UUID treatId = UUID.randomUUID();
        Category cat = sampleCategory(catId, "org-1");
        Treatment saved = new Treatment();
        saved.setId(treatId);
        saved.setName("Consultation");
        saved.setCategory(cat);
        saved.setPrice(new BigDecimal("150.00"));
        saved.setOrganizationId("org-1");

        when(treatmentRepository.existsByNameAndOrganizationId("Consultation", "org-1")).thenReturn(false);
        when(categoryRepository.findById(catId)).thenReturn(Optional.of(cat));
        when(treatmentRepository.save(any(Treatment.class))).thenReturn(saved);

        var result = treatmentService.createTreatment(sampleRequest(catId), "org-1");

        assertThat(result.getName()).isEqualTo("Consultation");
        verify(kafkaProducer).sendTreatmentEvent(saved, "TreatmentCreated", "org-1");
    }

    @Test
    void getTreatmentById_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(treatmentRepository.findByIdAndOrganizationId(id, "org-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> treatmentService.getTreatmentById(id, "org-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Treatment not found");
    }

    @Test
    void deleteTreatment_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(treatmentRepository.findByIdAndOrganizationId(id, "org-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> treatmentService.deleteTreatment(id, "org-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Treatment not found");
    }

    @Test
    void deleteTreatment_deletesAndPublishesEvent() {
        UUID id = UUID.randomUUID();
        Category cat = sampleCategory(UUID.randomUUID(), "org-1");
        Treatment treatment = new Treatment();
        treatment.setId(id);
        treatment.setName("Consultation");
        treatment.setCategory(cat);
        treatment.setOrganizationId("org-1");

        when(treatmentRepository.findByIdAndOrganizationId(id, "org-1")).thenReturn(Optional.of(treatment));

        treatmentService.deleteTreatment(id, "org-1");

        verify(treatmentRepository).deleteById(id);
        verify(kafkaProducer).sendTreatmentEvent(treatment, "TreatmentDeleted", "org-1");
    }

    // ── CategoryService tests ──

    @Test
    void createCategory_throws_onDuplicateName() {
        Category cat = sampleCategory(UUID.randomUUID(), "org-1");
        when(categoryRepository.existsByNameAndOrganizationId("General", "org-1")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(cat, "org-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createCategory_saves_whenNameIsUnique() {
        Category cat = sampleCategory(null, "org-1");
        Category saved = sampleCategory(UUID.randomUUID(), "org-1");
        when(categoryRepository.existsByNameAndOrganizationId("General", "org-1")).thenReturn(false);
        when(categoryRepository.save(cat)).thenReturn(saved);

        Category result = categoryService.createCategory(cat, "org-1");

        assertThat(result.getId()).isNotNull();
    }

    @Test
    void deleteCategory_throws_whenTreatmentsAssigned() {
        UUID catId = UUID.randomUUID();
        when(categoryRepository.existsById(catId)).thenReturn(true);
        when(treatmentRepository.existsByCategory_Id(catId)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(catId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot delete category");
    }
}
