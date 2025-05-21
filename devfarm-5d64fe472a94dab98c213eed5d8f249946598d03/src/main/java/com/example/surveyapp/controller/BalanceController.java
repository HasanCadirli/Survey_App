package com.example.surveyapp.controller;

import com.example.surveyapp.service.TenderlyBalanceService;
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

@RestController
@RequestMapping("/api/balance")
public class BalanceController {

    private static final Logger logger = LoggerFactory.getLogger(BalanceController.class);

    @Autowired
    private TenderlyBalanceService tenderlyBalanceService;

    /**
     * Endpoint to set ETH balance for a wallet address
     * 
     * @param request Contains wallet address and ETH amount
     * @return Response indicating success or failure
     */
    @PostMapping("/set")
    public ResponseEntity<Map<String, Object>> setWalletBalance(@RequestBody Map<String, String> request) {
        String walletAddress = request.get("walletAddress");
        String ethAmount = request.get("ethAmount");
        
        logger.info("Received request to set balance: wallet={}, amount={}", walletAddress, ethAmount);
        
        Map<String, Object> response = new HashMap<>();
        
        // Validate request parameters
        if (walletAddress == null || walletAddress.isEmpty() || ethAmount == null || ethAmount.isEmpty()) {
            logger.warn("Invalid request parameters");
            response.put("success", false);
            response.put("message", "Invalid request parameters. walletAddress and ethAmount are required.");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            // Convert ethAmount to integer
            int ethAmountInt = Integer.parseInt(ethAmount);
            
            // Set balance using service
            boolean success = tenderlyBalanceService.setBalanceEth(walletAddress, ethAmountInt);
            
            if (success) {
                response.put("success", true);
                response.put("message", "Successfully set balance of " + ethAmount + " ETH for wallet " + walletAddress);
            } else {
                response.put("success", false);
                response.put("message", "Failed to set wallet balance");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (NumberFormatException e) {
            logger.error("Invalid ETH amount format: {}", ethAmount, e);
            response.put("success", false);
            response.put("message", "Invalid ETH amount format. Must be a valid integer.");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Error setting wallet balance: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error setting wallet balance: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 