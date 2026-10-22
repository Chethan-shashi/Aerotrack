package com.aerotrack.backend.service;

import com.aerotrack.backend.service.Dtos.Airport;
import com.aerotrack.backend.service.Dtos.Route;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves callsign -> origin/destination airports using adsb.lol's free route database.
 * Results (including "not found") are cached so each callsign is looked up at most once every few hours.
 */
@Service
public class RouteService {

    private static final Duration TTL = Duration.ofHours(6);

    private record Cached(Route route, Instant fetchedAt) {}

    private final AdsbLolClient client;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    public RouteService(AdsbLolClient client) {
        this.client = client;
    }

    public Route lookup(String callsign) {
        if (callsign == null || callsign.isBlank()) return null;
        String key = callsign.trim().toUpperCase();

        Cached cached = cache.get(key);
        if (cached != null && cached.fetchedAt().plus(TTL).isAfter(Instant.now())) {
            return cached.route();
        }

        Route route = parse(client.fetchRoute(key));
        cache.put(key, new Cached(route, Instant.now()));
        return route;
    }

    private Route parse(Map<String, Object> json) {
        if (json == null || !(json.get("_airports") instanceof List<?> airports) || airports.size() < 2) {
            return null;
        }
        Airport origin = toAirport(airports.getFirst());
        Airport destination = toAirport(airports.getLast());
        return (origin == null || destination == null) ? null : new Route(origin, destination);
    }

    private Airport toAirport(Object o) {
        if (!(o instanceof Map<?, ?> m)) return null;
        if (!(m.get("lat") instanceof Number lat) || !(m.get("lon") instanceof Number lon)) return null;
        return new Airport(
                str(m.get("icao")), str(m.get("iata")), str(m.get("name")),
                str(m.get("location")), str(m.get("countryiso2")),
                lat.doubleValue(), lon.doubleValue());
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}

