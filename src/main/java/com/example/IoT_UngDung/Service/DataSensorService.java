package com.example.IoT_UngDung.Service;

import com.example.IoT_UngDung.DTO.PagedResponseDTO;
import com.example.IoT_UngDung.DTO.SessionDataDTO;
import com.example.IoT_UngDung.Entity.DataSensor;
import com.example.IoT_UngDung.Entity.ReadingSession;
import com.example.IoT_UngDung.Repository.DataSensorRepository;
import com.example.IoT_UngDung.Repository.ReadingSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.*;

import java.util.stream.Collectors;

@Service

public class DataSensorService {
    @Autowired
    private static DataSensorRepository dataSensorRepository;
    @Autowired
    private static ReadingSessionRepository readingSessionRepository;

    @Autowired
    public DataSensorService(DataSensorRepository dataSensorRepository,
                             ReadingSessionRepository readingSessionRepository) {
        this.dataSensorRepository = dataSensorRepository;
        this.readingSessionRepository = readingSessionRepository;
    }

    // Lấy phiên gần nhất
    public SessionDataDTO getLatestSessionData() {
        ReadingSession readingSession = readingSessionRepository
                .findTopByOrderByCreatedAtDesc();

        List<DataSensor> sensors = dataSensorRepository.findBySessionId(readingSession.getId());

        float temp = 0, hum = 0, lux = 0;

        for (DataSensor ds : sensors) {
            String type = ds.getSensor().getType().toLowerCase();
            switch (type) {
                case "temperature": temp = ds.getValue(); break;
                case "humidity": hum = ds.getValue(); break;
                case "lux": lux = ds.getValue(); break;
            }
        }

        return new SessionDataDTO(
                readingSession.getId(),
                readingSession.getCreatedAt().toString(),
                temp,
                hum,
                lux
        );
    }

    // Lấy 100 phiên gần nhất
    public List<SessionDataDTO> getLast100Sessions() {
        List<ReadingSession> readingSessions = readingSessionRepository
                .findTop100ByOrderByCreatedAtDesc();

        List<SessionDataDTO> result = new ArrayList<>();

        for (ReadingSession readingSession : readingSessions) {
            List<DataSensor> sensors = dataSensorRepository.findBySessionId(readingSession.getId());

            float temp = 0, hum = 0, lux = 0;

            for (DataSensor ds : sensors) {
                String type = ds.getSensor().getType().toLowerCase();
                switch (type) {
                    case "temperature": temp = ds.getValue(); break;
                    case "humidity": hum = ds.getValue(); break;
                    case "lux": lux = ds.getValue(); break;
                }            }

            result.add(new SessionDataDTO(
                    readingSession.getId(),
                    readingSession.getCreatedAt().toString(),
                    temp,
                    hum,
                    lux
            ));

        }

        return result;
    }

    // Lấy 1000 phiên gần nhất
    public List<SessionDataDTO> getLast1000Sessions() {
        List<ReadingSession> readingSessions = readingSessionRepository
                .findLatest1000();

        List<SessionDataDTO> result = new ArrayList<>();

        for (ReadingSession readingSession : readingSessions) {
            List<DataSensor> sensors = dataSensorRepository.findBySessionId(readingSession.getId());

            float temp = 0, hum = 0, lux = 0;

            for (DataSensor ds : sensors) {
                String type = ds.getSensor().getType().toLowerCase();
                switch (type) {
                    case "temperature": temp = ds.getValue(); break;
                    case "humidity": hum = ds.getValue(); break;
                    case "lux": lux = ds.getValue(); break;
                }            }

            result.add(new SessionDataDTO(
                    readingSession.getId(),
                    readingSession.getCreatedAt().toString(),
                    temp,
                    hum,
                    lux
            ));

        }

        return result;
    }

    // API phân trang

    public static PagedResponseDTO<SessionDataDTO> getPagedLatest1000(
            int page, int size, String filter, String value, String sortBy, String sortDir) {

        // Lấy danh sách ReadingSession và DataSensor mới nhất
        List<ReadingSession> sessions = readingSessionRepository.findLatest1000();
        Map<Integer, List<DataSensor>> groupedSensors = dataSensorRepository.findLatest1000().stream()
                .collect(Collectors.groupingBy(ds -> ds.getSession().getId()));

        // Gộp dữ liệu cảm biến vào từng session
        List<SessionDataDTO> dtos = sessions.stream().map(session -> {
            List<DataSensor> sensors = groupedSensors.getOrDefault(session.getId(), List.of());

            float temp = 0, hum = 0, lux = 0;
            for (DataSensor ds : sensors) {
                String type = ds.getSensor().getType().toLowerCase();
                float val = ds.getValue();
                switch (type) {
                    case "temperature" -> temp = val;
                    case "humidity" -> hum = val;
                    case "lux" -> lux = val;
                }
            }

            return new SessionDataDTO(
                    session.getId(),
                    session.getCreatedAt().toString(),
                    temp, hum, lux
            );
        }).collect(Collectors.toList());

        // Nếu có điều kiện tìm kiếm
        if (filter != null && value != null && !filter.isBlank() && !value.isBlank()) {
            String keyword = value.trim().toLowerCase();

            dtos = switch (filter.toLowerCase()) {
                case "temp", "temperature" -> filterByFloat(dtos, keyword, "temp");
                case "hum", "humidity" -> filterByFloat(dtos, keyword, "hum");
                case "lux", "light", "lightlevel" -> filterByFloat(dtos, keyword, "lux");
                case "all", "createdat" -> dtos.stream()
                        .filter(d -> d.getCreatedAt() != null &&
                                d.getCreatedAt().toLowerCase().contains(keyword))
                        .toList();
                default -> dtos.stream()
                        .filter(d -> d.getCreatedAt() != null &&
                                d.getCreatedAt().toLowerCase().contains(keyword))
                        .toList();
            };
        }

        //sort
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "time";
        }
        if (sortDir == null || sortDir.isBlank()) {
            sortDir = "desc"; // mặc định
        }

        Comparator<SessionDataDTO> comparator = switch (sortBy.toLowerCase()) {
            case "temp", "temperature" -> Comparator.comparing(SessionDataDTO::getTemperature);
            case "hum", "humidity" -> Comparator.comparing(SessionDataDTO::getHumidity);
            case "lux", "light", "lightlevel" -> Comparator.comparing(SessionDataDTO::getLux);
            case "time", "createdat" -> Comparator.comparing(SessionDataDTO::getCreatedAt);
            default -> Comparator.comparing(SessionDataDTO::getCreatedAt);
        };

        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        dtos = dtos.stream().sorted(comparator).toList();

        // Phân trang thủ công
        int total = dtos.size();
        int totalPages = (int) Math.ceil((double) total / size);
        int start = Math.min((page - 1) * size, total);
        int end = Math.min(start + size, total);
        List<SessionDataDTO> paged = dtos.subList(start, end);

        return new PagedResponseDTO<>(page, size, total, totalPages, paged);
    }

    /**
     * Hàm hỗ trợ lọc theo giá trị float
     */
    private static List<SessionDataDTO> filterByFloat(List<SessionDataDTO> dtos, String keyword, String field) {
        try {
            float val = Float.parseFloat(keyword);
            return dtos.stream().filter(d -> switch (field) {
                case "temp" -> d.getTemperature() == val;
                case "hum" -> d.getHumidity() == val;
                case "lux" -> d.getLux() == val;
                default -> false;
            }).toList();
        } catch (NumberFormatException e) {
            return List.of(); // keyword không phải số => trả list rỗng
        }
    }

}

