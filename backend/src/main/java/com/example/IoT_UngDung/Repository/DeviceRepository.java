package com.example.IoT_UngDung.Repository;

import com.example.IoT_UngDung.Entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Integer> {
    Optional<Device> findByName(String name);

    // Lấy trạng thái thiết bị theo tên (nếu bạn có cột 'state' kiểu boolean)
    @Query("SELECT d.status FROM Device d WHERE d.name = :name")
    Boolean findStateByName(String name);


}