package com.aerotrack.backend.repository;

import com.aerotrack.backend.model.PositionReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PositionReportRepository extends JpaRepository<PositionReport, Long> {

    List<PositionReport> findByHexOrderByRecordedAtAsc(String hex);

    @Modifying
    @Query("DELETE FROM PositionReport p WHERE p.recordedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}

