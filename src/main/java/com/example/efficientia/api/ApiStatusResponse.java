package com.example.efficientia.api;

public record ApiStatusResponse(
        String service,
        String status,
        String version
) {
}
