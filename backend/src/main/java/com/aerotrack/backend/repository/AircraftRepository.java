package com.aerotrack.backend.repository;

import com.aerotrack.backend.model.Aircraft;
import org.locationtech.jts.geom.Polygon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AircraftRepository extends JpaRepository<Aircraft, String> {

    // Module 2: PostGIS spatial bounding-box query - only return aircraft inside the user's map viewport
    @Query("SELECT a FROM Aircraft a WHERE within(a.location, :viewport) = true")
    List<Aircraft> findWithinViewport(@Param("viewport") Polygon viewport);

    @Modifying
    @Query("DELETE FROM Aircraft a WHERE a.lastSeen < :cutoff")
    int deleteStale(@Param("cutoff") Instant cutoff);
}

