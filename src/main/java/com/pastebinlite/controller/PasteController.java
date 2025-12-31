package com.pastebinlite.controller;

import com.pastebinlite.dto.CreatePasteRequest;
import com.pastebinlite.entity.Paste;
import com.pastebinlite.repository.PasteRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PasteController {

    @Autowired
    private PasteRepository pasteRepository;

    @GetMapping("/healthz")
    public Map<String, Boolean> healthz() {
        try {
            pasteRepository.count();
            return Map.of("ok", true);
        } catch (Exception e) {
            return Map.of("ok", false);
        }
    }

    @PostMapping("/pastes")
    public ResponseEntity<?> createPaste(@Valid @RequestBody CreatePasteRequest request, HttpServletRequest httpReq) {
        String scheme = httpReq.getScheme();
        String host = httpReq.getServerName();
        int port = httpReq.getServerPort() == 0 ? 80 : httpReq.getServerPort();
        String baseUrl = String.format("%s://%s:%d", scheme, host, port);

        Long expiresAtMs = null;
        if (request.getTtlSeconds() != null) {
            long now = getCurrentTime(httpReq);
            expiresAtMs = now + (request.getTtlSeconds() * 1000L);
        }

        Paste paste = new Paste(request.getContent(), expiresAtMs, request.getMaxViews());
        pasteRepository.save(paste);

        String url = baseUrl + "/p/" + paste.getId();
        Map<String, String> response = Map.of(
                "id", paste.getId(),
                "url", url
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pastes/{id}")
    public ResponseEntity<?> getPaste(@PathVariable String id, HttpServletRequest httpReq) {
        return pasteRepository.findById(id)
                .map(paste -> {
                    long now = getCurrentTime(httpReq);
                    if (isExpired(paste, now)) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Paste not found"));
                    }

                    if (paste.getRemainingViews() != null) {
                        paste.setRemainingViews(paste.getRemainingViews() - 1);
                        pasteRepository.save(paste);
                    }

                    // FIXED: Return Map instead of PasteResponse
                    Map<String, Object> response = new HashMap<>();
                    response.put("content", paste.getContent());
                    response.put("remainingViews", paste.getRemainingViews());
                    if (paste.getExpiresAtMs() != null) {
                        response.put("expiresAt", java.time.Instant.ofEpochMilli(paste.getExpiresAtMs()).toString());
                    }
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Paste not found")));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.badRequest().body(errors);
    }

    private long getCurrentTime(HttpServletRequest request) {
        if ("1".equals(System.getenv("TEST_MODE"))) {
            String testTimeHeader = request.getHeader("x-test-now-ms");
            if (testTimeHeader != null) {
                try {
                    return Long.parseLong(testTimeHeader);
                } catch (NumberFormatException e) {
                }
            }
        }
        return System.currentTimeMillis();
    }

    private boolean isExpired(Paste paste, long now) {
        if (paste.getExpiresAtMs() != null && now >= paste.getExpiresAtMs()) {
            return true;
        }
        return paste.getRemainingViews() != null && paste.getRemainingViews() <= 0;
    }
}
