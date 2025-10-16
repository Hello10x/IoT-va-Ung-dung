package com.example.IoT_UngDung.Repository;

import com.example.IoT_UngDung.Entity.DataSensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface DataSensorRepository extends JpaRepository<DataSensor, Long> {

    // lay data theo id session
    @Query("SELECT d FROM DataSensor d WHERE d.session.id = :sessionId")
    List<DataSensor> findBySessionId(@Param("sessionId") Integer sessionId);

    @Query("SELECT ds FROM DataSensor ds WHERE ds.session.id IN :ids")
    List<DataSensor> findAllBySessionIds(@Param("ids") List<Integer> ids);

    @Query(value = """
    SELECT ds.*
    FROM datasensor ds
    JOIN (
        SELECT id
        FROM datasensor
        ORDER BY id DESC
        LIMIT 1000
    ) recent ON ds.id = recent.id
    """, nativeQuery = true)
    List<DataSensor> findLatest1000();
}
