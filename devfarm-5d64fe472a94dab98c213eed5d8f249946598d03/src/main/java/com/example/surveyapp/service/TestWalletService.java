package com.example.surveyapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Test service that demonstrates how to use the TenderlyBalanceService
 * for setting wallet balances in test environments
 */
@Service
public class TestWalletService {

    private static final Logger logger = LoggerFactory.getLogger(TestWalletService.class);
    
    @Autowired
    private TenderlyBalanceService tenderlyBalanceService;
    
    /**
     * Sets a predefined amount of test ETH to a wallet for testing purposes
     * 
     * @param walletAddress The wallet address to fund
     * @param amount The amount of test ETH to add
     * @return true if balance was set successfully, false otherwise
     */
    public boolean fundTestWallet(String walletAddress, int amount) {
        logger.info("Funding test wallet {} with {} ETH", walletAddress, amount);
        
        if (walletAddress == null || walletAddress.isEmpty()) {
            logger.error("Invalid wallet address");
            return false;
        }
        
        if (amount <= 0 || amount > 1000) {
            logger.error("Invalid amount: {}. Must be between 1 and 1000", amount);
            return false;
        }
        
        try {
            // Use the Tenderly Balance Service to set wallet balance
            boolean success = tenderlyBalanceService.setBalanceEth(walletAddress, amount);
            
            if (success) {
                logger.info("Successfully funded test wallet {} with {} ETH", walletAddress, amount);
            } else {
                logger.error("Failed to fund test wallet");
            }
            
            return success;
        } catch (Exception e) {
            logger.error("Error funding test wallet: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Sets a specific ETH amount in wei (hexadecimal format)
     * 
     * @param walletAddress The wallet address to set balance for
     * @param weiAmountHex The amount in wei as hex string (e.g. "0x56BC75E2D63100000")
     * @return true if balance was set successfully, false otherwise
     */
    public boolean setExactBalance(String walletAddress, String weiAmountHex) {
        logger.info("Setting exact balance for wallet {} to {}", walletAddress, weiAmountHex);
        
        if (walletAddress == null || walletAddress.isEmpty()) {
            logger.error("Invalid wallet address");
            return false;
        }
        
        if (weiAmountHex == null || weiAmountHex.isEmpty() || !weiAmountHex.startsWith("0x")) {
            logger.error("Invalid wei amount: {}. Must be a hex string starting with 0x", weiAmountHex);
            return false;
        }
        
        try {
            // Use the Tenderly Balance Service to set wallet balance with exact hex amount
            boolean success = tenderlyBalanceService.setBalance(walletAddress, weiAmountHex);
            
            if (success) {
                logger.info("Successfully set exact balance for wallet {} to {}", walletAddress, weiAmountHex);
            } else {
                logger.error("Failed to set exact balance");
            }
            
            return success;
        } catch (Exception e) {
            logger.error("Error setting exact balance: {}", e.getMessage(), e);
            return false;
        }
    }
} 