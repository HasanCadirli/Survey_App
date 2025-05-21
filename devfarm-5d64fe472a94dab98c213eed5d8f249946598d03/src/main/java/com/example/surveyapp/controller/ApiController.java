package com.example.surveyapp.controller;

import com.example.surveyapp.model.User;
import com.example.surveyapp.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);
    private static final String RPC_URL = "https://virtual.sepolia.rpc.tenderly.co/d88ec9b6-956d-4d96-b852-76fcd37861b3";

    @Autowired
    private UserService userService;

    @PostMapping("/register-with-wallet")
    public ResponseEntity<?> registerUserWithWallet(@RequestBody Map<String, String> request) {
        logger.info("POST /api/register-with-wallet - Registering user with wallet address: {}", request.get("walletAddress"));
        Map<String, Object> response = new HashMap<>();
        
        try {
            String walletAddress = request.get("walletAddress");
            String signature = request.get("signature");
            String fullName = request.get("fullName");
            String email = request.get("email");
            
            if (walletAddress == null || walletAddress.isEmpty()) {
                throw new Exception("Cüzdan adresi gereklidir!");
            }
            
            if (signature == null || signature.isEmpty()) {
                throw new Exception("İmza gereklidir!");
            }
            
            User user = new User();
            user.setWalletAddress(walletAddress);
            user.setFullName(fullName != null ? fullName : "Web3 Kullanıcı");
            if (email != null && !email.isEmpty()) {
                user.setEmail(email);
            } else {
                // Web3 kullanıcılarına geçici email oluştur
                user.setEmail(walletAddress.substring(0, 10) + "@web3user.eth");
            }
            user.setPassword("web3_" + System.currentTimeMillis()); // Rastgele bir parola
            
            userService.registerUserWithWallet(user, signature);
            
            response.put("success", true);
            response.put("message", "Cüzdan ile kayıt başarılı!");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error registering user with wallet: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Cüzdan ile kayıt sırasında bir hata oluştu: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/login-with-wallet")
    public ResponseEntity<?> loginUserWithWallet(@RequestBody Map<String, String> request, HttpSession session) {
        logger.info("POST /api/login-with-wallet - Attempting login with wallet address: {}", request.get("walletAddress"));
        Map<String, Object> response = new HashMap<>();
        
        try {
            String walletAddress = request.get("walletAddress");
            String signature = request.get("signature");
            
            if (walletAddress == null || walletAddress.isEmpty()) {
                throw new Exception("Cüzdan adresi gereklidir!");
            }
            
            if (signature == null || signature.isEmpty()) {
                throw new Exception("İmza gereklidir!");
            }
            
            User user = userService.loginUserWithWallet(walletAddress, signature);
            
            // Session'a kullanıcı bilgilerini ekle
            session.setAttribute("loggedInUser", user.getEmail());
            session.setAttribute("walletAddress", user.getWalletAddress());
            
            response.put("success", true);
            response.put("message", "Cüzdan ile giriş başarılı!");
            response.put("redirectUrl", "/surveys");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error logging in user with wallet: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Cüzdan ile giriş sırasında bir hata oluştu: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/auth-message")
    public ResponseEntity<?> getAuthMessage() {
        Map<String, Object> response = new HashMap<>();
        String message = "Anket Sistemine giriş yapıyorum: " + System.currentTimeMillis();
        response.put("message", message);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/send-eth")
    public ResponseEntity<?> sendEthToWallet(@RequestBody Map<String, Object> request, HttpSession session) {
        logger.info("POST /api/send-eth - Sending ETH to wallet");
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Kullanıcı kontrolü
            String loggedInUserEmail = (String) session.getAttribute("loggedInUser");
            if (loggedInUserEmail == null) {
                logger.warn("No logged-in user found");
                response.put("success", false);
                response.put("message", "Lütfen önce giriş yapın");
                return ResponseEntity.badRequest().body(response);
            }
            
            // İstek parametrelerini al
            String walletAddress = (String) request.get("walletAddress");
            Integer amount = (Integer) request.get("amount");
            
            if (walletAddress == null || walletAddress.isEmpty()) {
                logger.warn("Wallet address is empty");
                response.put("success", false);
                response.put("message", "Cüzdan adresi gereklidir");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (amount == null || amount <= 0 || amount > 100) {
                logger.warn("Invalid amount: {}", amount);
                response.put("success", false);
                response.put("message", "Geçerli bir miktar girmelisiniz (1-100)");
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = userService.findUserByEmail(loggedInUserEmail);
            
            // Cüzdan adresi uyumu kontrolü
            if (user.getWalletAddress() == null || !user.getWalletAddress().equalsIgnoreCase(walletAddress)) {
                logger.warn("Wallet address mismatch. User: {}, Request: {}", 
                           user.getWalletAddress(), walletAddress);
                response.put("success", false);
                response.put("message", "Bu cüzdan adresi sizin hesabınızla ilişkili değil");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Tenderly API çağrısı yapılacak - Burada gerçek çağrı yapılıyor gibi simüle ediyoruz
            // Gerçek uygulamada Web3j veya başka bir HTTP client kullanılabilir
            
            logger.info("Simulating Tenderly API call to send {} ETH to {}", amount, walletAddress);
            
            // Burada gerçek bir HTTP çağrısı yapılabilir
            // Bu örnekte sadece başarılı olduğunu varsayıyoruz
            
            response.put("success", true);
            response.put("message", amount + " ETH başarıyla " + walletAddress + " adresine gönderildi");
            response.put("ethAmount", amount);
            response.put("walletAddress", walletAddress);
            response.put("txHash", "0x" + Long.toHexString(System.currentTimeMillis())); // Sahte bir tx hash
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error sending ETH: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "ETH gönderimi sırasında bir hata oluştu: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 