package com.aerotrack.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

/**
 * A live aircraft currently seen by the ADS-B network (one row per ICAO 24-bit transponder address).
 * Rows are upserted by {@link com.aerotrack.backend.service.FlightPoller} and removed when the aircraft
 * has not been seen for a while.
 */
@Entity
@Table(name = "live_aircraft")
public class Aircraft {

    /** ICAO 24-bit transponder address, e.g. "801749". Unique per airframe. */
    @Id
    private String hex;

    private String callsign;      // e.g. "IGO615C"
    private String registration;  // e.g. "VT-ICY"
    private String typeCode;      // ICAO type designator, e.g. "A21N"

    // 4326 is the standard EPSG code for GPS coordinates (WGS 84)
    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location;

    private Integer altitude;     // barometric altitude in feet (null when on ground)
    private boolean onGround;
    private Double groundSpeed;   // knots
    private Double track;         // degrees true
    private Integer verticalRate; // feet per minute
    private String squawk;
    private Instant lastSeen;

    public Aircraft() {}

    public Aircraft(String hex) {
        this.hex = hex;
    }

    public String getHex() { return hex; }
    public void setHex(String hex) { this.hex = hex; }

    public String getCallsign() { return callsign; }
    public void setCallsign(String callsign) { this.callsign = callsign; }

    public String getRegistration() { return registration; }
    public void setRegistration(String registration) { this.registration = registration; }

    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }

    public Point getLocation() { return location; }
    public void setLocation(Point location) { this.location = location; }

    public Integer getAltitude() { return altitude; }
    public void setAltitude(Integer altitude) { this.altitude = altitude; }

    public boolean isOnGround() { return onGround; }
    public void setOnGround(boolean onGround) { this.onGround = onGround; }

    public Double getGroundSpeed() { return groundSpeed; }
    public void setGroundSpeed(Double groundSpeed) { this.groundSpeed = groundSpeed; }

    public Double getTrack() { return track; }
    public void setTrack(Double track) { this.track = track; }

    public Integer getVerticalRate() { return verticalRate; }
    public void setVerticalRate(Integer verticalRate) { this.verticalRate = verticalRate; }

    public String getSquawk() { return squawk; }
    public void setSquawk(String squawk) { this.squawk = squawk; }

    public Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }
}

