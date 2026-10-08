# Sonar - InLane Technologies Assignment

To setup this application please have the following requirements satisfied in your computer:

```
Java Version 25 
Java Spring Boot >= 4.1.1
Spring Framework >= 7.0.9
PostgreSQL >= 18.0
PostgreSQL JDBC Driver 42.7.x
Maven 3.9.16
```

## 🚀 Steps for running this on your local machine
Inorder to run the application in your local machine please ensure that you have the above requirements satisfied.

### ⒈  Cloning the repository from github
Run the following command in your terminal to get the project
```
git clone https://github.com/likhithrajuuu/sonar.git
```

### 2. Create the database
Postgres has no `CREATE DATABASE IF NOT EXISTS`, so create it once by hand:
```bash
createdb sonar
```
(or `psql postgres -c "CREATE DATABASE sonar;"`). The `telemetry_event` table is created automatically on startup from `schema.sql`.

### 3. Configure the environment
1. Copy `.env.example` to `.env`.
2. Put your database name, username and password in it.

Spring Boot doesn't read `.env` files by itself, so load it into your terminal before starting the app:
```bash
set -a; source .env; set +a
```

### 4. Run the app
```bash
./mvnw spring-boot:run
```
Tomcat starts on port 8080.


## Commands for testing the application(sample test cases)
Alternative ways for testing this
1. look for `http` file and install `http client` extension on you IDE and then open a file a small `Send Request`link appears above each request just click that.

2. Run the following commands in your preferred terminal



**1. Ingest a batch of events**
```bash
curl -X POST http://localhost:8080/api/v1/telemetry/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "device_id": "DVC-1029",
    "events": [
      {
        "timestamp": 1730894521123,
        "lat": 12.9716, "lon": 77.5946, "speed_kmph": 42.5,
        "accel_x": 0.12, "accel_y": -0.03, "accel_z": 9.81,
        "gyro_x": 0.002, "gyro_y": -0.001, "gyro_z": 0.0
      },
      {
        "timestamp": 1730894521173,
        "lat": 12.9716, "lon": 77.5947, "speed_kmph": 42.8,
        "accel_x": 0.14, "accel_y": -0.02, "accel_z": 9.80,
        "gyro_x": 0.001, "gyro_y": -0.001, "gyro_z": 0.0
      }
    ]
  }'
```

Expected Response: 
```json
{
    "device_id":"DVC-1029",
    "accepted":2,
    "duplicates":0,
    "rejected":0,
    "errors":[]
}
```

**2. Send the same batch again(a retry)**
Run the exact above command a second time. Nothing new is stored, and both events come back as duplicates \

Expected Response:
```json
{
    "device_id":"DVC-1029",
    "accepted":0,
    "duplicates":2,
    "rejected":0,
    "errors":[]
}
```
**3. Get a summary for a time window**

Both `from` and `to` are epoch milliseconds and are inclusive.
```bash
curl "http://localhost:8080/api/v1/telemetry/summary?device_id=DVC-1029&from=1730894520000&to=1730894580000"
```

Expected response (after the two events above are ingested):
```json
{
    "device_id": "DVC-1029",
    "from": 1730894520000,
    "to": 1730894580000,
    "avg_speed_kmph": 42.65,
    "max_accel_magnitude": 9.810...,
    "event_count": 2
}
```

**4. Empty window** (no events in range): returns `event_count: 0` and `null` for the two aggregates.
```bash
curl "http://localhost:8080/api/v1/telemetry/summary?device_id=DVC-1029&from=1&to=2"
```

**5. Bad input**: `from` greater than `to`, or a missing parameter, returns `400`.
```bash
curl -i "http://localhost:8080/api/v1/telemetry/summary?device_id=DVC-1029&from=10&to=5"
```


## Design Notes

**Schema and indexes**

I have one table, `telemetry_event`, with one row per sensor reading: `device_id`, `ts` (epoch millis, BIGINT), then lat, lon, speed and the six accel/gyro values as `DOUBLE PRECISION`. Every column is `NOT NULL` because the spec says all fields are required.

```sql
CREATE TABLE IF NOT EXISTS telemetry_event(
    device_id VARCHAR(255) NOT NULL,
    ts BIGINT NOT NULL,
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
```

The primary key is `(device_id, ts)`, in that order. I didn't add any other index. Postgres builds a B-tree for the primary key, and putting `device_id` first means the summary query (`WHERE device_id = ? AND ts BETWEEN ? AND ?`) is a straight range scan on that same index. A second index would only add write cost on a table that gets a lot of inserts. I used `ts` for the column name because it's the event time, not the time the server received it.

**Duplicate detection and concurrency**

Duplicates are caught in two places:

- Inside one batch, I keep a set of timestamps while validating. The first event with a given timestamp is kept and later ones count as duplicates.
- Across requests, I let the database decide. The insert is `INSERT ... ON CONFLICT (device_id, ts) DO NOTHING`, and the driver tells me per row whether it was inserted (1) or skipped (0). Skipped rows are reported as duplicates.

I deliberately don't do "check if it exists, then insert", because two requests could both pass the check and then both insert. With `ON CONFLICT`, the primary key makes the call. If two requests with the same event arrive at once, Postgres lets the first one commit and the second one sees the conflict and does nothing. One row is stored, one request reports it as accepted and the other as a duplicate.

**What I'd change for production**

I'd stop writing straight into one plain Postgres table. At 10-50 events per second per device, this table grows very fast, so I would partition it by time (Postgres native partitioning, or TimescaleDB) so old data can be dropped or archived by partition and the indexes stay small. The primary key already includes `ts`, so it works as a partitioned key. I'd also put a queue like Kafka or Kinesis between the API and the database, so a traffic spike gets buffered instead of hitting Postgres directly. The details are in [DESIGN.md](DESIGN.md).

**Concurrency setup**

- Each batch is sorted by timestamp before inserting. Two overlapping batches that insert the same keys in different orders can deadlock in Postgres, and a fixed order avoids that.
- Requests run on virtual threads (`spring.threads.virtual.enabled`), and the connection pool size can be set with the optional `DATASOURCE_POOL_SIZE` variable (default 20).
- `reWriteBatchedInserts=true` on the JDBC URL turns a batch into multi-row inserts.

## Running the tests

```bash
./mvnw test
```

`IngestConcurrencyTest` starts a throwaway Postgres with Testcontainers (Docker must be running), sends the same batch from 32 threads at once and checks every event is stored exactly once. `SonarApplicationTests` needs the `DATASOURCE_*` variables exported.


## Part -3 Notes
See [DESIGN.md](DESIGN.md) for the system design write-up (Part 3).
