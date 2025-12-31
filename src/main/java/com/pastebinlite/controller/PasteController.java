package com.pastebinlite.controller;

import com.pastebinlite.model.Paste;
import com.pastebinlite.repository.PasteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping
public class PasteController {
    
    @Autowired
    private PasteRepository pasteRepository;
    
    // 🚨 FIXED: Base URL for Railway deployment
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    private final SecureRandom random = new SecureRandom();
    
    @PostMapping("/api/pastes")
    public ResponseEntity<?> createPaste(@RequestBody Map<String, Object> request) {
        String content = (String) request.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Content is required and cannot be empty"));
        }
        
        Integer ttlSeconds = parseInteger(request.get("ttl_seconds"));
        Integer maxViews = parseInteger(request.get("max_views"));
        
        if (ttlSeconds != null && ttlSeconds < 1) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "ttl_seconds must be >= 1"));
        }
        if (maxViews != null && maxViews < 1) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "max_views must be >= 1"));
        }
        
        String id = generateId();
        Paste paste = new Paste(id, content, maxViews, ttlSeconds);
        pasteRepository.save(paste);
        
        // 🚨 FIXED: Use baseUrl from application.properties
        String url = baseUrl.replace("http://localhost:8080", 
            System.getenv("RAILWAY_PUBLIC_DOMAIN") != null ? 
            "https://" + System.getenv("RAILWAY_PUBLIC_DOMAIN") : baseUrl) + "/p/" + id;
        
        return ResponseEntity.ok(Map.of("id", id, "url", url));
    }
    
    @GetMapping("/api/pastes/{id}")
    public ResponseEntity<?> getPaste(@PathVariable String id, 
                                    HttpServletRequest request) {
        Instant now = getCurrentTime(request);
        Optional<Paste> optionalPaste = pasteRepository.findById(id);
        
        if (optionalPaste.isEmpty() || optionalPaste.get().isExpired(now)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Paste not found or expired"));
        }
        
        Paste paste = optionalPaste.get();
        if (paste.getRemainingViews() != null && paste.getRemainingViews() <= 0) {
            pasteRepository.delete(paste);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Paste not found or expired"));
        }
        
        if (!paste.decrementView()) {
            pasteRepository.delete(paste);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Paste not found or expired"));
        }
        
        pasteRepository.save(paste);
        Map<String, Object> response = new HashMap<>();
        response.put("content", paste.getContent());
        response.put("remaining_views", paste.getRemainingViews());
        response.put("expires_at", paste.getExpiresAt());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/p/{id}")
    public String viewPaste(@PathVariable String id, HttpServletRequest request) {
        Instant now = getCurrentTime(request);
        Optional<Paste> optionalPaste = pasteRepository.findById(id);
        
        if (optionalPaste.isEmpty() || optionalPaste.get().isExpired(now)) {
            return """
                <!DOCTYPE html>
                <html><head><title>Not Found</title></head>
                <body><h1>404 - Paste not found or expired</h1></body></html>
                """;
        }
        
        Paste paste = optionalPaste.get();
        if (paste.getRemainingViews() != null && paste.getRemainingViews() <= 0) {
            pasteRepository.delete(paste);
            return """
                <!DOCTYPE html>
                <html><head><title>Not Found</title></head>
                <body><h1>404 - Paste not found or expired</h1></body></html>
                """;
        }
        
        if (!paste.decrementView()) {
            pasteRepository.delete(paste);
            return """
                <!DOCTYPE html>
                <html><head><title>Not Found</title></head>
                <body><h1>404 - Paste not found or expired</h1></body></html>
                """;
        }
        
        pasteRepository.save(paste);
        String content = optionalPaste.get().getContent()
            .replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
        
        return """
            <!DOCTYPE html>
            <html>
            <head><title>Paste #%s</title>
            <meta name="viewport" content="width=device-width">
            <style>
                body {max-width:800px;margin:50px auto;padding:20px;font-family:monospace;background:#1e1e1e;color:#d4d4d4;}
                pre {white-space:pre-wrap;word-wrap:break-word;background:#0d1117;padding:20px;border-radius:6px;}
                h1 {color:#58a6ff;}
            </style>
            </head>
            <body>
                <h1>Pastebin-Lite</h1>
                <pre>%s</pre>
            </body>
            </html>
            """.formatted(id, content);
    }
    
    private String generateId() {
        byte[] bytes = new byte[6];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    private Integer parseInteger(Object value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private Instant getCurrentTime(HttpServletRequest request) {
        String testMode = System.getenv("TEST_MODE");
        if ("1".equals(testMode)) {
            String testTime = request.getHeader("x-test-now-ms");
            if (testTime != null) {
                try {
                    return Instant.ofEpochMilli(Long.parseLong(testTime));
                } catch (NumberFormatException e) {
                    // Fall back to real time
                }
            }
        }
        return Instant.now();
    }
}
