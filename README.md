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