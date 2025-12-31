package com.pastebinlite.controller;

import com.pastebinlite.entity.Paste;
import com.pastebinlite.repository.PasteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
public class PasteController {
    
    @Autowired
    private PasteRepository pasteRepository;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    @PostMapping("/api/pastes")
    public ResponseEntity<?> createPaste(@RequestBody Map<String, Object> request) {
        String content = (String) request.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Content required"));
        }
        
        Long ttlSeconds = parseLong(request.get("ttl_seconds"));
        Integer maxViews = parseInteger(request.get("max_views"));
        
        Long expiresAtMs = ttlSeconds != null ? System.currentTimeMillis() + (ttlSeconds * 1000) : null;
        
        String id = UUID.randomUUID().toString().substring(0, 8);
        Paste paste = new Paste(content, expiresAtMs, maxViews);
        paste.setId(id);
        pasteRepository.save(paste);
        
        String url = baseUrl + "/p/" + id;
        return ResponseEntity.ok(Map.of("id", id, "url", url));
    }
    
    @GetMapping("/api/pastes/{id}")
    public ResponseEntity<?> getPaste(@PathVariable String id, HttpServletRequest request) {
        long now = getCurrentTime(request);
        Optional<Paste> optionalPaste = pasteRepository.findById(id);
        
        if (optionalPaste.isEmpty() || isExpired(optionalPaste.get(), now)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Paste not found or expired"));
        }
        
        Paste paste = optionalPaste.get();
        if (paste.getRemainingViews() != null && paste.getRemainingViews() <= 0) {
            pasteRepository.delete(paste);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Paste expired"));
        }
        
        paste.setRemainingViews(paste.getRemainingViews() != null ? paste.getRemainingViews() - 1 : null);
        pasteRepository.save(paste);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", paste.getContent());
        response.put("remaining_views", paste.getRemainingViews());
        response.put("expires_at", paste.getExpiresAtMs() != null ? 
            java.time.Instant.ofEpochMilli(paste.getExpiresAtMs()).toString() : null);
        
        return ResponseEntity.ok(response);
    }
    
    private long getCurrentTime(HttpServletRequest request) {
        if ("1".equals(System.getenv("TEST_MODE"))) {
            String testTime = request.getHeader("x-test-now-ms");
            if (testTime != null) {
                try {
                    return Long.parseLong(testTime);
                } catch (NumberFormatException ignored) {}
            }
        }
        return System.currentTimeMillis();
    }
    
    private boolean isExpired(Paste paste, long now) {
        return (paste.getExpiresAtMs() != null && now >= paste.getExpiresAtMs()) ||
               (paste.getRemainingViews() != null && paste.getRemainingViews() <= 0);
    }
    
    private Integer parseInteger(Object value) {
        if (value == null) return null;
        try { return Integer.parseInt(value.toString()); } catch (Exception e) { return null; }
    }
    
    private Long parseLong(Object value) {
        if (value == null) return null;
        try { return Long.parseLong(value.toString()); } catch (Exception e) { return null; }
    }
}
