package com.example.IoT_UngDung.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class SessionDataDTO {
    private Integer sessionId;

    private String createdAt;

    private float temperature;
    private float humidity;
    private float lux;

    public SessionDataDTO(Integer sessionId, String createdAt,
                          float temperature, float humidity, float lux) {
        this.sessionId = sessionId;
        this.createdAt = createdAt;
        this.temperature = temperature;
        this.humidity = humidity;
        this.lux = lux;
    }

    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public float getHumidity() {
        return humidity;
    }

    public void setHumidity(float humidity) {
        this.humidity = humidity;
    }

    public float getLux() {
        return lux;
    }

    public void setLux(float lux) {
        this.lux = lux;
    }
}
