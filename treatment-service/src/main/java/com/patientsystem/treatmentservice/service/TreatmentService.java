package com.patientsystem.treatmentservice.service;
import java.util.List;
import org.springframework.stereotype.Service;
import com.patientsystem.treatmentservice.dto.TreatmentRequestDTO;
import com.patientsystem.treatmentservice.dto.TreatmentResponseDTO;
import com.patientsystem.treatmentservice.model.Category;
import com.patientsystem.treatmentservice.model.Treatment;
import com.patientsystem.treatmentservice.repository.CategoryRepository;
import com.patientsystem.treatmentservice.repository.TreatmentRepository;
import com.patientsystem.treatmentservice.kafka.KafkaProducer;

@Service
public class TreatmentService {
    private final TreatmentRepository treatmentRepository;
    private final CategoryRepository categoryRepository;
    private final KafkaProducer kafkaProducer;

    public TreatmentService(TreatmentRepository treatmentRepository, CategoryRepository categoryRepository, KafkaProducer kafkaProducer) {
        this.treatmentRepository = treatmentRepository;
        this.categoryRepository = categoryRepository;
        this.kafkaProducer = kafkaProducer;
    }

    private TreatmentResponseDTO toDTO(Treatment treatment) {
        TreatmentResponseDTO dto = new TreatmentResponseDTO();
        dto.setId(treatment.getId());
        dto.setName(treatment.getName());
        dto.setPrice(treatment.getPrice());
        TreatmentResponseDTO.CategoryDTO categoryDTO = new TreatmentResponseDTO.CategoryDTO();
        categoryDTO.setId(treatment.getCategory().getId());
        categoryDTO.setName(treatment.getCategory().getName());
        categoryDTO.setDescription(treatment.getCategory().getDescription());
        dto.setCategory(categoryDTO);
        return dto;
    }

    public List<TreatmentResponseDTO> getAllTreatments() {
        return treatmentRepository.findAll().stream().map(this::toDTO).toList();
    }

    public TreatmentResponseDTO getTreatmentById(String treatmentId) {
        return toDTO(treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new RuntimeException("Treatment not found for ID: " + treatmentId)));
    }

    public TreatmentResponseDTO createTreatment(TreatmentRequestDTO request) {
        if (treatmentRepository.existsByName(request.getName())) {
            throw new RuntimeException("Treatment with name " + request.getName() + " already exists.");
        }
        Category category = categoryRepository.findById(request.getCategory())
                .orElseThrow(() -> new RuntimeException("Category not found for ID: " + request.getCategory()));
        Treatment treatment = new Treatment();
        treatment.setName(request.getName());
        treatment.setCategory(category);
        treatment.setPrice(request.getPrice());
        Treatment savedTreatment = treatmentRepository.save(treatment);
        kafkaProducer.sendTreatmentEvent(savedTreatment, "TreatmentCreated");
        return toDTO(savedTreatment);
    }

    public TreatmentResponseDTO updateTreatment(String treatmentId, TreatmentRequestDTO request) {
        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new RuntimeException("Treatment not found for ID: " + treatmentId));
        Category category = categoryRepository.findById(request.getCategory())
                .orElseThrow(() -> new RuntimeException("Category not found for ID: " + request.getCategory()));
        treatment.setName(request.getName());
        treatment.setCategory(category);
        treatment.setPrice(request.getPrice());
        Treatment savedTreatment = treatmentRepository.save(treatment);
        kafkaProducer.sendTreatmentEvent(savedTreatment, "TreatmentUpdated");
        return toDTO(savedTreatment);
    }

    public void deleteTreatment(String treatmentId) {
        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new RuntimeException("Treatment not found for ID: " + treatmentId));
        treatmentRepository.deleteById(treatmentId);
        kafkaProducer.sendTreatmentEvent(treatment, "TreatmentDeleted");
    }
}
