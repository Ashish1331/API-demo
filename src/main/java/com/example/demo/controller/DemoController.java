package com.example.demo.controller;

import com.example.demo.service.DemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("demo")
public class DemoController {

    @GetMapping("/ads")
    public String getAdsCount() {
        return "Current ads count: " + DemoService.getAdsCount();
    }
}