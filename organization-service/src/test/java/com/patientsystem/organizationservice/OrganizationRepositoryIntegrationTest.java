package com.patientsystem.organizationservice;

import com.patientsystem.organizationservice.model.Organization;
import com.patientsystem.organizationservice.repository.OrganizationRepository;
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

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// organization-service has no Flyway — let Hibernate create the schema
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class OrganizationRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    OrganizationRepository organizationRepository;

    @BeforeEach
    void cleanup() {
        organizationRepository.deleteAll();
    }

    private Organization buildOrg(String name) {
        Organization org = new Organization();
        org.setName(name);
        org.setAdminEmail("admin@" + name.toLowerCase().replace(" ", "") + ".com");
        org.setRegisteredDate(LocalDate.now());
        return org;
    }

    @Test
    void save_andFindById_returnsOrganization() {
        Organization saved = organizationRepository.save(buildOrg("Acme Clinic"));

        Optional<Organization> found = organizationRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Acme Clinic");
    }

    @Test
    void findById_returnsEmpty_forNonExistentId() {
        Optional<Organization> found = organizationRepository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void delete_removesOrganization() {
        Organization saved = organizationRepository.save(buildOrg("Delete Me Clinic"));
        UUID id = saved.getId();

        organizationRepository.delete(saved);

        assertThat(organizationRepository.findById(id)).isEmpty();
    }
}
