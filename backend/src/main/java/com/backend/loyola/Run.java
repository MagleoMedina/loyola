package com.backend.loyola;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Run {

    @GetMapping("/run")
    public String run() {
        return "Running...";
    }
}
