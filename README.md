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

### 2. Configuring the `.env` in you machine
1. Please look for the file that is named as `.env.example`
2. Duplicate that file in the same root folder and rename to `.env`
3. Replace `your-database-name` with the actual database name in the `DATASOURCE_URL` field
4. Replace the credentials of `DATASOURCE_USERNAME` and `DATASOURCE_PASSWORD` with your database credentials

### 3. Running the spring boot application
Please go to the project directory in your terminal and run the following command to start the application

``` ./mvnw spring-boot:run ```

This will ensure the TomCat server is up and running on the port 8080 ! (default)


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

## Design Notes

**Schema and indexes**

I have one table, `telemetry_event`, with one row per sensor reading: `device_id`, `ts` (epoch millis, BIGINT), then lat, lon, speed and the six accel/gyro values as `DOUBLE PRECISION`. Every column is `NOT NULL` because the spec says all fields are required.

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
