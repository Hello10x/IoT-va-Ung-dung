package com.example.IoT_UngDung.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "datasensor")
public class DataSensor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Float value;

    @ManyToOne
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private ReadingSession session;

    // getters & setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Float getValue() {
        return value;
    }

    public void setValue(Float value) {
        this.value = value;
    }

    public Sensor getSensor() {
        return sensor;
    }

    public void setSensor(Sensor sensor) {
        this.sensor = sensor;
    }

    public ReadingSession getSession() {
        return session;
    }

    public void setSession(ReadingSession session) {
        this.session = session;
    }
}
