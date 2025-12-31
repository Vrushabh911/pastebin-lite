package com.pastebinlite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

public class CreatePasteRequest {
    @NotBlank(message = "Content is required")
    private String content;

    @Min(value = 1, message = "TTL must be >= 1")
    private Long ttlSeconds;

    @Min(value = 1, message = "Max views must be >= 1")
    private Integer maxViews;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(Long ttlSeconds) { this.ttlSeconds = ttlSeconds; }
    public Integer getMaxViews() { return maxViews; }
    public void setMaxViews(Integer maxViews) { this.maxViews = maxViews; }
}
