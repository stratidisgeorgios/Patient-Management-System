package com.patientsystem.patientservice;

import com.patientsystem.patientservice.model.Gender;
import com.patientsystem.patientservice.model.Patient;
import com.patientsystem.patientservice.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PatientRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    PatientRepository patientRepository;

    private Patient buildPatient(String name, String email, String orgId) {
        Patient p = new Patient();
        p.setName(name);
        p.setEmail(email);
        p.setGender(Gender.FEMALE);
        p.setAddress("123 Test St");
        p.setDateOfBirth(LocalDate.of(1990, 5, 20));
        p.setRegisteredDate(LocalDate.now());
        p.setOrganizationId(orgId);
        return p;
    }

    @BeforeEach
    void cleanup() {
        patientRepository.deleteAll();
    }

    @Test
    void save_andFindByIdAndOrgId_returnsPatient() {
        Patient saved = patientRepository.save(buildPatient("Alice", "alice@test.com", "org-1"));

        Optional<Patient> found = patientRepository.findByIdAndOrganizationId(saved.getId(), "org-1");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Alice");
        assertThat(found.get().getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    void findByIdAndOrgId_returnsEmpty_forWrongOrg() {
        Patient saved = patientRepository.save(buildPatient("Bob", "bob@test.com", "org-1"));

        Optional<Patient> found = patientRepository.findByIdAndOrganizationId(saved.getId(), "org-2");

        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmailAndOrgId_returnsTrue_whenDuplicate() {
        patientRepository.save(buildPatient("Carol", "carol@test.com", "org-1"));

        boolean exists = patientRepository.existsByEmailAndOrganizationId("carol@test.com", "org-1");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmailAndOrgId_returnsFalse_acrossOrgs() {
        patientRepository.save(buildPatient("Dave", "dave@test.com", "org-1"));

        boolean exists = patientRepository.existsByEmailAndOrganizationId("dave@test.com", "org-2");

        assertThat(exists).isFalse();
    }

    @Test
    void findByIdAndOrgId_returnsEmpty_forNonExistentId() {
        Optional<Patient> found = patientRepository.findByIdAndOrganizationId(UUID.randomUUID(), "org-1");

        assertThat(found).isEmpty();
    }
}
