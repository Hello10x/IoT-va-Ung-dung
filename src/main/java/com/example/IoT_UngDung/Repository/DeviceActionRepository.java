package com.example.IoT_UngDung.Repository;

import com.example.IoT_UngDung.Entity.DeviceAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DeviceActionRepository extends JpaRepository<DeviceAction, Integer> {
    @Query(value = "SELECT * FROM deviceactions ORDER BY time DESC LIMIT 1000", nativeQuery = true)
    List<DeviceAction> findLatest1000();

}
