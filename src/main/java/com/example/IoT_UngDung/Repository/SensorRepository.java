package com.example.IoT_UngDung.Repository;

import com.example.IoT_UngDung.Entity.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SensorRepository extends JpaRepository<Sensor, Integer> {
    Optional<Sensor> findByType(String type);
}
