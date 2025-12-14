package com.furkan.scheduler.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class healthController {
    @GetMapping(path="/health")
    public String health(){
        return "OK";
    }
}
