package com.example.surveyapp.controller;

import com.example.surveyapp.service.TenderlyBalanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Basit test kontrolcüsü - Tenderly ayarlarını test etmek için
 */
@RestController
@RequestMapping("/test-balance")
public class TestBalanceController {

    private static final Logger logger = LoggerFactory.getLogger(TestBalanceController.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final String tenderlyRpcUrl = "https://virtual.sepolia.rpc.tenderly.co/d88ec9b6-956d-486e-b852-76fcd37861b3";
    
    @Autowired
    private TenderlyBalanceService tenderlyBalanceService;
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public TestBalanceController() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * En basit test - sadece true/false döner
     */
    @GetMapping("/simple")
    public String simpleTest(@RequestParam String address) {
        try {
            logger.info("Basit test çağrıldı: {}", address);
            boolean result = tenderlyBalanceService.setBalanceEth(address, 1);
            logger.info("Test sonucu: {}", result);
            return "Test sonucu: " + result;
        } catch (Exception e) {
            logger.error("Test hatası: ", e);
            return "HATA: " + e.getMessage() + " (" + e.getClass().getName() + ")";
        }
    }
    
    /**
     * Bakiye sorgulama testi
     */
    @GetMapping("/get-balance")
    public String getBalanceTest(@RequestParam String address) {
        try {
            logger.info("Bakiye sorgulama testi çağrıldı: {}", address);
            BigInteger balance = tenderlyBalanceService.getBalance(address);
            
            if (balance != null) {
                // Wei'den ETH'ye çevir (1 ETH = 10^18 wei)
                double ethBalance = balance.doubleValue() / Math.pow(10, 18);
                logger.info("Cüzdan bakiyesi: {} wei = {} ETH", balance, ethBalance);
                return "Cüzdan bakiyesi: " + ethBalance + " ETH";
            } else {
                return "Bakiye sorgulanamadı";
            }
        } catch (Exception e) {
            logger.error("Test hatası: ", e);
            return "HATA: " + e.getMessage() + " (" + e.getClass().getName() + ")";
        }
    }
    
    /**
     * Bakiye ekleme testi
     */
    @GetMapping("/add-balance")
    public String addBalanceTest(@RequestParam String address, @RequestParam int amount) {
        try {
            logger.info("Bakiye ekleme testi çağrıldı: {} ETH -> {}", amount, address);
            
            // Önce mevcut bakiyeyi al
            BigInteger oldBalance = tenderlyBalanceService.getBalance(address);
            double oldEthBalance = oldBalance != null ? oldBalance.doubleValue() / Math.pow(10, 18) : 0;
            
            // Bakiye ekle
            boolean result = tenderlyBalanceService.addBalanceEth(address, amount);
            
            if (result) {
                // Yeni bakiyeyi al
                BigInteger newBalance = tenderlyBalanceService.getBalance(address);
                double newEthBalance = newBalance != null ? newBalance.doubleValue() / Math.pow(10, 18) : 0;
                
                logger.info("Bakiye ekleme başarılı. Eski: {} ETH, Eklenen: {} ETH, Yeni: {} ETH", 
                        oldEthBalance, amount, newEthBalance);
                
                return String.format("Bakiye ekleme başarılı!\nEski bakiye: %.4f ETH\nEklenen: %d ETH\nYeni bakiye: %.4f ETH", 
                        oldEthBalance, amount, newEthBalance);
            } else {
                return "Bakiye ekleme başarısız";
            }
        } catch (Exception e) {
            logger.error("Test hatası: ", e);
            return "HATA: " + e.getMessage() + " (" + e.getClass().getName() + ")";
        }
    }
    
    /**
     * Doğrudan RPC testi - Servisten bağımsız
     */
    @GetMapping("/direct-rpc-test")
    public String directRpcTest(@RequestParam String address) {
        try {
            logger.info("Doğrudan RPC testi çağrıldı: {}", address);
            
            // Cüzdan adresini kontrol et
            if (!address.startsWith("0x")) {
                address = "0x" + address;
            }
            
            // 1 ETH değeri (wei cinsinden hex)
            String ethAmountHex = "0x" + BigInteger.TEN.pow(18).toString(16);
            
            // JSON-RPC isteği oluştur
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("jsonrpc", "2.0");
            requestMap.put("method", "tenderly_setBalance");
            requestMap.put("id", 1);
            requestMap.put("params", new String[]{address, ethAmountHex});
            
            String requestBody = objectMapper.writeValueAsString(requestMap);
            logger.info("İstek gönderiliyor: URL={}, Body={}", tenderlyRpcUrl, requestBody);
            
            // HTTP isteği oluştur
            Request request = new Request.Builder()
                    .url(tenderlyRpcUrl)
                    .post(RequestBody.create(requestBody, JSON))
                    .header("Content-Type", "application/json")
                    .build();
            
            // İsteği gönder
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "Boş yanıt";
                logger.info("RPC yanıtı: Kod={}, Mesaj={}, Body={}", 
                        response.code(), response.message(), responseBody);
                
                return "RPC test sonucu: " + response.code() + " - " + responseBody;
            }
            
        } catch (Exception e) {
            logger.error("Doğrudan RPC testi hatası: ", e);
            return "RPC TEST HATASI: " + e.getMessage() + " (" + e.getClass().getName() + ")";
        }
    }
} 