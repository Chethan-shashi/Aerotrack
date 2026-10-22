package com.aerotrack.backend.service;

import com.aerotrack.backend.model.Aircraft;
import com.aerotrack.backend.model.PositionReport;
import com.aerotrack.backend.repository.AircraftRepository;
import com.aerotrack.backend.repository.PositionReportRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Polls the live ADS-B feed for the configured region (Karnataka by default) and stores it in PostGIS.
 * <p>
 * Only this one job talks to the external API, so the number of API calls is constant
 * (one every few seconds) no matter how many users have the map open. All browsers are served
 * from our own database via the spatial viewport query.
 */
@Service
public class FlightPoller {

    private static final Logger log = LoggerFactory.getLogger(FlightPoller.class);
    private static final GeometryFactory GEOMETRY = new GeometryFactory(new PrecisionModel(), 4326);

    private final AdsbLolClient client;
    private final AircraftRepository aircraftRepository;
    private final PositionReportRepository positionRepository;

    @Value("${aerotrack.region.lat:15.0}")
    private double regionLat;
    @Value("${aerotrack.region.lon:76.3}")
    private double regionLon;
    @Value("${aerotrack.region.radius-nm:250}")
    private int regionRadiusNm;
    @Value("${aerotrack.stale-after-seconds:60}")
    private long staleAfterSeconds;
    @Value("${aerotrack.trail-minutes:60}")
    private long trailMinutes;

    private volatile Instant lastSuccess;

    public FlightPoller(AdsbLolClient client, AircraftRepository aircraftRepository,
                        PositionReportRepository positionRepository) {
        this.client = client;
        this.aircraftRepository = aircraftRepository;
        this.positionRepository = positionRepository;
    }

    @Scheduled(fixedDelayString = "${aerotrack.poll-ms:5000}", initialDelay = 1000)
    @Transactional
    public void poll() {
        List<Map<String, Object>> feed;
        try {
            feed = client.fetchAircraft(regionLat, regionLon, regionRadiusNm);
        } catch (Exception e) {
            log.warn("ADS-B fetch failed: {}", e.getMessage());
            return;
        }

        Instant now = Instant.now();
        Map<String, Aircraft> existing = aircraftRepository.findAll().stream()
                .collect(Collectors.toMap(Aircraft::getHex, Function.identity()));

        List<Aircraft> toSave = new ArrayList<>();
        List<PositionReport> newPositions = new ArrayList<>();

        for (Map<String, Object> ac : feed) {
            String hex = str(ac.get("hex"));
            Double lat = num(ac.get("lat"));
            Double lon = num(ac.get("lon"));
            Double seenPos = num(ac.get("seen_pos"));
            if (hex == null || lat == null || lon == null) continue;           // no position yet
            if (seenPos != null && seenPos > staleAfterSeconds) continue;      // position too old

            Aircraft a = existing.getOrDefault(hex, new Aircraft(hex));
            Point previous = a.getLocation();
            Point point = GEOMETRY.createPoint(new Coordinate(lon, lat));

            Object altBaro = ac.get("alt_baro");
            boolean onGround = "ground".equals(altBaro);
            Integer altitude = onGround ? 0 : intOrNull(altBaro);

            a.setCallsign(trimToNull(str(ac.get("flight"))));
            a.setRegistration(trimToNull(str(ac.get("r"))));
            a.setTypeCode(trimToNull(str(ac.get("t"))));
            a.setLocation(point);
            a.setOnGround(onGround);
            a.setAltitude(altitude);
            a.setGroundSpeed(num(ac.get("gs")));
            a.setTrack(firstNonNull(num(ac.get("track")), num(ac.get("true_heading")), a.getTrack()));
            a.setVerticalRate(intOrNull(firstNonNull(ac.get("baro_rate"), ac.get("geom_rate"))));
            a.setSquawk(str(ac.get("squawk")));
            Instant seenAt = seenPos != null ? now.minusMillis((long) (seenPos * 1000)) : now;
            a.setLastSeen(seenAt);
            toSave.add(a);

            // Record trail point only when the aircraft actually moved
            if (previous == null || !previous.equalsExact(point, 1e-6)) {
                newPositions.add(new PositionReport(hex, point, altitude, seenAt));
            }
        }

        aircraftRepository.saveAll(toSave);
        positionRepository.saveAll(newPositions);

        int removed = aircraftRepository.deleteStale(now.minusSeconds(staleAfterSeconds));
        positionRepository.deleteOlderThan(now.minus(Duration.ofMinutes(trailMinutes)));

        lastSuccess = now;
        log.debug("ADS-B poll: {} aircraft, {} new positions, {} removed", toSave.size(), newPositions.size(), removed);
    }

    public Instant getLastSuccess() {
        return lastSuccess;
    }

    // ---------------- JSON helpers ----------------

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static Double num(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    private static Integer intOrNull(Object o) {
        Double d = num(o);
        return d == null ? null : (int) Math.round(d);
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T v : values) if (v != null) return v;
        return null;
    }
}
