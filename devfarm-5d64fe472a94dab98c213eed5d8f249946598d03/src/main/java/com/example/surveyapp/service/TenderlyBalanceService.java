package com.example.surveyapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class TenderlyBalanceService {
    
    private static final Logger logger = LoggerFactory.getLogger(TenderlyBalanceService.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    // URL'yi doğrudan kod içinde tanımlama
    private final String tenderlyRpcUrl = "https://virtual.sepolia.rpc.tenderly.co/d88ec9b6-956d-486e-b852-76fcd37861b3";
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public TenderlyBalanceService() {
        // Timeout süreleri arttırıldı
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
        logger.info("TenderlyBalanceService başlatıldı. RPC URL: {}", tenderlyRpcUrl);
    }
    
    /**
     * Get the current ETH balance for a wallet address
     * @param walletAddress Ethereum wallet address to get balance for
     * @return Balance in wei as a BigInteger, or null if failed
     */
    public BigInteger getBalance(String walletAddress) {
        logger.info("Getting wallet balance for address: {}", walletAddress);
        
        try {
            // Cüzdan adresini kontrol et - 0x ile başlamalı
            if (!walletAddress.startsWith("0x")) {
                walletAddress = "0x" + walletAddress;
                logger.info("Wallet address corrected to: {}", walletAddress);
            }
            
            // Prepare JSON-RPC request body
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("jsonrpc", "2.0");
            requestMap.put("method", "eth_getBalance");
            requestMap.put("id", 1);
            requestMap.put("params", new String[]{walletAddress, "latest"});
            
            String requestBody = objectMapper.writeValueAsString(requestMap);
            logger.info("Request JSON: {}", requestBody);
            
            // Create request
            Request request = new Request.Builder()
                    .url(tenderlyRpcUrl)
                    .post(RequestBody.create(requestBody, JSON))
                    .header("Content-Type", "application/json")
                    .build();
            
            // Execute request
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "Empty response";
                
                if (!response.isSuccessful()) {
                    logger.error("HTTP error getting balance: {} {}, Body: {}", 
                            response.code(), response.message(), responseBody);
                    return null;
                }
                
                logger.info("Get balance response: {}", responseBody);
                
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                
                // Check for errors in response
                if (responseMap.containsKey("error")) {
                    logger.error("Error from Tenderly API: {}", responseMap.get("error"));
                    return null;
                }
                
                // Parse result
                String balanceHex = (String) responseMap.get("result");
                if (balanceHex != null && balanceHex.startsWith("0x")) {
                    BigInteger balance = new BigInteger(balanceHex.substring(2), 16);
                    logger.info("Current balance for {}: {} wei", walletAddress, balance);
                    return balance;
                } else {
                    logger.error("Invalid balance format: {}", balanceHex);
                    return null;
                }
            }
            
        } catch (Exception e) {
            logger.error("Error getting wallet balance: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Sets the ETH balance for a wallet address using Tenderly's virtual RPC
     * @param walletAddress Ethereum wallet address to set balance for
     * @param ethAmountHex Hex string representing the amount of ETH (e.g. "0x56BC75E2D63100000" for 100 ETH)
     * @return Response status indicating success or failure
     */
    public boolean setBalance(String walletAddress, String ethAmountHex) {
        logger.info("Setting wallet balance for address: {} to amount: {} using URL: {}", 
                walletAddress, ethAmountHex, tenderlyRpcUrl);
        
        try {
            // Cüzdan adresini kontrol et - 0x ile başlamalı
            if (!walletAddress.startsWith("0x")) {
                walletAddress = "0x" + walletAddress;
                logger.info("Wallet address corrected to: {}", walletAddress);
            }
            
            // Prepare JSON-RPC request body
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("jsonrpc", "2.0");
            requestMap.put("method", "tenderly_setBalance");
            requestMap.put("id", 1);
            
            // Parameters: [address, balance]
            requestMap.put("params", new String[]{walletAddress, ethAmountHex});
            
            String requestBody = objectMapper.writeValueAsString(requestMap);
            logger.info("Request JSON: {}", requestBody);
            
            // Create request
            Request request = new Request.Builder()
                    .url(tenderlyRpcUrl)
                    .post(RequestBody.create(requestBody, JSON))
                    .header("Content-Type", "application/json")
                    .build();
            
            // Execute request
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "Empty response";
                
                if (!response.isSuccessful()) {
                    logger.error("HTTP error: {} {}, Body: {}", 
                            response.code(), response.message(), responseBody);
                    return false;
                }
                
                logger.info("Response body: {}", responseBody);
                
                // Boş yanıt kontrolü
                if (responseBody == null || responseBody.trim().isEmpty()) {
                    logger.error("Empty response from Tenderly API");
                    return false;
                }
                
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                
                // Check for errors in response
                if (responseMap.containsKey("error")) {
                    logger.error("Error from Tenderly API: {}", responseMap.get("error"));
                    return false;
                }
                
                logger.info("Successfully set balance for wallet: {}", walletAddress);
                return true;
            }
            
        } catch (Exception e) {
            logger.error("Error setting wallet balance: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Convenience method to set balance with a decimal ETH amount
     * @param walletAddress Ethereum wallet address to set balance for
     * @param ethAmount Decimal amount of ETH to set
     * @return Response status indicating success or failure
     */
    public boolean setBalanceEth(String walletAddress, int ethAmount) {
        // Convert decimal ETH to hex string (1 ETH = 10^18 wei)
        String ethHex = "0x" + BigInteger.TEN.pow(18)
                .multiply(BigInteger.valueOf(ethAmount))
                .toString(16);
        
        logger.info("Converting {} ETH to hex: {}", ethAmount, ethHex);
        return setBalance(walletAddress, ethHex);
    }
    
    /**
     * Adds ETH to a wallet address (gets current balance and adds to it)
     * @param walletAddress Ethereum wallet address to add balance to
     * @param ethAmount Decimal amount of ETH to add
     * @return Response status indicating success or failure
     */
    public boolean addBalanceEth(String walletAddress, int ethAmount) {
        logger.info("Adding {} ETH to wallet: {}", ethAmount, walletAddress);
        
        try {
            // Format wallet address
            if (!walletAddress.startsWith("0x")) {
                walletAddress = "0x" + walletAddress;
            }
            
            // First get current balance
            BigInteger currentBalance = getBalance(walletAddress);
            if (currentBalance == null) {
                logger.error("Failed to get current balance for wallet: {}", walletAddress);
                return false;
            }
            
            // Calculate new balance
            BigInteger ethInWei = BigInteger.TEN.pow(18).multiply(BigInteger.valueOf(ethAmount));
            BigInteger newBalance = currentBalance.add(ethInWei);
            String newBalanceHex = "0x" + newBalance.toString(16);
            
            logger.info("Current balance: {} wei, Adding: {} wei, New balance: {} wei", 
                    currentBalance, ethInWei, newBalance);
            
            // Set the new balance
            return setBalance(walletAddress, newBalanceHex);
            
        } catch (Exception e) {
            logger.error("Error adding ETH to wallet: {}", e.getMessage(), e);
            return false;
        }
    }
} 