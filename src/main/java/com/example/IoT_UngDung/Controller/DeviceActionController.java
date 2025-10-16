package com.example.IoT_UngDung.Controller;

import com.example.IoT_UngDung.DTO.DeviceActionDTO;
import com.example.IoT_UngDung.DTO.PagedResponseDTO;
import com.example.IoT_UngDung.Service.DeviceActionService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deviceactions")
@CrossOrigin(origins = "*")
public class DeviceActionController {

    @Autowired
    private DeviceActionService deviceActionService;

    @GetMapping("/data")
    public PagedResponseDTO<DeviceActionDTO> getPagedData(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,

            @Parameter(
                    description = "Thiết bị (all, fan, led, ac)",
                    schema = @Schema(allowableValues = {"all", "fan", "led", "ac"})
            )
            @RequestParam(defaultValue = "all") String device,

            @Parameter(
                    description = "Hành động (all, on, off)",
                    schema = @Schema(allowableValues = {"all", "on", "off"})
            )
            @RequestParam(defaultValue = "all") String action,

            @Parameter(
                    description = "Chế độ kích hoạt (all, manual, schedule, auto)",
                    schema = @Schema(allowableValues = {"all", "manual", "schedule", "auto"})
            )
            @RequestParam(defaultValue = "all") String trigger,

            @RequestParam(required = false, defaultValue = "") String value,
            @RequestParam(required = false, defaultValue = "time") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir
    ) {
        sortBy = sortBy.trim().toLowerCase();
        sortDir = sortDir.trim().toLowerCase();

        if (size < 1) size = 10;
        if (size > 100) size = 100;
        if (page < 1) page = 1;

        return deviceActionService.getPagedActions(page, size, device, action, trigger, value, sortBy, sortDir);
    }
}
