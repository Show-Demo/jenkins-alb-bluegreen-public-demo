package com.example.order.controller;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/")
    public Map<String, String> index() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("service", "order-api");
        info.put("color", getenv("RELEASE_COLOR", "unknown"));
        info.put("version", getenv("RELEASE_VERSION", "local"));
        info.put("hostname", hostname());
        return info;
    }

    @GetMapping("/version")
    public Map<String, String> version() {
        return index();
    }

    private String getenv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    private String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }
}
