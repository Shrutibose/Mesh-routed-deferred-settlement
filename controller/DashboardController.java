package com.meshrouteddeferredsettlement.upi.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    @GetMapping("/")
    public String home() {
        log.info("Dashboard page requested");
        return "dashboard";
    }
}