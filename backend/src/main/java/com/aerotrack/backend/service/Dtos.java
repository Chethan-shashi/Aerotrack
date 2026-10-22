package com.aerotrack.backend.service;

/** Small DTOs shared between services and the REST API. */
public final class Dtos {

    private Dtos() {}

    public record Airport(String icao, String iata, String name, String city, String country, double lat, double lng) {}

    public record Route(Airport origin, Airport destination) {}

    public record Airline(String icao, String name) {}

    public record TrailPoint(double lat, double lng, Integer altitude, long t) {}

    public record FlightSummary(String hex, String callsign, String registration, String type,
                                double lat, double lng, Integer altitude, boolean onGround,
                                Double speed, Double track) {}

    public record FlightDetails(String hex, String callsign, String registration, String type, String aircraftName,
                                Airline airline, Route route,
                                double lat, double lng, Integer altitude, boolean onGround,
                                Double speed, Double track, Integer verticalRate, String squawk, long lastSeen) {}

    public record Status(int aircraft, Long lastUpdate, String source) {}
}
