package com.likhithraju.sonar.entity;

public record Event(
    long timestamp,
    double lat,
    double lon,
    double speedKmph,
    double accelX,
    double accelY,
    double accelZ,
    double gyroX,
    double gyroY,
    double gyroZ
) {
}
