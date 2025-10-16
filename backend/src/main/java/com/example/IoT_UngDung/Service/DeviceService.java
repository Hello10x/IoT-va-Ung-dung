package com.example.IoT_UngDung.Service;

import com.example.IoT_UngDung.Entity.Device;
import com.example.IoT_UngDung.Repository.DeviceRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepo;

    public DeviceService(DeviceRepository deviceRepo) {
        this.deviceRepo = deviceRepo;
    }

    // Lấy danh sách trạng thái hiện tại của các thiết bị (led1, led2, led3)
    public List<Device> getAllDevices() {
        return deviceRepo.findAll();
    }

    // Cập nhật trạng thái bật/tắt của 1 thiết bị
    public void updateDeviceStatus(String deviceName, boolean status) {
        deviceRepo.findByName(deviceName).ifPresent(device -> {
            device.setStatus(status);
            deviceRepo.save(device);
        });
    }

}
