package com.example.surveyapp.controller;

import com.example.surveyapp.service.TestWalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for wallet testing operations - should be disabled in production
 */
@RestController
@RequestMapping("/api/test/wallet")
public class TestWalletController {

    private static final Logger logger = LoggerFactory.getLogger(TestWalletController.class);

    @Autowired
    private TestWalletService testWalletService;

    /**
     * Fund a test wallet with the specified amount
     */
    @PostMapping("/fund")
    public ResponseEntity<Map<String, Object>> fundTestWallet(@RequestBody Map<String, String> request) {
        String walletAddress = request.get("walletAddress");
        String amount = request.get("amount");
        
        logger.info("Received request to fund test wallet: wallet={}, amount={}", walletAddress, amount);
        
        Map<String, Object> response = new HashMap<>();
        
        if (walletAddress == null || walletAddress.isEmpty() || amount == null || amount.isEmpty()) {
            response.put("success", false);
            response.put("message", "Invalid request. walletAddress and amount are required.");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            int amountInt = Integer.parseInt(amount);
            boolean success = testWalletService.fundTestWallet(walletAddress, amountInt);
            
            if (success) {
                response.put("success", true);
                response.put("message", "Successfully funded test wallet with " + amount + " ETH");
            } else {
                response.put("success", false);
                response.put("message", "Failed to fund test wallet");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("message", "Invalid amount format. Must be a valid integer.");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error funding test wallet: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Set exact balance with hex amount
     */
    @PostMapping("/set-exact")
    public ResponseEntity<Map<String, Object>> setExactBalance(@RequestBody Map<String, String> request) {
        String walletAddress = request.get("walletAddress");
        String hexAmount = request.get("hexAmount");
        
        logger.info("Received request to set exact balance: wallet={}, hexAmount={}", walletAddress, hexAmount);
        
        Map<String, Object> response = new HashMap<>();
        
        if (walletAddress == null || walletAddress.isEmpty() || hexAmount == null || hexAmount.isEmpty()) {
            response.put("success", false);
            response.put("message", "Invalid request. walletAddress and hexAmount are required.");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            boolean success = testWalletService.setExactBalance(walletAddress, hexAmount);
            
            if (success) {
                response.put("success", true);
                response.put("message", "Successfully set exact balance for wallet");
            } else {
                response.put("success", false);
                response.put("message", "Failed to set exact balance");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error setting exact balance: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 