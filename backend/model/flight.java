package com.aerotrack.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "active_flights")
public class Flight {

    @Id
    private String id;
    
    private String callsign;
    private double heading;

    // This tells Hibernate to use the PostGIS spatial geometry engine
    // 4326 is the standard EPSG code for GPS coordinates (WGS 84)
    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location;

    // We still store the simulated origin to keep your long orange trails working
    @Column(name = "origin_lat")
    private double originLat;

    @Column(name = "origin_lng")
    private double originLng;

    // Empty constructor required by JPA
    public Flight() {}

    public Flight(String id, String callsign, double heading, Point location, double originLat, double originLng) {
        this.id = id;
        this.callsign = callsign;
        this.heading = heading;
        this.location = location;
        this.originLat = originLat;
        this.originLng = originLng;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCallsign() { return callsign; }
    public void setCallsign(String callsign) { this.callsign = callsign; }

    public double getHeading() { return heading; }
    public void setHeading(double heading) { this.heading = heading; }

    public Point getLocation() { return location; }
    public void setLocation(Point location) { this.location = location; }

    public double getOriginLat() { return originLat; }
    public void setOriginLat(double originLat) { this.originLat = originLat; }

    public double getOriginLng() { return originLng; }
    public void setOriginLng(double originLng) { this.originLng = originLng; }
}