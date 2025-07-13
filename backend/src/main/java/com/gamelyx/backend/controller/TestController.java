package com.gamelyx.backend.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class TestController {

    @GetMapping("/hello")
    public String hello() {
        return "¡Hola desde Gamelyx Backend! 🎮";
    }

    @GetMapping("/status")
    public String status() {
        return "Backend funcionando correctamente ✅";
    }
}