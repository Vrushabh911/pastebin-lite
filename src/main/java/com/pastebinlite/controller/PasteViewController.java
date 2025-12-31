package com.pastebinlite.controller;

import com.pastebinlite.entity.Paste;
import com.pastebinlite.repository.PasteRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PasteViewController {

    @Autowired
    private PasteRepository pasteRepository;

    @GetMapping("/")
    public String index() {
        return "create";
    }

    @GetMapping("/p/{id}")
    public String viewPaste(@PathVariable String id, Model model, HttpServletRequest request) {
        return pasteRepository.findById(id)
                .filter(paste -> {
                    long now = getCurrentTime(request);
                    return !isExpired(paste, now);
                })
                .map(paste -> {
                    if (paste.getRemainingViews() != null) {
                        paste.setRemainingViews(paste.getRemainingViews() - 1);
                        pasteRepository.save(paste);
                    }
                    model.addAttribute("paste", paste);
                    return "view";
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "Paste not found or expired");
                    return "view";
                });
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
