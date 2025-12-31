package com.pastebinlite.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

@RestController
public class HealthController {
    
    @Autowired
    private DataSource dataSource;
    
    @GetMapping("/api/healthz")
    public Map<String, Boolean> healthz() {
        try (Connection conn = dataSource.getConnection()) {
            return Map.of("ok", true);
        } catch (SQLException e) {
            return Map.of("ok", false);
        }
    }
}
