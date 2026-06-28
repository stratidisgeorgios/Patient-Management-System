package com.patientsystem.analyticsservice.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class TimeSeriesId implements Serializable {
    private String id;
    private LocalDateTime timestamp;

    public TimeSeriesId() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeSeriesId)) return false;
        TimeSeriesId that = (TimeSeriesId) o;
        return Objects.equals(id, that.id) && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp);
    }
}
