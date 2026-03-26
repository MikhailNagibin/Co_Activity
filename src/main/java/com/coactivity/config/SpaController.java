package com.coactivity.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping(value = {
        "/",
        "/main",
        "/sign-in",
        "/sign-up",
        "/profile",
        "/qa",
        "/qa/new",
        "/create-room",
        "/cards/**",
        "/questions/**"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
