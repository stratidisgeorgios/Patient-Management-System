package com.patientsystem.billingservice.service;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

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

    public void removeCharge(String patientId, UUID chargeId) {
        BillingAccount billingAccount = getAccount(patientId);
        Charge charge = chargeRepository.findById(chargeId)
                .orElseThrow(() -> new RuntimeException("Charge not found for ID: " + chargeId));
        if (!charge.getBillingAccountId().equals(billingAccount.getId())) {
            throw new RuntimeException("Charge does not belong to the specified billing account");
        }
        billingAccount.setBalance(billingAccount.getBalance().subtract(charge.getPrice()));
        billingAccountRepository.save(billingAccount);
        chargeRepository.delete(charge);
    }

    public void deleteAccount(String patientId) {
        BillingAccount billingAccount = getAccount(patientId);
        List<Charge> charges = chargeRepository.findAllByBillingAccountId(billingAccount.getId());
        chargeRepository.deleteAll(charges);
        billingAccountRepository.delete(billingAccount);
    }

    public byte[] generateInvoice(String patientId) {
        BillingResponseDTO billingInfo = getBillingInfo(patientId);
        try {
            InputStream template = getClass().getResourceAsStream("/templates/invoice.jrxml");
            JasperReport jasperReport = JasperCompileManager.compileReport(template);

            Map<String, Object> params = new HashMap<>();
            params.put("PATIENT_NAME", billingInfo.getPatientName());
            params.put("PATIENT_EMAIL", billingInfo.getPatientEmail());
            params.put("PATIENT_ID", billingInfo.getPatientId());
            params.put("TOTAL_BALANCE", String.format("$%,.2f", billingInfo.getBalance()));
            params.put("INVOICE_DATE", LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            List<Map<String, Object>> rows = billingInfo.getCharges().stream()
                    .map(c -> {
                        Map<String, Object> row = new HashMap<>();
                        row.put("treatmentName", c.getTreatmentName());
                        row.put("treatmentCategory", c.getTreatmentCategory());
                        row.put("date", c.getTimestamp() != null ? c.getTimestamp().format(dateFormatter) : "");
                        row.put("amount", String.format("$%,.2f", c.getPrice()));
                        return row;
                    })
                    .toList();

            JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(rows);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, dataSource);
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (JRException e) {
            throw new RuntimeException("Failed to generate invoice", e);
        }
    }

}
