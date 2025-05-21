package com.example.surveyapp.controller;

import com.example.surveyapp.model.User;
import com.example.surveyapp.service.RewardService;
import com.example.surveyapp.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/rewards")
public class RewardController {

    private static final Logger logger = LoggerFactory.getLogger(RewardController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private RewardService rewardService;

    @GetMapping
    public String showRewardsPage(Model model, HttpSession session) {
        logger.info("GET /rewards - Showing rewards page");
        
        // Kullanıcı giriş yapmış mı kontrol et
        String loggedInUserEmail = (String) session.getAttribute("loggedInUser");
        if (loggedInUserEmail == null) {
            logger.warn("No logged-in user found, redirecting to login");
            return "redirect:/login?error=" + URLEncoder.encode("Lütfen önce giriş yapın.", StandardCharsets.UTF_8);
        }
        
        // Kullanıcı bilgilerini getir
        User user = userService.findUserByEmail(loggedInUserEmail);
        
        // Model'e veri ekle
        model.addAttribute("user", user);
        model.addAttribute("loggedInUser", loggedInUserEmail);
        model.addAttribute("userPoints", user.getPoints());
        model.addAttribute("hasWallet", user.getWalletAddress() != null && !user.getWalletAddress().isEmpty());
        model.addAttribute("walletAddress", user.getWalletAddress());
        
        // Test ağı bilgilerini gönder
        model.addAttribute("chainId", "11155112");
        model.addAttribute("chainName", "Sepolia Testnet");
        model.addAttribute("rpcUrl", "https://virtual.sepolia.rpc.tenderly.co/d88ec9b6-956d-4d96-b852-76fcd37861b3");
        
        return "rewards";
    }
    
    @PostMapping("/convert")
    public String convertPointsToEth(@RequestParam("points") int points, 
                                    @RequestParam("walletAddress") String walletAddress,
                                    HttpSession session, Model model) {
        logger.info("POST /rewards/convert - Converting points to ETH");
        
        // Kullanıcı giriş yapmış mı kontrol et
        String loggedInUserEmail = (String) session.getAttribute("loggedInUser");
        if (loggedInUserEmail == null) {
            logger.warn("No logged-in user found, redirecting to login");
            return "redirect:/login?error=" + URLEncoder.encode("Lütfen önce giriş yapın.", StandardCharsets.UTF_8);
        }
        
        try {
            // Kullanıcı bilgilerini getir
            User user = userService.findUserByEmail(loggedInUserEmail);
            
            // Puanları ETH'ye çevir
            var result = rewardService.convertPointsToEth(user, walletAddress, points);
            
            if ((boolean) result.get("success")) {
                logger.info("Successfully converted points to ETH for user: {}", loggedInUserEmail);
                return "redirect:/rewards?success=" + URLEncoder.encode((String) result.get("message"), StandardCharsets.UTF_8);
            } else {
                logger.warn("Failed to convert points to ETH: {}", result.get("message"));
                return "redirect:/rewards?error=" + URLEncoder.encode((String) result.get("message"), StandardCharsets.UTF_8);
            }
            
        } catch (Exception e) {
            logger.error("Error converting points to ETH: {}", e.getMessage(), e);
            return "redirect:/rewards?error=" + URLEncoder.encode("İşlem sırasında bir hata oluştu: " + e.getMessage(), StandardCharsets.UTF_8);
        }
    }
} 