package com.smartemergency.service;

/** Geographic helpers. */
public final class Geo {
    private static final double EARTH_RADIUS_KM = 6371.0;

    private Geo() {}

    /** Straight-line distance in km between two GPS points (Haversine formula). */
    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }

    public static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
