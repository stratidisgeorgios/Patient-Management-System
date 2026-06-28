package com.patientsystem.analyticsservice.repository;
import com.patientsystem.analyticsservice.model.ChargeEvent;
import java.util.List;
import java.math.BigDecimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.patientsystem.analyticsservice.model.TimeSeriesId;

public interface ChargeEventRepository extends JpaRepository<ChargeEvent, TimeSeriesId> {
    @Query(value = "SELECT SUM(CAST(price AS NUMERIC)) FROM charge_event WHERE EXTRACT(YEAR FROM timestamp) = :year", nativeQuery = true)
    BigDecimal getAnnualRevenue(@Param("year") int year);

    @Query(value = "SELECT category, SUM(CAST(price AS NUMERIC)) FROM charge_event GROUP BY category", nativeQuery = true)
    List<Object[]> getRevenuePerCategory();

    @Query("SELECT c.treatmentName, COUNT(c) FROM ChargeEvent c GROUP BY c.treatmentName ORDER BY COUNT(c) DESC")
    List<Object[]> getMostUsedTreatments();

}
