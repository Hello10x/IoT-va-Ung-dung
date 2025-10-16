package com.example.IoT_UngDung.Service;

import com.example.IoT_UngDung.Entity.*;
import com.example.IoT_UngDung.Repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
public class MqttService {

    private final MqttClient mqttClient;
    private final SensorRepository sensorRepo;
    private final ReadingSessionRepository sessionRepo;
    private final DataSensorRepository dataRepo;
    private final DeviceRepository deviceRepo;
    private final DeviceActionRepository deviceActionRepo;

    // Các topic MQTT
    private static final String SENSOR_TOPIC = "sensor/data";
    private static final String ACTION_TOPIC = "device/action";
    private static final String RESULT_TOPIC = "action/result";

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Map lưu các action đang chờ phản hồi từ ESP
    private final Map<String, CompletableFuture<Boolean>> pendingActions = new ConcurrentHashMap<>();

    // Danh sách các thiết bị đang được khởi tạo (không ghi log)
    private final Set<String> initializingDevices = ConcurrentHashMap.newKeySet();


    public MqttService(MqttClient mqttClient,
                       SensorRepository sensorRepo,
                       ReadingSessionRepository sessionRepo,
                       DataSensorRepository dataRepo,
                       DeviceRepository deviceRepo,
                       DeviceActionRepository deviceActionRepo) {
        this.mqttClient = mqttClient;
        this.sensorRepo = sensorRepo;
        this.sessionRepo = sessionRepo;
        this.dataRepo = dataRepo;
        this.deviceRepo = deviceRepo;
        this.deviceActionRepo = deviceActionRepo;
    }

