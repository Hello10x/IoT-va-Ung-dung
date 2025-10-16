package com.example.IoT_UngDung.Controller;

import com.example.IoT_UngDung.Entity.Device;
import com.example.IoT_UngDung.Repository.DeviceRepository;
import com.example.IoT_UngDung.Service.MqttService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api/device")
public class DeviceController {

    private final MqttService mqttService;
    private final DeviceRepository deviceRepo;

    public DeviceController(DeviceRepository deviceRepo, MqttService mqttService) {
        this.deviceRepo = deviceRepo;
        this.mqttService = mqttService;
    }

    @GetMapping("/status")
    public List<Device> getAllDeviceStatus() {
        return deviceRepo.findAll();
    }

    @PostMapping("/action/{key}")
    public ResponseEntity<String> toggleDevice(@PathVariable String key, @RequestParam boolean state) {
        try {
            boolean success = mqttService.publishDeviceActionAndWait(key, state);

            if (success) {
                return ResponseEntity.ok("✅ Thiết bị " + key + " bật/tắt thành công");
            } else {
                return ResponseEntity.status(500).body("⚠️ Thiết bị " + key + " lỗi hoặc không phản hồi");
            }
        } catch (TimeoutException e) {
            return ResponseEntity.status(504).body("❌ Quá 10 giây không có phản hồi từ thiết bị");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ Lỗi xử lý: " + e.getMessage());
        }
    }

}
