package com.example.analyticsservice.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;


@Component
public class TimescaleConfig implements ApplicationRunner{
    private final JdbcTemplate jdbcTemplate;

    public TimescaleConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("SELECT create_hypertable('patient_event', 'timestamp', if_not_exists => TRUE)");
        jdbcTemplate.execute("SELECT create_hypertable('charge_event', 'timestamp', if_not_exists => TRUE)");

        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_patient_event_type ON patient_event(event_type)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_patient_event_patient_id ON patient_event(patient_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_patient_event_gender ON patient_event(gender)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_patient_event_timestamp ON patient_event(timestamp DESC)");

        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_charge_event_timestamp ON charge_event(timestamp DESC)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_charge_event_category ON charge_event(category)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_charge_event_treatment ON charge_event(treatment_name)");
    }

}
