package com.likhithraju.sonar.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.likhithraju.sonar.entity.Event;

@Repository
public class TelemetryRepository {

    // ON CONFLICT DO NOTHING lets the primary key (device_id, ts) arbitrate duplicates
    // atomically, so concurrent retries can never produce a second row.
    private static final String INSERT = """
        INSERT INTO telemetry_event
            (device_id, ts, lat, lon, speed_kmph, accel_x, accel_y, accel_z, gyro_x, gyro_y, gyro_z)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (device_id, ts) DO NOTHING
        """;

    private final JdbcTemplate jdbc;

    public TelemetryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Returns one entry per event: 1 if inserted, 0 if it already existed. */
    public int[] insertBatch(String deviceId, List<Event> events) {
        return jdbc.batchUpdate(INSERT, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Event e = events.get(i);
                ps.setString(1, deviceId);
                ps.setLong(2, e.timestamp());
                ps.setDouble(3, e.lat());
                ps.setDouble(4, e.lon());
                ps.setDouble(5, e.speedKmph());
                ps.setDouble(6, e.accelX());
                ps.setDouble(7, e.accelY());
                ps.setDouble(8, e.accelZ());
                ps.setDouble(9, e.gyroX());
                ps.setDouble(10, e.gyroY());
                ps.setDouble(11, e.gyroZ());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        });
    }
}
