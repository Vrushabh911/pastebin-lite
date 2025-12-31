package com.pastebinlite.dto;


import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PasteRepository {
    private String content;
    private Integer remainingViews;
    private String expiresAt;

    public PasteRepository(String content, Integer remainingViews, Long expiresAtMs) {
        this.content = content;
        this.remainingViews = remainingViews;
        if (expiresAtMs != null) {
            this.expiresAt = java.time.Instant.ofEpochMilli(expiresAtMs).toString();
        }
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getRemainingViews() { return remainingViews; }
    public void setRemainingViews(Integer remainingViews) { this.remainingViews = remainingViews; }
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
}
