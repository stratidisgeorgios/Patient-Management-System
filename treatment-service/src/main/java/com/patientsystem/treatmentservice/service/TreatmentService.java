package com.patientsystem.treatmentservice.service;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.patientsystem.treatmentservice.dto.TreatmentRequestDTO;
import com.patientsystem.treatmentservice.dto.TreatmentResponseDTO;
import com.patientsystem.treatmentservice.mapper.TreatmentMapper;
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

    public TreatmentResponseDTO getTreatmentById(UUID treatmentId) {
        return TreatmentMapper.toDTO(treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new RuntimeException("Treatment not found for ID: " + treatmentId)));
    }

    public TreatmentResponseDTO createTreatment(TreatmentRequestDTO request) {
        if (treatmentRepository.existsByName(request.getName())) {
            throw new RuntimeException("Treatment with name " + request.getName() + " already exists.");
        }
        Category category = categoryRepository.findById(UUID.fromString(request.getCategory()))
                .orElseThrow(() -> new RuntimeException("Category not found for ID: " + request.getCategory()));
        Treatment treatment = new Treatment();
        treatment.setName(request.getName());
        treatment.setCategory(category);
        treatment.setPrice(request.getPrice());
        Treatment savedTreatment = treatmentRepository.save(treatment);
        kafkaProducer.sendTreatmentEvent(savedTreatment, "TreatmentCreated");
        return TreatmentMapper.toDTO(savedTreatment);
    }

    public TreatmentResponseDTO updateTreatment(UUID treatmentId, TreatmentRequestDTO request) {
        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new RuntimeException("Treatment not found for ID: " + treatmentId));
        Category category = categoryRepository.findById(UUID.fromString(request.getCategory()))
                .orElseThrow(() -> new RuntimeException("Category not found for ID: " + request.getCategory()));
        treatment.setName(request.getName());
        treatment.setCategory(category);
        treatment.setPrice(request.getPrice());
        Treatment savedTreatment = treatmentRepository.save(treatment);
        kafkaProducer.sendTreatmentEvent(savedTreatment, "TreatmentUpdated");
        return TreatmentMapper.toDTO(savedTreatment);
    }

    public void deleteTreatment(UUID treatmentId) {
        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new RuntimeException("Treatment not found for ID: " + treatmentId));
        treatmentRepository.deleteById(treatmentId);
        kafkaProducer.sendTreatmentEvent(treatment, "TreatmentDeleted");
    }
}
