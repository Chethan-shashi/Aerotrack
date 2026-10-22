package com.aerotrack.backend.controller;

import com.aerotrack.backend.model.Aircraft;
import com.aerotrack.backend.repository.AircraftRepository;
import com.aerotrack.backend.repository.PositionReportRepository;
import com.aerotrack.backend.service.Dtos.Airport;
import com.aerotrack.backend.service.Dtos.FlightDetails;
import com.aerotrack.backend.service.Dtos.FlightSummary;
import com.aerotrack.backend.service.Dtos.Status;
import com.aerotrack.backend.service.Dtos.TrailPoint;
import com.aerotrack.backend.service.FlightPoller;
import com.aerotrack.backend.service.ReferenceData;
import com.aerotrack.backend.service.RouteService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Read-only API for the map. All data comes from our PostGIS database (filled by {@link FlightPoller}),
 * never directly from the external ADS-B API.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class FlightController {

    private static final GeometryFactory GEOMETRY = new GeometryFactory(new PrecisionModel(), 4326);

    private final AircraftRepository aircraftRepository;
    private final PositionReportRepository positionRepository;
    private final RouteService routeService;
    private final FlightPoller poller;

    public FlightController(AircraftRepository aircraftRepository, PositionReportRepository positionRepository,
                            RouteService routeService, FlightPoller poller) {
        this.aircraftRepository = aircraftRepository;
        this.positionRepository = positionRepository;
        this.routeService = routeService;
        this.poller = poller;
    }

    /** Module 2: aircraft inside the user's current map viewport (PostGIS spatial query). */
    @GetMapping("/flights")
    public List<FlightSummary> flightsInViewport(
            @RequestParam(defaultValue = "-90") double minLat,
            @RequestParam(defaultValue = "-180") double minLng,
            @RequestParam(defaultValue = "90") double maxLat,
            @RequestParam(defaultValue = "180") double maxLng) {

        Polygon viewport = GEOMETRY.createPolygon(new Coordinate[] {
                new Coordinate(minLng, minLat),
                new Coordinate(maxLng, minLat),
                new Coordinate(maxLng, maxLat),
                new Coordinate(minLng, maxLat),
                new Coordinate(minLng, minLat) // A valid Polygon must close back on its first point
        });

        return aircraftRepository.findWithinViewport(viewport).stream().map(FlightController::toSummary).toList();
    }

    /** Full details for the side panel: airline, aircraft type, route (origin -> destination), live data. */
    @GetMapping("/flights/{hex}")
    public ResponseEntity<FlightDetails> flightDetails(@PathVariable String hex) {
        return aircraftRepository.findById(hex.toLowerCase())
                .map(a -> ResponseEntity.ok(new FlightDetails(
                        a.getHex(), a.getCallsign(), a.getRegistration(), a.getTypeCode(),
                        ReferenceData.aircraftName(a.getTypeCode()),
                        ReferenceData.airlineFor(a.getCallsign()),
                        routeService.lookup(a.getCallsign()),
                        a.getLocation().getY(), a.getLocation().getX(),
                        a.getAltitude(), a.isOnGround(), a.getGroundSpeed(), a.getTrack(),
                        a.getVerticalRate(), a.getSquawk(),
                        a.getLastSeen() == null ? 0 : a.getLastSeen().toEpochMilli())))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Position history used to draw the flight trail. */
    @GetMapping("/flights/{hex}/trail")
    public List<TrailPoint> trail(@PathVariable String hex) {
        return positionRepository.findByHexOrderByRecordedAtAsc(hex.toLowerCase()).stream()
                .map(p -> new TrailPoint(p.getLocation().getY(), p.getLocation().getX(), p.getAltitude(),
                        p.getRecordedAt().toEpochMilli()))
                .toList();
    }

    @GetMapping("/airports")
    public List<Airport> airports() {
        return ReferenceData.KARNATAKA_AIRPORTS;
    }

    @GetMapping("/status")
    public Status status() {
        Instant last = poller.getLastSuccess();
        return new Status((int) aircraftRepository.count(), last == null ? null : last.toEpochMilli(),
                "adsb.lol (ODbL)");
    }

    private static FlightSummary toSummary(Aircraft a) {
        return new FlightSummary(a.getHex(), a.getCallsign(), a.getRegistration(), a.getTypeCode(),
                a.getLocation().getY(), a.getLocation().getX(), a.getAltitude(), a.isOnGround(),
                a.getGroundSpeed(), a.getTrack());
    }
}

