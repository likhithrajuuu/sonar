# System Design notes

## Description
Provided scenario page 1 of the backend engineer assignment, which explicitly states that system needs to recieve from the smartphones at a high frequency(10-50 events per second per device): GPS, accelerometer, and gyroscope readings. Your job is to build a minimal but correct ingestion + query service along how I decide to scale it

## Provided Information
1. Batching every 2s per device 
2. 10,000 devices support
3. 10-50 Hz sampling rate 
4. Batches of 1-500 events
5. No Auth, a single node is fine for the code, any language and database

## Assumptions
1. 10,000 is the stead-load not the peak
2. Every device streams continuously for 24 hours
3. 10-50 Hz sampling rate where I choose 30 Hz mid-point for a more realistic average
4. Event is about 100 bytes raw: 8 bytes for the timestamp, 72 for nine float64 values and 8 for the numeric device key, rounded up.

## Functional Requirements
1. The system should accept sensor data from a phone through a REST API (POST /api/v1/telemetry/ingest).
2. A request should contain one device_id and a list of 1 to 500 events.
3. Each event should have a timestamp, lat, lon, speed, accelerometer values (x, y, z) and gyroscope values (x, y, z).
4. The system should check every event on its own:
    - timestamp must be positive and not more than 24 hours in the future
    - lat must be between -90 and 90
    - lon must be between -180 and 180
    - speed must be between 0 and 300
    - accelerometer and gyroscope values must be numbers
5. Valid events should be saved, and invalid events should be rejected with their position (index) and the reason.
6. If the same device_id and timestamp come again, the system should not save it twice. It should count it as a duplicate.
7. The system should return 400 for the whole request if device_id is missing or empty, events is missing or empty, or there are more than 500 events.
8. The response should show accepted, duplicates, rejected and the list of errors.
9. The system should provide a summary API (GET /api/v1/telemetry/summary) that takes device_id, from and to.
10. The summary should return average speed, maximum acceleration and total event count for that time range (both from and to included).
If no events are found, it should return 200 with count 0 and null values.

## Non-functional requirements
1. Performance: the system should handle many requests per second (5,000 requests/s and up to 500,000 events/s in the design).
2. Fast queries: searching events of one device by time range should be fast, so the data is stored with an index on device_id and timestamp.
3. Reliability: if a phone sends the same batch again because of a network problem, the data should still be stored only once.
4. Concurrency: if two requests with the same event arrive at the same time, only one copy should be saved.
5. Scalability: the system should be able to grow by adding more servers when more devices join.
6. Availability: the system should stay up almost all the time, and it should not lose data when traffic suddenly increases.
7. Late data: events that arrive late should still be saved and show up in later queries.
8. Maintainability: the code should be clean and split into layers (API, service, storage), and it should have tests.
9. Easy setup: anyone should be able to run the project with simple steps from the README.
10. Security: login and authentication are not needed for this assignment, but a real system would need them.

## Back-of-the envelope estimation
Each device sends 1 request every 2 seconds, so each device makes around:  
`1 device / 2 s = 0.5 requests/s`  
So for the whole fleet of 10,000 devices the RPS would be(steady-state load):  
`10,000 x 0.5 requests/s = 5000 requests/s`.  
So for the peak load assuming that peak load would be 5x-10x more than the steady-state load:  
`25,000 requests/s - 50,000 requests/s`

## How a device sends a reading
Hertz in physics is basically what we call times per second. 
So let's say the device measure how many times, holds the readings in the buffer, and sends the whole buffer event 2 seconds.


