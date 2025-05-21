package com.example.surveyapp.service;

import com.example.surveyapp.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class RewardService {

    private static final Logger logger = LoggerFactory.getLogger(RewardService.class);
    private static final int MAX_ETH_WITHDRAWAL = 100;
    private static final String RPC_URL = "https://virtual.sepolia.rpc.tenderly.co/d88ec9b6-956d-4d96-b852-76fcd37861b3";
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private Web3TenderlyService web3TenderlyService;
    
    /**
     * Kullanıcının puanlarını ETH'ye çevirir
     * @param user Puanlarını ETH'ye çevirmek isteyen kullanıcı
     * @param walletAddress Kullanıcının Ethereum cüzdan adresi
     * @param pointsToConvert Çevrilecek puan miktarı
     * @return İşlem sonucunu içeren Map
     */
    @Transactional
    public Map<String, Object> convertPointsToEth(User user, String walletAddress, int pointsToConvert) {
        Map<String, Object> result = new HashMap<>();
        
        logger.info("Converting {} points to ETH for user: {}, wallet: {}", 
                   pointsToConvert, user.getEmail(), walletAddress);
        
        try {
            // Parametreleri doğrula
            if (pointsToConvert <= 0) {
                logger.warn("Invalid points amount: {}", pointsToConvert);
                result.put("success", false);
                result.put("message", "Geçerli bir puan miktarı giriniz");
                return result;
            }
            
            if (pointsToConvert > MAX_ETH_WITHDRAWAL) {
                logger.warn("Points exceed maximum withdrawal limit: {}", pointsToConvert);
                result.put("success", false);
                result.put("message", "Maksimum " + MAX_ETH_WITHDRAWAL + " puan çekebilirsiniz");
                return result;
            }
            
            if (pointsToConvert > user.getPoints()) {
                logger.warn("Not enough points. Requested: {}, Available: {}", pointsToConvert, user.getPoints());
                result.put("success", false);
                result.put("message", "Yeterli puanınız bulunmamaktadır");
                return result;
            }
            
            // Kullanıcının cüzdan adresi bağlı mı kontrol et
            if (user.getWalletAddress() == null || user.getWalletAddress().isEmpty()) {
                logger.warn("User has no linked wallet address: {}", user.getEmail());
                result.put("success", false);
                result.put("message", "Önce bir Ethereum cüzdanı bağlamalısınız");
                return result;
            }
            
            // Verilen cüzdan adresi kullanıcının bağlı cüzdanıyla aynı mı kontrol et
            if (!user.getWalletAddress().equalsIgnoreCase(walletAddress)) {
                logger.warn("Wallet address mismatch. User wallet: {}, Provided wallet: {}", 
                          user.getWalletAddress(), walletAddress);
                result.put("success", false);
                result.put("message", "Girilen cüzdan adresi sizin cüzdanınızla eşleşmiyor");
                return result;
            }
            
            // ETH gönderimi için Tenderly API'ye istek gönder
            String txHash = null;
            try {
                txHash = web3TenderlyService.sendEthToWallet(walletAddress, pointsToConvert);
                logger.info("Successfully sent {} ETH to wallet: {}, txHash: {}", 
                          pointsToConvert, walletAddress, txHash);
            } catch (Exception e) {
                logger.error("Failed to send ETH: {}", e.getMessage(), e);
                result.put("success", false);
                result.put("message", "ETH gönderimi sırasında bir hata oluştu: " + e.getMessage());
                return result;
            }
            
            // Kullanıcının puanlarını güncelle
            userService.updateUserPoints(user, -pointsToConvert);
            logger.info("Deducted {} points from user: {}", pointsToConvert, user.getEmail());
            
            // Başarılı sonucu dön
            result.put("success", true);
            result.put("message", pointsToConvert + " puan başarıyla " + pointsToConvert + " ETH'ye çevrildi ve cüzdanınıza gönderildi");
            result.put("txHash", txHash);
            result.put("ethAmount", pointsToConvert);
            result.put("walletAddress", walletAddress);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error converting points to ETH: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "ETH dönüşümü sırasında bir hata oluştu: " + e.getMessage());
            return result;
        }
    }
} 