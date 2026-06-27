package com.example.billingservice.service;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import com.example.billingservice.model.BillingAccount;
import com.example.billingservice.repository.BillingAccountRepository;
import com.example.billingservice.repository.ChargeRepository;
import com.example.billingservice.model.Charge;
import com.example.billingservice.dto.BillingResponseDTO;
import com.example.billingservice.dto.ChargeResponse;
import com.example.billingservice.model.TreatmentCatalog;
import com.example.billingservice.repository.TreatmentCatalogRepository;
import com.example.billingservice.kafka.KafkaProducer;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BillingService {

    private final BillingAccountRepository billingAccountRepository;
    private final ChargeRepository chargeRepository;
    private final TreatmentCatalogRepository treatmentCatalogRepository;
    private final KafkaProducer kafkaProducer;

    public BillingService(BillingAccountRepository billingAccountRepository, ChargeRepository chargeRepository, TreatmentCatalogRepository treatmentCatalogRepository, KafkaProducer kafkaProducer) {
        this.billingAccountRepository = billingAccountRepository;
        this.chargeRepository = chargeRepository;
        this.treatmentCatalogRepository = treatmentCatalogRepository;
        this.kafkaProducer = kafkaProducer;
    }


    public void createAccount(String patientId, String name, String email) {
        BillingAccount billingAccount = new BillingAccount();
        billingAccount.setPatientId(patientId);
        billingAccount.setPatientName(name);
        billingAccount.setPatientEmail(email);
        billingAccount.setBalance(BigDecimal.ZERO);
        billingAccountRepository.save(billingAccount);
    }

    public BillingAccount getAccount(String patientId) {
        return billingAccountRepository.findByPatientId(patientId)
                .orElseThrow(() -> new RuntimeException("Billing account not found for patient ID: " + patientId));
    }

    public void addCharge(String patientId, String treatmentId, BigDecimal price) {
        BillingAccount billingAccount = getAccount(patientId);
        TreatmentCatalog treatmentCatalog = treatmentCatalogRepository.findById(treatmentId)
                .orElseThrow(() -> new RuntimeException("Treatment not found for ID: " + treatmentId));
        Charge charge = new Charge();
        charge.setBillingAccountId(billingAccount.getId());
        charge.setTreatmentCatalog(treatmentCatalog);
        charge.setPrice(price);
        charge.setTimestamp(LocalDateTime.now());
        chargeRepository.save(charge);
        billingAccount.setBalance(billingAccount.getBalance().add(price));
        billingAccountRepository.save(billingAccount);
        kafkaProducer.sendChargeEvent(patientId, treatmentCatalog.getName(), treatmentCatalog.getCategory(), price.toString(), LocalDateTime.now().toString());
    }

    public BillingResponseDTO getBillingInfo(String patientId) {
        BillingAccount account = getAccount(patientId);
        List<ChargeResponse> charges = chargeRepository.findAllByBillingAccountId(account.getId())
                .stream()
                .map(c -> new ChargeResponse(c.getId(), c.getTreatmentCatalog().getName(), c.getTreatmentCatalog().getCategory(), c.getPrice(), c.getTimestamp()))
                .toList();
        return new BillingResponseDTO(account.getPatientId(), account.getPatientName(), account.getPatientEmail(), account.getBalance(), charges);
    }
}

