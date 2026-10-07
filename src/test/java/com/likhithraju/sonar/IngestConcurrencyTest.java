package com.likhithraju.sonar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.likhithraju.sonar.dto.IngestResponse;
import com.likhithraju.sonar.service.IngestService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@SpringBootTest
class IngestConcurrencyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    IngestService ingestService;

    @Autowired
    JdbcTemplate jdbc;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void sameBatchSentConcurrentlyIsStoredOnce() throws Exception {
        int batchSize = 200;
        int threads = 32;
        long base = System.currentTimeMillis() - 60_000;

        List<JsonNode> events = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            events.add(mapper.readTree("""
                {"timestamp": %d, "lat": 12.9, "lon": 77.6, "speed_kmph": 30,
                 "accel_x": 0.1, "accel_y": 0.2, "accel_z": 9.8,
                 "gyro_x": 0.0, "gyro_y": 0.0, "gyro_z": 0.0}
                """.formatted(base + i)));
        }

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<IngestResponse>> results = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            Callable<IngestResponse> task = () -> {
                start.await();
                return ingestService.ingest("device-1", events);
            };
            results.add(pool.submit(task));
        }
        start.countDown();

        int accepted = 0;
        int duplicates = 0;
        for (Future<IngestResponse> f : results) {
            IngestResponse r = f.get();
            accepted += r.accepted();
            duplicates += r.duplicates();
        }
        pool.shutdown();

        assertEquals(batchSize, accepted);
        assertEquals((threads - 1) * batchSize, duplicates);
        assertEquals(batchSize,
            jdbc.queryForObject("SELECT count(*) FROM telemetry_event WHERE device_id = 'device-1'", Integer.class));
    }
}
