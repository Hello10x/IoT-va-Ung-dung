package com.example.IoT_UngDung.DTO;



import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class DeviceActionDTO {
    private int id;
    private String deviceName;
    private boolean action;
    private String trigger;

    private String time;

    public DeviceActionDTO(int id, String deviceName, boolean action, String trigger, String time) {
        this.id = id;
        this.deviceName = deviceName;
        this.action = action;
        this.trigger = trigger;
        this.time = time;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public boolean isAction() {
        return action;
    }

    public void setAction(boolean action) {
        this.action = action;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}

