package com.patientsystem.billingservice.service;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.patientsystem.billingservice.model.BillingAccount;
import com.patientsystem.billingservice.repository.BillingAccountRepository;
import com.patientsystem.billingservice.repository.ChargeRepository;
import com.patientsystem.billingservice.model.Charge;
import com.patientsystem.billingservice.dto.BillingResponseDTO;
import com.patientsystem.billingservice.dto.ChargeResponseDTO;
import com.patientsystem.billingservice.grpc.BillingServiceGrpcClient;
import com.patientsystem.billingservice.kafka.KafkaProducer;
import com.patientsystem.treatment.grpc.TreatmentResponse;

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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            Color dark    = new Color(0x14, 0x14, 0x14);
            Color muted   = new Color(0x7A, 0x7A, 0x7A);
            Color rowLine = new Color(0xEE, 0xEE, 0xEE);
            Color divLine = new Color(0xC7, 0xC7, 0xC7);

            Font fClinic    = new Font(Font.HELVETICA, 18, Font.BOLD,   dark);
            Font fInvoice   = new Font(Font.HELVETICA, 22, Font.BOLD,   dark);
            Font fSubtitle  = new Font(Font.HELVETICA,  9, Font.NORMAL, muted);
            Font fLabel     = new Font(Font.HELVETICA,  7, Font.BOLD,   muted);
            Font fName      = new Font(Font.HELVETICA, 13, Font.BOLD,   dark);
            Font fInfo      = new Font(Font.HELVETICA,  9, Font.NORMAL, muted);
            Font fThHead    = new Font(Font.HELVETICA,  8, Font.BOLD,   Color.WHITE);
            Font fTdDark    = new Font(Font.HELVETICA,  8, Font.NORMAL, dark);
            Font fTdMuted   = new Font(Font.HELVETICA,  8, Font.NORMAL, muted);
            Font fTotal     = new Font(Font.HELVETICA, 11, Font.BOLD,   dark);

            // ── Header ──────────────────────────────────────────────────────────
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setSpacingAfter(4);

            PdfPCell clinicCell = new PdfPCell();
            clinicCell.setBorder(Rectangle.NO_BORDER);
            clinicCell.setPaddingBottom(6);
            clinicCell.addElement(new Paragraph("Patient System", fClinic));
            clinicCell.addElement(new Paragraph("Medical Patient Management System", fSubtitle));
            header.addCell(clinicCell);

            PdfPCell invCell = new PdfPCell();
            invCell.setBorder(Rectangle.NO_BORDER);
            invCell.setPaddingBottom(6);
            Paragraph invTitle = new Paragraph("INVOICE", fInvoice);
            invTitle.setAlignment(Element.ALIGN_RIGHT);
            invCell.addElement(invTitle);
            Paragraph invDate = new Paragraph("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")), fSubtitle);
            invDate.setAlignment(Element.ALIGN_RIGHT);
            invCell.addElement(invDate);
            header.addCell(invCell);

            document.add(header);

            // ── Divider ──────────────────────────────────────────────────────────
            PdfPTable divider = new PdfPTable(1);
            divider.setWidthPercentage(100);
            divider.setSpacingAfter(12);
            PdfPCell divCell = new PdfPCell(new Phrase(" "));
            divCell.setBorder(Rectangle.BOTTOM);
            divCell.setBorderColorBottom(divLine);
            divCell.setBorderWidthBottom(0.5f);
            divider.addCell(divCell);
            document.add(divider);

            // ── Bill To ──────────────────────────────────────────────────────────
            Paragraph billTo = new Paragraph("BILL TO", fLabel);
            billTo.setSpacingAfter(4);
            document.add(billTo);
            document.add(new Paragraph(billingInfo.getPatientName(), fName));
            document.add(new Paragraph(billingInfo.getPatientEmail(), fInfo));
            document.add(new Paragraph("Patient ID: " + billingInfo.getPatientId(), fInfo));

            // ── Charges table ─────────────────────────────────────────────────────
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3.2f, 2.4f, 2.2f, 1.8f});
            table.setSpacingBefore(16);

            int[] aligns = {Element.ALIGN_LEFT, Element.ALIGN_LEFT, Element.ALIGN_LEFT, Element.ALIGN_RIGHT};
            for (int i = 0; i < 4; i++) {
                String[] hdrs = {"Treatment", "Category", "Date", "Amount"};
                PdfPCell c = new PdfPCell(new Phrase(hdrs[i], fThHead));
                c.setBackgroundColor(dark);
                c.setBorder(Rectangle.NO_BORDER);
                c.setPadding(7);
                c.setHorizontalAlignment(aligns[i]);
                table.addCell(c);
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            for (ChargeResponseDTO charge : billingInfo.getCharges()) {
                String[] vals = {
                    charge.getTreatmentName(),
                    charge.getTreatmentCategory(),
                    charge.getTimestamp() != null ? charge.getTimestamp().format(fmt) : "",
                    String.format("$%,.2f", charge.getPrice())
                };
                Font[] fonts = {fTdDark, fTdMuted, fTdMuted, fTdDark};
                for (int i = 0; i < 4; i++) {
                    PdfPCell c = new PdfPCell(new Phrase(vals[i], fonts[i]));
                    c.setBorder(Rectangle.BOTTOM);
                    c.setBorderColorBottom(rowLine);
                    c.setBorderWidthBottom(0.5f);
                    c.setPadding(7);
                    c.setHorizontalAlignment(aligns[i]);
                    table.addCell(c);
                }
            }
            document.add(table);

            // ── Total ─────────────────────────────────────────────────────────────
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(100);
            totalTable.setWidths(new float[]{4, 2});
            totalTable.setSpacingBefore(0);

            PdfPCell totalLabel = new PdfPCell(new Phrase("Total Balance:", fTotal));
            totalLabel.setBorder(Rectangle.TOP);
            totalLabel.setBorderColorTop(dark);
            totalLabel.setBorderWidthTop(1f);
            totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalLabel.setPadding(8);
            totalTable.addCell(totalLabel);

            PdfPCell totalValue = new PdfPCell(new Phrase(String.format("$%,.2f", billingInfo.getBalance()), fTotal));
            totalValue.setBorder(Rectangle.TOP);
            totalValue.setBorderColorTop(dark);
            totalValue.setBorderWidthTop(1f);
            totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalValue.setPadding(8);
            totalTable.addCell(totalValue);

            document.add(totalTable);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate invoice", e);
        } finally {
            document.close();
        }
        return baos.toByteArray();
    }
}
