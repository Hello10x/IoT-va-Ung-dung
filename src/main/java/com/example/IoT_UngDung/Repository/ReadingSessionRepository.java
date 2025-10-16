package com.example.IoT_UngDung.Repository;

import com.example.IoT_UngDung.Entity.ReadingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReadingSessionRepository extends JpaRepository<ReadingSession, Integer> {

    // Lấy 1 session mới nhất
    ReadingSession findTopByOrderByCreatedAtDesc();
    // Lấy 100 session mới nhất
    List<ReadingSession> findTop100ByOrderByCreatedAtDesc();

    @Query(value = "SELECT * FROM readingsession ORDER BY created_at DESC LIMIT 1000", nativeQuery = true)
    List<ReadingSession> findLatest1000();

}
