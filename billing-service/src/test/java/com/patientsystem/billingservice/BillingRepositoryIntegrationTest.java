package com.patientsystem.billingservice;

import com.patientsystem.billingservice.model.BillingAccount;
import com.patientsystem.billingservice.model.Charge;
import com.patientsystem.billingservice.repository.BillingAccountRepository;
import com.patientsystem.billingservice.repository.ChargeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BillingRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    BillingAccountRepository billingAccountRepository;

    @Autowired
    ChargeRepository chargeRepository;

    @BeforeEach
    void cleanup() {
        chargeRepository.deleteAll();
        billingAccountRepository.deleteAll();
    }

    private BillingAccount saveAccount(String patientId, String orgId) {
        BillingAccount a = new BillingAccount();
        a.setPatientId(patientId);
        a.setPatientName("Test Patient");
        a.setPatientEmail("test@example.com");
        a.setBalance(BigDecimal.ZERO);
        a.setOrganizationId(orgId);
        return billingAccountRepository.save(a);
    }

    @Test
    void findByPatientIdAndOrgId_returnsAccount() {
        saveAccount("patient-1", "org-1");

        Optional<BillingAccount> found =
                billingAccountRepository.findByPatientIdAndOrganizationId("patient-1", "org-1");

        assertThat(found).isPresent();
        assertThat(found.get().getPatientId()).isEqualTo("patient-1");
    }

    @Test
    void findByPatientIdAndOrgId_returnsEmpty_forWrongOrg() {
        saveAccount("patient-1", "org-1");

        Optional<BillingAccount> found =
                billingAccountRepository.findByPatientIdAndOrganizationId("patient-1", "org-2");

        assertThat(found).isEmpty();
    }

    @Test
    void findAllByBillingAccountId_returnsAllCharges() {
        BillingAccount account = saveAccount("patient-2", "org-1");

        Charge c1 = new Charge();
        c1.setBillingAccountId(account.getId());
        c1.setTreatmentName("Consultation");
        c1.setTreatmentCategory("General");
        c1.setPrice(new BigDecimal("100.00"));
        c1.setTimestamp(LocalDateTime.now());

        Charge c2 = new Charge();
        c2.setBillingAccountId(account.getId());
        c2.setTreatmentName("Blood Test");
        c2.setTreatmentCategory("Lab");
        c2.setPrice(new BigDecimal("50.00"));
        c2.setTimestamp(LocalDateTime.now().minusMinutes(1));

        chargeRepository.save(c1);
        chargeRepository.save(c2);

        List<Charge> charges = chargeRepository.findAllByBillingAccountId(account.getId());

        assertThat(charges).hasSize(2);
        assertThat(charges).extracting(Charge::getTreatmentName)
                .containsExactlyInAnyOrder("Consultation", "Blood Test");
    }

    @Test
    void findAllByBillingAccountId_returnsEmpty_whenNoCharges() {
        BillingAccount account = saveAccount("patient-3", "org-1");

        List<Charge> charges = chargeRepository.findAllByBillingAccountId(account.getId());

        assertThat(charges).isEmpty();
    }
}
