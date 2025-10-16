package com.example.IoT_UngDung.Controller;

import com.example.IoT_UngDung.DTO.PagedResponseDTO;
import com.example.IoT_UngDung.Entity.DataSensor;
import com.example.IoT_UngDung.Service.DataSensorService;
import com.example.IoT_UngDung.Repository.DataSensorRepository;
import com.example.IoT_UngDung.DTO.SessionDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/datasensors")
public class DataSensorController {

    private final DataSensorRepository repo;
    private final DataSensorService service;

    @Autowired
    public DataSensorController(DataSensorRepository repo, DataSensorService service) {
        this.repo = repo;
        this.service = service;
    }

    // Lấy dữ liệu của session gần nhất (DTO gọn gàng)
    @GetMapping(value = "/latest-session", produces = "application/json")
    public ResponseEntity<SessionDataDTO> getLatestSession() {
        return ResponseEntity.ok(service.getLatestSessionData());
    }

    // lấy 100 session gần nhất (DTO gọn gàng)
    @GetMapping(value = "/last-100-session", produces = "application/json")
    public ResponseEntity<List<SessionDataDTO>> getLast100Sessions() {
        return ResponseEntity.ok(service.getLast100Sessions());
    }

//    @GetMapping(value = "/last-1000-session", produces = "application/json")
//    public ResponseEntity<List<SessionDataDTO>> getLast1000Sessions() {
//        return ResponseEntity.ok(service.getLast1000Sessions());
//    }

    @GetMapping(value = "/data", produces = "application/json")
    public PagedResponseDTO<SessionDataDTO> getPagedData(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String value,
            @RequestParam(defaultValue = "time") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(DataSensorService.getPagedLatest1000(page, size, filter, value, sortBy, sortDir)).getBody();
    }


}
