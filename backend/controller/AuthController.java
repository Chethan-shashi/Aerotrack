package com.aerotrack.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        // Basic validation to connect the Angular frontend to the backend
        if ("admin".equals(username) && "admin_password".equals(password)) {
            // Returns a simple token that Angular saves to localStorage
            return ResponseEntity.ok(Map.of("token", "aerotrack-module3-test-token"));
        } else {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }
}