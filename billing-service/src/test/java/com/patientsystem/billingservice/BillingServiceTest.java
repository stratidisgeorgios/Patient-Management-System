package com.patientsystem.billingservice;

import com.patientsystem.billingservice.grpc.BillingServiceGrpcClient;
import com.patientsystem.billingservice.grpc.PatientServiceGrpcClient;
import com.patientsystem.billingservice.kafka.KafkaProducer;
import com.patientsystem.billingservice.model.BillingAccount;
import com.patientsystem.billingservice.model.Charge;
import com.patientsystem.billingservice.repository.BillingAccountRepository;
import com.patientsystem.billingservice.repository.ChargeRepository;
import com.patientsystem.billingservice.service.BillingService;
import com.patientsystem.treatment.grpc.TreatmentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    BillingAccountRepository billingAccountRepository;
    @Mock
    ChargeRepository chargeRepository;
    @Mock
    KafkaProducer kafkaProducer;
    @Mock
    BillingServiceGrpcClient billingServiceGrpcClient;
    @Mock
    PatientServiceGrpcClient patientServiceGrpcClient;

    @InjectMocks
    BillingService billingService;

    private BillingAccount sampleAccount(UUID id, String patientId, String orgId) {
        BillingAccount a = new BillingAccount();
        a.setId(id);
        a.setPatientId(patientId);
        a.setPatientName("Bob");
        a.setPatientEmail("bob@example.com");
        a.setBalance(BigDecimal.ZERO);
        a.setOrganizationId(orgId);
        return a;
    }

    @Test
    void createAccount_savesAndReturnsAccount() {
        BillingAccount saved = sampleAccount(UUID.randomUUID(), "patient-1", "org-1");
        when(billingAccountRepository.save(any(BillingAccount.class))).thenReturn(saved);

        BillingAccount result = billingService.createAccount("patient-1", "Bob", "bob@example.com", "org-1");

        assertThat(result.getPatientId()).isEqualTo("patient-1");
        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(billingAccountRepository).save(any(BillingAccount.class));
    }

    @Test
    void addCharge_savesChargeAndUpdatesBalance() {
        UUID accountId = UUID.randomUUID();
        BillingAccount account = sampleAccount(accountId, "patient-1", "org-1");

        TreatmentResponse treatment = TreatmentResponse.newBuilder()
                .setId("treat-1")
                .setName("Consultation")
                .setCategory("General")
                .setPrice("100.00")
                .build();

        when(billingAccountRepository.findByPatientIdAndOrganizationId("patient-1", "org-1"))
                .thenReturn(Optional.of(account));
        when(billingServiceGrpcClient.getTreatment("treat-1")).thenReturn(treatment);
        when(chargeRepository.save(any(Charge.class))).thenReturn(new Charge());
        when(billingAccountRepository.save(any(BillingAccount.class))).thenReturn(account);

        billingService.addCharge("patient-1", "treat-1", "org-1");

        verify(chargeRepository).save(any(Charge.class));
        verify(kafkaProducer).sendChargeEvent(eq("patient-1"), eq("Consultation"), eq("General"), eq("100.00"), any(), eq("org-1"));
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void removeCharge_throws_whenChargeNotFound() {
        UUID chargeId = UUID.randomUUID();
        BillingAccount account = sampleAccount(UUID.randomUUID(), "patient-1", "org-1");
        when(billingAccountRepository.findByPatientIdAndOrganizationId("patient-1", "org-1"))
                .thenReturn(Optional.of(account));
        when(chargeRepository.findById(chargeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> billingService.removeCharge("patient-1", chargeId, "org-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Charge not found");
    }

    @Test
    void removeCharge_throws_whenChargeDoesNotBelongToAccount() {
        UUID accountId = UUID.randomUUID();
        UUID chargeId = UUID.randomUUID();
        BillingAccount account = sampleAccount(accountId, "patient-1", "org-1");

        Charge charge = new Charge();
        charge.setId(chargeId);
        charge.setBillingAccountId(UUID.randomUUID()); // different account
        charge.setPrice(new BigDecimal("50.00"));

        when(billingAccountRepository.findByPatientIdAndOrganizationId("patient-1", "org-1"))
                .thenReturn(Optional.of(account));
        when(chargeRepository.findById(chargeId)).thenReturn(Optional.of(charge));

        assertThatThrownBy(() -> billingService.removeCharge("patient-1", chargeId, "org-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void removeCharge_subtractsFromBalance_andDeletes() {
        UUID accountId = UUID.randomUUID();
        UUID chargeId = UUID.randomUUID();
        BillingAccount account = sampleAccount(accountId, "patient-1", "org-1");
        account.setBalance(new BigDecimal("200.00"));

        Charge charge = new Charge();
        charge.setId(chargeId);
        charge.setBillingAccountId(accountId);
        charge.setPrice(new BigDecimal("50.00"));

        when(billingAccountRepository.findByPatientIdAndOrganizationId("patient-1", "org-1"))
                .thenReturn(Optional.of(account));
        when(chargeRepository.findById(chargeId)).thenReturn(Optional.of(charge));
        when(billingAccountRepository.save(any())).thenReturn(account);

        billingService.removeCharge("patient-1", chargeId, "org-1");

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("150.00"));
        verify(chargeRepository).delete(charge);
    }

    @Test
    void updateAccount_updatesNameAndEmail() {
        UUID accountId = UUID.randomUUID();
        BillingAccount account = sampleAccount(accountId, "patient-1", "org-1");
        when(billingAccountRepository.findByPatientIdAndOrganizationId("patient-1", "org-1"))
                .thenReturn(Optional.of(account));
        when(billingAccountRepository.save(any())).thenReturn(account);

        billingService.updateAccount("patient-1", "Bob Updated", "bob.updated@example.com", "org-1");

        assertThat(account.getPatientName()).isEqualTo("Bob Updated");
        assertThat(account.getPatientEmail()).isEqualTo("bob.updated@example.com");
    }

    @Test
    void deleteAccount_deletesChargesAndAccount() {
        UUID accountId = UUID.randomUUID();
        BillingAccount account = sampleAccount(accountId, "patient-1", "org-1");
        List<Charge> charges = List.of(new Charge(), new Charge());

        when(billingAccountRepository.findByPatientIdAndOrganizationId("patient-1", "org-1"))
                .thenReturn(Optional.of(account));
        when(chargeRepository.findAllByBillingAccountId(accountId)).thenReturn(charges);

        billingService.deleteAccount("patient-1", "org-1");

        verify(chargeRepository).deleteAll(charges);
        verify(billingAccountRepository).delete(account);
    }
}
