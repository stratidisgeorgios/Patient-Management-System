package com.patientsystem.analyticsservice.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

import com.patientsystem.analyticsservice.repository.ChargeEventRepository;
import com.patientsystem.analyticsservice.repository.PatientEventRepository;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import org.springframework.cache.annotation.Cacheable;

@Service
public class AnalyticsService {
    private final PatientEventRepository patientEventRepository;
    private final ChargeEventRepository chargeEventRepository;
    public AnalyticsService(PatientEventRepository patientEventRepository, ChargeEventRepository chargeEventRepository) {
        this.patientEventRepository = patientEventRepository;
        this.chargeEventRepository = chargeEventRepository;
    }
    @Cacheable("total-active-patients")
    public long getTotalActivePatients(){
        return patientEventRepository.totalCreated() - patientEventRepository.totalDeleted();
    }
    @Cacheable("patient-registrations-per-month")
    public List<Object[]> getPatientRegistrationsPerMonth(int year) {
        return patientEventRepository.countCreatedPerMonth(year);
    }
    @Cacheable("average-age")
    public Double getAverageAge(){
        return patientEventRepository.getAverageAge();
    }
    @Cacheable("annual-revenue")
    public BigDecimal getAnnualRevenue(int year) {
        return chargeEventRepository.getAnnualRevenue(year);
    }
    @Cacheable("revenue-per-category")
    public List<Object[]> getRevenuePerCategory() {
        return chargeEventRepository.getRevenuePerCategory();
    }
    @Cacheable("most-used-treatments")
    public List<Object[]> getMostUsedTreatments() {
        return chargeEventRepository.getMostUsedTreatments();
    }
    @Cacheable("gender-distribution")
    public Map<String, Double> getGenderDistribution() {
        List<Object[]> results = patientEventRepository.getGenderDistribution();
        long total = results.stream().mapToLong(r -> (Long) r[1]).sum();
        Map<String, Double> map = new LinkedHashMap<>();
        for (Object[] row : results) {
            double percentage = (Long) row[1] * 100.0 / total;
            map.put((String) row[0], percentage);
        }
        return map;
    }
}
