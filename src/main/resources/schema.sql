-- Telemetry event table schema
CREATE TABLE IF NOT EXISTS telemetry_event(
    device_id VARCHAR(255) NOT NULL,
    ts BIGINT NOT NULL,  -- this is the timestamp of the event in milliseconds since epoch
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL,
    speed_kmph DOUBLE PRECISION NOT NULL,
    accel_x DOUBLE PRECISION NOT NULL,
    accel_y DOUBLE PRECISION NOT NULL,
    accel_z DOUBLE PRECISION NOT NULL,
    gyro_x DOUBLE PRECISION NOT NULL,
    gyro_y DOUBLE PRECISION NOT NULL,
    gyro_z DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (device_id, ts)
);

