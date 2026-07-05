package com.patientsystem.billingservice.service;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;
import com.patientsystem.billingservice.model.BillingAccount;
import com.patientsystem.billingservice.repository.BillingAccountRepository;
import com.patientsystem.billingservice.repository.ChargeRepository;
import com.patientsystem.billingservice.model.Charge;
import com.patientsystem.billingservice.dto.BillingResponseDTO;
import com.patientsystem.billingservice.dto.ChargeResponseDTO;
import com.patientsystem.billingservice.mapper.BillingMapper;
import com.patientsystem.billingservice.grpc.BillingServiceGrpcClient;
import com.patientsystem.billingservice.grpc.PatientServiceGrpcClient;
import com.patientsystem.billingservice.kafka.KafkaProducer;
import com.patientsystem.patient.grpc.PatientResponse;
import com.patientsystem.treatment.grpc.TreatmentResponse;

@Service
public class BillingService {

    private final BillingAccountRepository billingAccountRepository;
    private final ChargeRepository chargeRepository;
    private final KafkaProducer kafkaProducer;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final PatientServiceGrpcClient patientServiceGrpcClient;

    public BillingService(BillingAccountRepository billingAccountRepository, ChargeRepository chargeRepository, BillingServiceGrpcClient billingServiceGrpcClient, KafkaProducer kafkaProducer, PatientServiceGrpcClient patientServiceGrpcClient) {
        this.billingAccountRepository = billingAccountRepository;
        this.chargeRepository = chargeRepository;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
        this.kafkaProducer = kafkaProducer;
        this.patientServiceGrpcClient = patientServiceGrpcClient;
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
        List<Charge> charges = chargeRepository.findAllByBillingAccountId(account.getId());
        return BillingMapper.toDTO(account, charges);
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
    public BillingAccount updateAccount(String patientId, String name, String email) {
        BillingAccount billingAccount = getAccount(patientId);
        billingAccount.setPatientName(name);
        billingAccount.setPatientEmail(email);
        return billingAccountRepository.save(billingAccount);
    }

    public void deleteAccount(String patientId) {
        BillingAccount billingAccount = getAccount(patientId);
        List<Charge> charges = chargeRepository.findAllByBillingAccountId(billingAccount.getId());
        chargeRepository.deleteAll(charges);
        billingAccountRepository.delete(billingAccount);
    }

    public byte[] generateInvoice(String patientId) {
        BillingResponseDTO billingInfo = getBillingInfo(patientId);
        PatientResponse patient = patientServiceGrpcClient.getPatient(patientId);
        try {
            InputStream is = getClass().getResourceAsStream("/templates/invoice.html");
            String template = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            StringBuilder rows = new StringBuilder();
            for (ChargeResponseDTO charge : billingInfo.getCharges()) {
                rows.append("<tr>")
                    .append("<td>").append(xml(charge.getTreatmentName())).append("</td>")
                    .append("<td class=\"muted\">").append(xml(charge.getTreatmentCategory())).append("</td>")
                    .append("<td class=\"muted\">").append((charge.getTimestamp() != null && !charge.getTimestamp().isEmpty()) ? LocalDateTime.parse(charge.getTimestamp()).format(fmt) : "").append("</td>")
                    .append("<td class=\"right\">").append(String.format("$%,.2f", new BigDecimal(charge.getPrice()))).append("</td>")
                    .append("</tr>");
            }

            String dob = patient.getDateOfBirth().isBlank() ? "" :
                    LocalDate.parse(patient.getDateOfBirth(), dateFmt).format(fmt);
            String registered = patient.getRegisteredDate().isBlank() ? "" :
                    LocalDate.parse(patient.getRegisteredDate(), dateFmt).format(fmt);

            String html = template
                    .replace("{{PATIENT_NAME}}", xml(patient.getName()))
                    .replace("{{PATIENT_EMAIL}}", xml(patient.getEmail()))
                    .replace("{{PATIENT_GENDER}}", xml(patient.getGender()))
                    .replace("{{PATIENT_DOB}}", xml(dob))
                    .replace("{{PATIENT_REGISTERED}}", xml(registered))
                    .replace("{{PATIENT_ADDRESS}}", xml(patient.getAddress()))
                    .replace("{{INVOICE_DATE}}", LocalDate.now().format(fmt))
                    .replace("{{TOTAL_BALANCE}}", String.format("$%,.2f", new BigDecimal(billingInfo.getBalance())))
                    .replace("{{CHARGES_ROWS}}", rows.toString());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate invoice", e);
        }
    }

    private String xml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
