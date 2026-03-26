package com.coactivity.controller.impl;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageControllerImpl {

    @GetMapping("/")
    public String home() {
        return "redirect:/main";
    }

    @GetMapping("/main")
    public String mainPage() {
        return "main";
    }

    @GetMapping("/qa-page")
    public String qaPage() {
        return "qa-page";
    }

    @GetMapping("/sign-in")
    public String signIn() {
        return "sign-in";
    }

    @GetMapping("/sign-up")
    public String signUp() {
        return "sign-up";
    }

    @GetMapping("/room/{roomId}")
    public String roomDetails(@PathVariable Integer roomId, Model model) {
        model.addAttribute("roomId", roomId);
        return "default-card0";
    }

    @GetMapping("/question/{questionId}")
    public String questionDetails(@PathVariable Integer questionId, Model model) {
        model.addAttribute("questionId", questionId);
        return "default-question0";
    }

    @GetMapping("/ask-question")
    public String askQuestion() {
        return "ask-question"; // Создайте эту страницу, если нужно
    }

    @ExceptionHandler(Exception.class)
    public String handleError(Model model, Exception e) {
        model.addAttribute("error", e.getMessage());
        return "error";
    }
}