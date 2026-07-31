package com.patientsystem.analyticsservice;

import com.patientsystem.analyticsservice.repository.ChargeEventRepository;
import com.patientsystem.analyticsservice.repository.PatientEventRepository;
import com.patientsystem.analyticsservice.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    PatientEventRepository patientEventRepository;
    @Mock
    ChargeEventRepository chargeEventRepository;

    @InjectMocks
    AnalyticsService analyticsService;

    @Test
    void getTotalActivePatients_subtractsDeletedFromCreated() {
        when(patientEventRepository.totalCreated()).thenReturn(10L);
        when(patientEventRepository.totalDeleted()).thenReturn(3L);

        long result = analyticsService.getTotalActivePatients();

        assertThat(result).isEqualTo(7L);
    }

    @Test
    void getAverageAge_returnsRepositoryValue() {
        when(patientEventRepository.getAverageAge()).thenReturn(35.5);

        Double result = analyticsService.getAverageAge();

        assertThat(result).isEqualTo(35.5);
    }

    @Test
    void getAnnualRevenue_returnsZeroString_whenRepositoryReturnsNull() {
        when(chargeEventRepository.getAnnualRevenue(2024)).thenReturn(null);

        String result = analyticsService.getAnnualRevenue(2024);

        assertThat(result).isEqualTo("0");
    }

    @Test
    void getAnnualRevenue_returnsAmount_whenDataExists() {
        when(chargeEventRepository.getAnnualRevenue(2024)).thenReturn(new BigDecimal("1500.00"));

        String result = analyticsService.getAnnualRevenue(2024);

        assertThat(result).isEqualTo("1500.00");
    }

    @Test
    void getGenderDistribution_calculatesPercentagesCorrectly() {
        List<Object[]> rows = List.of(
                new Object[]{"MALE", 60L},
                new Object[]{"FEMALE", 40L}
        );
        when(patientEventRepository.getGenderDistribution()).thenReturn(rows);

        var result = analyticsService.getGenderDistribution();

        assertThat(result).containsKey("MALE").containsKey("FEMALE");
        assertThat(result.get("MALE")).isEqualTo(60.0);
        assertThat(result.get("FEMALE")).isEqualTo(40.0);
    }

    @Test
    void getPatientRegistrationsPerMonth_delegatesToRepository() {
        List<Object[]> expected = List.<Object[]>of(new Object[]{1.0, 5L});
        when(patientEventRepository.countCreatedPerMonth(2024)).thenReturn(expected);

        var result = analyticsService.getPatientRegistrationsPerMonth(2024);

        assertThat(result).isEqualTo(expected);
    }
}
