package com.bobo.aismartcloud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;

@Tag(name = "Health监测")
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    @Operation(summary = "健康检查")
    public String healthCheck() {
        return "ok";
    }
}
