package com.example.analyticsservice.repository;
import com.example.analyticsservice.model.PatientEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PatientEventRepository extends JpaRepository<PatientEvent, String> {
    @Query("SELECT COUNT(p) FROM PatientEvent p WHERE p.eventType = 'PatientCreated'")
    long totalCreated();

    @Query("SELECT COUNT(p) FROM PatientEvent p WHERE p.eventType = 'PatientDeleted'")
    long totalDeleted();

    @Query("SELECT EXTRACT(MONTH FROM p.timestamp), COUNT(p) FROM PatientEvent p WHERE p.eventType = 'PatientCreated' AND EXTRACT(YEAR FROM p.timestamp) = :year GROUP BY EXTRACT(MONTH FROM p.timestamp) ORDER BY EXTRACT(MONTH FROM p.timestamp)")
    List<Object[]> countCreatedPerMonth(@Param("year") int year);

    @Query(value = "SELECT AVG(DATE_PART('year', AGE(CURRENT_DATE, pe.date_of_birth::date))) FROM patient_event pe LEFT JOIN patient_event del ON pe.patient_id = del.patient_id AND del.event_type = 'PatientDeleted' WHERE pe.event_type = 'PatientCreated' AND del.patient_id IS NULL", nativeQuery = true)
    Double getAverageAge();

    @Query(value = "SELECT pe.gender, COUNT(*) FROM patient_event pe LEFT JOIN patient_event del ON pe.patient_id = del.patient_id AND del.event_type = 'PatientDeleted' WHERE pe.event_type = 'PatientCreated' AND del.patient_id IS NULL GROUP BY pe.gender", nativeQuery = true)
    List<Object[]> getGenderDistribution();
}
