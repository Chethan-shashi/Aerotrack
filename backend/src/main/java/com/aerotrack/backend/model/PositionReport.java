package com.aerotrack.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

/** One historical position of an aircraft, used to draw the flight trail. */
@Entity
@Table(name = "aircraft_positions", indexes = @Index(name = "idx_positions_hex_time", columnList = "hex, recordedAt"))
public class PositionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String hex;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location;

    private Integer altitude;
    private Instant recordedAt;

    public PositionReport() {}

    public PositionReport(String hex, Point location, Integer altitude, Instant recordedAt) {
        this.hex = hex;
        this.location = location;
        this.altitude = altitude;
        this.recordedAt = recordedAt;
    }

    public Long getId() { return id; }
    public String getHex() { return hex; }
    public Point getLocation() { return location; }
    public Integer getAltitude() { return altitude; }
    public Instant getRecordedAt() { return recordedAt; }
}