    // Khởi động service và đăng ký lắng nghe topic MQTT
    @PostConstruct
    public void subscribe() throws Exception {
        // Lắng nghe dữ liệu cảm biến
        mqttClient.subscribe(SENSOR_TOPIC, (topic, message) -> handleSensorMessage(message));

        // Lắng nghe kết quả phản hồi hành động thiết bị
        mqttClient.subscribe(RESULT_TOPIC, (topic, message) -> handleResultMessage(message));

        // --- Lắng nghe yêu cầu đồng bộ trạng thái ---
        mqttClient.subscribe("device/state/request", (topic, message) -> {
            String payload = new String(message.getPayload());
            System.out.println(" ESP yêu cầu đồng bộ trạng thái: " + payload);
            handleStateRequest();
        });

        // Gửi trạng thái khởi tạo của thiết bị khi khởi động server
        initDeviceState();
    }
    private void handleStateRequest() {
        try {
            List<Device> devices = deviceRepo.findAll();
            if (devices.isEmpty()) {
                System.out.println("⚠️ Không có thiết bị nào trong database.");
                return;
            }

            Map<String, String> stateMap = new HashMap<>();
            for (Device d : devices) {
                stateMap.put(d.getName(), d.isStatus() ? "ON" : "OFF");
            }

            String json = objectMapper.writeValueAsString(stateMap);
            MqttMessage msg = new MqttMessage(json.getBytes(StandardCharsets.UTF_8));
            msg.setQos(1);

            mqttClient.publish("device/state", msg);
            System.out.println("✅ Gửi lại trạng thái thiết bị: " + json);

        } catch (Exception e) {
            System.err.println("❌ Lỗi khi gửi trạng thái thiết bị: " + e.getMessage());
        }
    }
    // Xử lý dữ liệu cảm biến gửi về
    private void handleSensorMessage(MqttMessage message) {
        try {
            String payload = new String(message.getPayload());
            System.out.println("Received SENSOR MQTT: " + payload);

            JsonNode root = objectMapper.readTree(payload);
            ReadingSession session = new ReadingSession();
            sessionRepo.save(session);

            List<String> keys = Arrays.asList("temperature", "humidity", "lux");

            for (String key : keys) {
                if (root.has(key)) {
                    float value = root.get(key).floatValue();

                    Sensor sensor = sensorRepo.findByType(key).orElseGet(() -> {
                        Sensor s = new Sensor();
                        s.setName(key.toUpperCase());
                        s.setType(key);
                        s.setUnit(key.equals("temperature") ? "°C" :
                                key.equals("humidity") ? "%" : "lux");
                        return sensorRepo.save(s);
                    });

                    DataSensor data = new DataSensor();
                    data.setValue(value);
                    data.setSensor(sensor);
                    data.setSession(session);
                    dataRepo.save(data);

//                    System.out.println("Saved sensor: " + key + " = " + value);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing SENSOR JSON: " + e.getMessage());
        }
    }

    // Xử lý kết quả hành động từ ESP gửi về
    private void handleResultMessage(MqttMessage message) {
        try {
            String payload = new String(message.getPayload());
            System.out.println("Received RESULT MQTT: " + payload);

            try {
                JsonNode root = objectMapper.readTree(payload);

                String deviceName = root.get("device").asText();
                String action = root.get("action").asText();
                boolean success = root.get("result").asBoolean();

                // Thêm source: nếu ESP không gửi, mặc định là "Manual"
                String source = root.has("source") ? root.get("source").asText() : "Manual";

                // Hoàn tất future đang chờ (nếu là hành động người dùng)
                CompletableFuture<Boolean> future = pendingActions.remove(deviceName);
                if (future != null) {
                    future.complete(success);
                    System.out.println("Completed future for " + deviceName + " = " + success);
                }

                // Chỉ lưu nếu KHÔNG phải source = "Sync"
                if (!"Sync".equalsIgnoreCase(source)) {
                    handleResultToDatabase(deviceName, action, source, success);
                } else {
                    System.out.println("Auto - reconnect " + deviceName);
                }

            } catch (Exception e) {
                System.err.println("Error parsing result JSON: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Error parsing RESULT JSON: " + e.getMessage());
        }
    }

    // Gửi lệnh MQTT và chờ phản hồi tối đa 10 giây
    public boolean publishDeviceActionAndWait(String key, boolean state) throws Exception {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        pendingActions.put(key, future);

        // Gửi lệnh MQTT
        Map<String, String> msg = new HashMap<>();
        msg.put(key, state ? "ON" : "OFF");
        String json = objectMapper.writeValueAsString(msg);
        MqttMessage mqttMessage = new MqttMessage(json.getBytes(StandardCharsets.UTF_8));
        mqttMessage.setQos(1);

        mqttClient.publish(ACTION_TOPIC, mqttMessage);
        System.out.println("Published ACTION: " + json);

        try {
            // Chờ phản hồi tối đa 10 giây
            return future.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            pendingActions.remove(key);
            System.err.println("Timeout: No result for " + key);
            throw e;
        }
    }

    // ✅ Lưu kết quả vào database khi có phản hồi từ ESP
    private void handleResultToDatabase(String deviceName, String action,String source, boolean success) {
        try {
            if (initializingDevices.contains(deviceName)) {
                System.out.println("Device đang khởi tạo: " + deviceName + ")");
                initializingDevices.remove(deviceName);
                return;
            }

            Device device = deviceRepo.findByName(deviceName).orElseThrow(() -> new RuntimeException("No device found with name: " + deviceName));
            if (device == null) {
                device = new Device();
                device.setName(deviceName);
                device.setStatus(false);
                deviceRepo.save(device);
            }

            if (success) {
                device.setStatus(action.equalsIgnoreCase("ON"));
                deviceRepo.save(device);
            }

            DeviceAction log = new DeviceAction();
            log.setDevice(device);
            log.setAction(action.equalsIgnoreCase("ON"));
            log.setSource(source);
            log.setTime(LocalDateTime.now());
            deviceActionRepo.save(log);

            System.out.println("💾 Saved DeviceAction for " + deviceName);

        } catch (Exception e) {
            System.err.println("❌ Error saving result: " + e.getMessage());
        }
    }


    // Khởi tạo trạng thái thiết bị từ database khi khởi động
    private void initDeviceState() {
        try {
            List<Device> devices = deviceRepo.findAll();
            if (devices.isEmpty()) {
                System.out.println("⚠️ Không có thiết bị nào trong database để khởi tạo.");
                return;
            }

            Map<String, String> initMap = new HashMap<>();
            for (Device d : devices) {
                initMap.put(d.getName(), d.isStatus() ? "ON" : "OFF");

                initializingDevices.add(d.getName());
            }

            String json = objectMapper.writeValueAsString(initMap);
            MqttMessage msg = new MqttMessage(json.getBytes(StandardCharsets.UTF_8));
            msg.setQos(1);

            mqttClient.publish(ACTION_TOPIC, msg);
            System.out.println("Khởi tạo trạng thái thiết bị: " + json);

        } catch (Exception e) {
            System.err.println("Error sending initial device state: " + e.getMessage());
        }
    }
}
