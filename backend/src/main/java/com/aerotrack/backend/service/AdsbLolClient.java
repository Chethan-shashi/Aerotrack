package com.aerotrack.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Thin client for the free, key-less adsb.lol API (community ADS-B network, ODbL licensed data).
 * <ul>
 *   <li>Live aircraft: {@code GET https://api.adsb.lol/v2/point/{lat}/{lon}/{radiusNm}} (radius max 250 nm)</li>
 *   <li>Routes: {@code GET https://vrs-standing-data.adsb.lol/routes/{XX}/{CALLSIGN}.json}</li>
 * </ul>
 */
@Component
public class AdsbLolClient {

    private static final Logger log = LoggerFactory.getLogger(AdsbLolClient.class);

    private final RestClient liveClient;
    private final RestClient routeClient;

    public AdsbLolClient(@Value("${aerotrack.adsb.base-url:https://api.adsb.lol}") String baseUrl,
                         @Value("${aerotrack.adsb.route-url:https://vrs-standing-data.adsb.lol}") String routeUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));

        this.liveClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory)
                .defaultHeader("User-Agent", "AeroTrack-MSc-Project").build();
        this.routeClient = RestClient.builder().baseUrl(routeUrl).requestFactory(factory)
                .defaultHeader("User-Agent", "AeroTrack-MSc-Project").build();
    }

    /** Returns the raw "ac" array of aircraft within {@code radiusNm} nautical miles of the point. */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchAircraft(double lat, double lon, int radiusNm) {
        Map<String, Object> body = liveClient.get()
                .uri("/v2/point/{lat}/{lon}/{radius}", lat, lon, Math.min(radiusNm, 250))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        if (body == null || !(body.get("ac") instanceof List<?> ac)) {
            return List.of();
        }
        return (List<Map<String, Object>>) ac;
    }

    /** Looks up the scheduled route for a callsign. Returns null when unknown (e.g. private/military flights). */
    public Map<String, Object> fetchRoute(String callsign) {
        if (callsign == null || callsign.length() < 3) return null;
        try {
            return routeClient.get()
                    .uri("/routes/{prefix}/{callsign}.json", callsign.substring(0, 2), callsign)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            // 404 = no route known for this callsign; not an error for us
            log.debug("No route for {}: {}", callsign, e.getMessage());
            return null;
        }
    }
}

