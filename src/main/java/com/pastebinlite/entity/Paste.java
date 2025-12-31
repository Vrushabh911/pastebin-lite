package com.pastebinlite.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class Paste {
    @Id
    private String id;
    private String content;
    private Long expiresAtMs;
    private Integer remainingViews;
    private Long createdAtMs;

    public Paste() {}

    public Paste(String content, Long expiresAtMs, Integer remainingViews) {
        this.id = UUID.randomUUID().toString();
        this.content = content;
        this.expiresAtMs = expiresAtMs;
        this.remainingViews = remainingViews;
        this.createdAtMs = System.currentTimeMillis();
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getExpiresAtMs() { return expiresAtMs; }
    public void setExpiresAtMs(Long expiresAtMs) { this.expiresAtMs = expiresAtMs; }
    public Integer getRemainingViews() { return remainingViews; }
    public void setRemainingViews(Integer remainingViews) { this.remainingViews = remainingViews; }
    public Long getCreatedAtMs() { return createdAtMs; }
    public void setCreatedAtMs(Long createdAtMs) { this.createdAtMs = createdAtMs; }
}
