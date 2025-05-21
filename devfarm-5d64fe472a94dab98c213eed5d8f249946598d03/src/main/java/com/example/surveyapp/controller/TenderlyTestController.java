package com.example.surveyapp.controller;

import com.example.surveyapp.service.TenderlyBalanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Tenderly servisini test etmek için basit controller
 */
@RestController
@RequestMapping("/api/tenderly/test")
public class TenderlyTestController {

    private static final Logger logger = LoggerFactory.getLogger(TenderlyTestController.class);

    @Autowired
    private TenderlyBalanceService tenderlyBalanceService;
    
    // URL'yi sabit tanımlama
    private final String tenderlyRpcUrl = "https://virtual.sepolia.rpc.tenderly.co/d88ec9b6-956d-486e-b852-76fcd37861b3";

    /**
     * Test amaçlı basit endpoint - cüzdana 1 ETH yükler
     */
    @GetMapping("/fund/{walletAddress}")
    public ResponseEntity<Map<String, Object>> testFund(@PathVariable String walletAddress) {
        logger.info("Test endpoint called for wallet: {}", walletAddress);
        
        Map<String, Object> response = new HashMap<>();
        response.put("walletAddress", walletAddress);
        response.put("amount", 1);
        response.put("tenderlyUrl", tenderlyRpcUrl);
        
        try {
            // Önce mevcut bakiyeyi kontrol et
            BigInteger oldBalance = tenderlyBalanceService.getBalance(walletAddress);
            Double oldEthBalance = null;
            if (oldBalance != null) {
                oldEthBalance = oldBalance.doubleValue() / Math.pow(10, 18);
                response.put("oldBalance", oldEthBalance);
            }
            
            // Bakiye ekle (set yerine add kullan)
            boolean success = tenderlyBalanceService.addBalanceEth(walletAddress, 1);
            
            if (success) {
                response.put("success", true);
                response.put("message", "Test başarılı! 1 ETH cüzdana eklendi");
                
                // Yeni bakiyeyi kontrol et
                BigInteger newBalance = tenderlyBalanceService.getBalance(walletAddress);
                if (newBalance != null) {
                    double newEthBalance = newBalance.doubleValue() / Math.pow(10, 18);
                    response.put("newBalance", newEthBalance);
                    
                    if (oldEthBalance != null) {
                        response.put("difference", newEthBalance - oldEthBalance);
                    }
                }
            } else {
                response.put("success", false);
                response.put("message", "Test başarısız. Loglara bakınız.");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Test sırasında hata: ", e);
            response.put("success", false);
            response.put("message", "Hata: " + e.getMessage());
            response.put("error", e.getClass().getName());
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 