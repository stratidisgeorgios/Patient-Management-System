package com.patientsystem.billingservice.service;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import com.patientsystem.billingservice.model.BillingAccount;
import com.patientsystem.billingservice.repository.BillingAccountRepository;
import com.patientsystem.billingservice.repository.ChargeRepository;
import com.patientsystem.billingservice.model.Charge;
import com.patientsystem.billingservice.dto.BillingResponseDTO;
import com.patientsystem.billingservice.dto.ChargeResponseDTO;
import com.patientsystem.billingservice.grpc.BillingServiceGrpcClient;
import com.patientsystem.billingservice.kafka.KafkaProducer;
import com.patientsystem.treatment.grpc.TreatmentResponse;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BillingService {

    private final BillingAccountRepository billingAccountRepository;
    private final ChargeRepository chargeRepository;
    private final KafkaProducer kafkaProducer;
    private final BillingServiceGrpcClient billingServiceGrpcClient;

    public BillingService(BillingAccountRepository billingAccountRepository, ChargeRepository chargeRepository, BillingServiceGrpcClient billingServiceGrpcClient, KafkaProducer kafkaProducer) {
        this.billingAccountRepository = billingAccountRepository;
        this.chargeRepository = chargeRepository;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
        this.kafkaProducer = kafkaProducer;
    }


    public BillingAccount createAccount(String patientId, String name, String email) {
        BillingAccount billingAccount = new BillingAccount();
        billingAccount.setPatientId(patientId);
        billingAccount.setPatientName(name);
        billingAccount.setPatientEmail(email);
        billingAccount.setBalance(BigDecimal.ZERO);
        return billingAccountRepository.save(billingAccount);
    }

    public BillingAccount getAccount(String patientId) {
        return billingAccountRepository.findByPatientId(patientId)
                .orElseThrow(() -> new RuntimeException("Billing account not found for patient ID: " + patientId));
    }

    public void addCharge(String patientId, String treatmentId) {
        BillingAccount billingAccount = getAccount(patientId);
        TreatmentResponse treatment = billingServiceGrpcClient.getTreatment(treatmentId);
        BigDecimal price = new BigDecimal(treatment.getPrice());
        Charge charge = new Charge();
        charge.setBillingAccountId(billingAccount.getId());
        charge.setTreatmentId(treatment.getId());
        charge.setTreatmentName(treatment.getName());
        charge.setTreatmentCategory(treatment.getCategory());
        charge.setPrice(price);
        charge.setTimestamp(LocalDateTime.now());
        chargeRepository.save(charge);
        billingAccount.setBalance(billingAccount.getBalance().add(price));
        billingAccountRepository.save(billingAccount);
        kafkaProducer.sendChargeEvent(patientId, treatment.getName(), treatment.getCategory(), treatment.getPrice(), LocalDateTime.now().toString());
    }

    public BillingResponseDTO getBillingInfo(String patientId) {
        BillingAccount account = getAccount(patientId);
        List<ChargeResponseDTO> charges = chargeRepository.findAllByBillingAccountId(account.getId())
                .stream()
                .map(c -> new ChargeResponseDTO(c.getId().toString(), c.getTreatmentId(), c.getTreatmentName(), c.getTreatmentCategory(), c.getPrice(), c.getTimestamp()))
                .toList();
        return new BillingResponseDTO(account.getPatientId(), account.getPatientName(), account.getPatientEmail(), account.getBalance(), charges);
    }
}
