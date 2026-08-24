package com.example.efficientia.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ApiStatusController {

    private final String applicationName;
    private final String applicationVersion;

    public ApiStatusController(
            @Value("${spring.application.name}") String applicationName,
            @Value("${app.version:dev}") String applicationVersion
    ) {
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
    }

    @GetMapping("/status")
    public ApiStatusResponse status() {
        return new ApiStatusResponse(applicationName, "UP", applicationVersion);
    }
}
