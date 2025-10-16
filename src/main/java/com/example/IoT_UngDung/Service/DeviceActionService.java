package com.example.IoT_UngDung.Service;

import com.example.IoT_UngDung.DTO.DeviceActionDTO;
import com.example.IoT_UngDung.DTO.PagedResponseDTO;
import com.example.IoT_UngDung.DTO.SessionDataDTO;
import com.example.IoT_UngDung.Entity.DeviceAction;
import com.example.IoT_UngDung.Repository.DeviceActionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeviceActionService {

    @Autowired
    private DeviceActionRepository deviceActionRepository;

    /**
     * Lấy danh sách tối đa 1000 bản ghi và xử lý filter, sort, phân trang.
     */
    public PagedResponseDTO<DeviceActionDTO> getPagedActions(
            int page, int size,
            String device, String action, String trigger, String value,
            String sortBy, String sortDir) {

        // 1️⃣ Lấy dữ liệu mới nhất từ DB
        List<DeviceAction> actions = deviceActionRepository.findLatest1000();
        Map<String, String> nameMapping = Map.of(
                "led1", "Fan",
                "led2", "Led",
                "led3", "AC"
        );
        // 2️⃣ Map sang DTO
        List<DeviceActionDTO> dtos = actions.stream()
                .map(a -> new DeviceActionDTO(
                        a.getId(),
                        nameMapping.getOrDefault(a.getDevice().getName(), a.getDevice().getName()),
                        a.isAction(),
                        a.getSource(),
                        a.getTime().toString()
                ))
                .collect(Collectors.toList());

        // 3️⃣ Lọc theo filter + value
        String devFilter = device != null ? device.trim().toLowerCase() : "all";
        String actFilter = action != null ? action.trim().toLowerCase() : "all";
        String trigFilter = trigger != null ? trigger.trim().toLowerCase() : "all";
        String searchValue = value != null ? value.trim().toLowerCase() : "";

        dtos = dtos.stream()
                // Lọc theo device
                .filter(d -> devFilter.equals("all")
                        || (d.getDeviceName() != null && d.getDeviceName().toLowerCase().contains(devFilter)))
                // Lọc theo action (ON/OFF)
                .filter(d -> actFilter.equals("all")
                        || (actFilter.equals("on") && d.isAction())
                        || (actFilter.equals("off") && !d.isAction()))
                // Lọc theo trigger/source
                .filter(d -> trigFilter.equals("all")
                        || (d.getTrigger() != null && d.getTrigger().toLowerCase().contains(trigFilter)))
                // Lọc theo giá trị search (thời gian)
                .filter(d -> searchValue.isEmpty()
                        || (d.getTime() != null && d.getTime().toLowerCase().contains(searchValue)))
                .toList();

//
        // 4️⃣ Sắp xếp
        
        if (sortBy == null || sortBy.isBlank()) sortBy = "time";
        if (sortDir == null || sortDir.isBlank()) sortDir = "desc";

        Comparator<DeviceActionDTO> comparator = switch (sortBy.toLowerCase()) {
            case "time", "createdat" -> Comparator.comparing(DeviceActionDTO::getTime);
            default -> Comparator.comparing(DeviceActionDTO::getTime);
        };

        if ("desc".equalsIgnoreCase(sortDir)) comparator = comparator.reversed();

        dtos = dtos.stream().sorted(comparator).toList();

        // 5️⃣ Phân trang thủ công
        int total = dtos.size();
        int totalPages = (int) Math.ceil((double) total / size);
        int start = Math.min((page - 1) * size, total);
        int end = Math.min(start + size, total);
        List<DeviceActionDTO> paged = dtos.subList(start, end);

        return new PagedResponseDTO<>(page, size, total, totalPages, paged);
    }
}
