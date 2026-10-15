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
    private final String deploymentCommit;

    public ApiStatusController(
            @Value("${spring.application.name}") String applicationName,
            @Value("${app.version:dev}") String applicationVersion,
            @Value("${app.commit:unknown}") String deploymentCommit
    ) {
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
        this.deploymentCommit = deploymentCommit;
    }

    @GetMapping("/status")
    public ApiStatusResponse status() {
        return new ApiStatusResponse(applicationName, "UP", applicationVersion, deploymentCommit);
    }
}
