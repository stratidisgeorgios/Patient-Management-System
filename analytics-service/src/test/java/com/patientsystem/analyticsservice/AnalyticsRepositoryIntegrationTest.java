package com.patientsystem.analyticsservice;

import com.patientsystem.analyticsservice.model.ChargeEvent;
import com.patientsystem.analyticsservice.model.PatientEvent;
import com.patientsystem.analyticsservice.repository.ChargeEventRepository;
import com.patientsystem.analyticsservice.repository.PatientEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.flyway.enabled=true")
class AnalyticsRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    PatientEventRepository patientEventRepository;

    @Autowired
    ChargeEventRepository chargeEventRepository;

    @BeforeEach
    void cleanup() {
        chargeEventRepository.deleteAll();
        patientEventRepository.deleteAll();
    }

    private PatientEvent patientEvent(String patientId, String eventType, String gender, String dob, LocalDateTime ts) {
        PatientEvent e = new PatientEvent();
        e.setPatientId(patientId);
        e.setEventType(eventType);
        e.setGender(gender);
        e.setDateOfBirth(dob);
        e.setOrganizationId("org-1");
        e.setTimestamp(ts);
        return e;
    }

    @Test
    void totalCreated_countsOnlyCreatedEvents() {
        patientEventRepository.save(patientEvent("p1", "PatientCreated", "MALE", "1990-01-01", LocalDateTime.now()));
        patientEventRepository.save(patientEvent("p2", "PatientCreated", "FEMALE", "1985-06-15", LocalDateTime.now().minusSeconds(1)));
        patientEventRepository.save(patientEvent("p1", "PatientDeleted", "MALE", "1990-01-01", LocalDateTime.now().plusSeconds(1)));

        assertThat(patientEventRepository.totalCreated()).isEqualTo(2L);
        assertThat(patientEventRepository.totalDeleted()).isEqualTo(1L);
    }

    @Test
    void countCreatedPerMonth_groupsByMonth() {
        int year = LocalDateTime.now().getYear();
        patientEventRepository.save(patientEvent("p1", "PatientCreated", "MALE", "1990-01-01",
                LocalDateTime.of(year, 1, 10, 0, 0)));
        patientEventRepository.save(patientEvent("p2", "PatientCreated", "FEMALE", "1985-06-15",
                LocalDateTime.of(year, 1, 20, 0, 0)));
        patientEventRepository.save(patientEvent("p3", "PatientCreated", "MALE", "1992-03-22",
                LocalDateTime.of(year, 3, 5, 0, 0)));

        List<Object[]> rows = patientEventRepository.countCreatedPerMonth(year);

        assertThat(rows).hasSize(2);
        // January should have 2, March should have 1
        Map<Double, Long> byMonth = Map.of(
                (double) ((Number) rows.get(0)[0]).intValue(), ((Number) rows.get(0)[1]).longValue(),
                (double) ((Number) rows.get(1)[0]).intValue(), ((Number) rows.get(1)[1]).longValue()
        );
        assertThat(byMonth.get(1.0)).isEqualTo(2L);
        assertThat(byMonth.get(3.0)).isEqualTo(1L);
    }

    @Test
    void getAnnualRevenue_sumsCharges() {
        LocalDateTime now = LocalDateTime.now();
        ChargeEvent c1 = new ChargeEvent();
        c1.setPatientId("p1");
        c1.setTreatmentName("Consultation");
        c1.setCategory("General");
        c1.setPrice("100.00");
        c1.setOrganizationId("org-1");
        c1.setTimestamp(now);

        ChargeEvent c2 = new ChargeEvent();
        c2.setPatientId("p2");
        c2.setTreatmentName("X-Ray");
        c2.setCategory("Radiology");
        c2.setPrice("250.50");
        c2.setOrganizationId("org-1");
        c2.setTimestamp(now.minusSeconds(1));

        chargeEventRepository.save(c1);
        chargeEventRepository.save(c2);

        BigDecimal revenue = chargeEventRepository.getAnnualRevenue(now.getYear());

        assertThat(revenue).isEqualByComparingTo(new BigDecimal("350.50"));
    }

    @Test
    void getAnnualRevenue_returnsNull_whenNoCharges() {
        BigDecimal revenue = chargeEventRepository.getAnnualRevenue(2000);
        assertThat(revenue).isNull();
    }
}
