package com.vypeensoft.fakegps.model;

import java.io.Serializable;

public class LocationPoint implements Serializable {
    private final double latitude;
    private final double longitude;
    private final String timestamp; // ISO 8601 format

    public LocationPoint(double latitude, double longitude, String timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
