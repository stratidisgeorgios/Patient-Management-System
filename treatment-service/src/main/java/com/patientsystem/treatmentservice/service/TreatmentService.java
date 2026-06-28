package com.patientsystem.treatmentservice.service;
import java.util.List;
import org.springframework.stereotype.Service;
import com.patientsystem.treatmentservice.model.Treatment;
import com.patientsystem.treatmentservice.repository.TreatmentRepository;
import com.patientsystem.treatmentservice.kafka.KafkaProducer;
@Service
public class TreatmentService {
    private final TreatmentRepository treatmentRepository;
    private final KafkaProducer kafkaProducer;

    public TreatmentService(TreatmentRepository treatmentRepository, KafkaProducer kafkaProducer) {
        this.treatmentRepository = treatmentRepository;
        this.kafkaProducer = kafkaProducer;
    }

    public List<Treatment> getAllTreatments() {
        return treatmentRepository.findAll();
    }

    public Treatment getTreatmentById(String treatmentId) {
        return treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new RuntimeException("Treatment not found for ID: " + treatmentId));
    }

    public Treatment createTreatment(Treatment treatment) {
        if (treatmentRepository.existsByName(treatment.getName())) {
            throw new RuntimeException("Treatment with name " + treatment.getName() + " already exists.");
        }
        Treatment savedTreatment = treatmentRepository.save(treatment);
        kafkaProducer.sendTreatmentEvent(savedTreatment, "TreatmentCreated");
        return savedTreatment;
    }

    public Treatment updateTreatment(String treatmentId, Treatment treatment) {
        if (!treatmentRepository.existsById(treatmentId)) {
            throw new RuntimeException("Treatment not found for ID: " + treatmentId);
        }
        treatment.setId(treatmentId);
        Treatment savedTreatment = treatmentRepository.save(treatment);
        kafkaProducer.sendTreatmentEvent(savedTreatment, "TreatmentUpdated");
        return savedTreatment;
    }

    public void deleteTreatment(String treatmentId) {
        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new RuntimeException("Treatment not found for ID: " + treatmentId));
        treatmentRepository.deleteById(treatmentId);
        kafkaProducer.sendTreatmentEvent(treatment, "TreatmentDeleted");
    }
}
