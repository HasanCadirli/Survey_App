package com.example.surveyapp.service;

import com.example.surveyapp.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class RewardService {

    private static final Logger logger = LoggerFactory.getLogger(RewardService.class);
    private static final int MAX_ETH_WITHDRAWAL = 100;
    
    // URL'yi sabit tanımlama
    private final String tenderlyRpcUrl = "https://virtual.sepolia.rpc.tenderly.co/d88ec9b6-956d-486e-b852-76fcd37861b3";

    @Autowired
    private UserService userService;

    @Autowired
    private Web3TenderlyService web3TenderlyService;
    
    @Autowired
    private TenderlyBalanceService tenderlyBalanceService;

    /**
     * Kullanıcının puanlarını ETH'ye çevirir
     *
     * @param user            Puanlarını ETH'ye çevirmek isteyen kullanıcı
     * @param walletAddress   Kullanıcının Ethereum cüzdan adresi
     * @param pointsToConvert Çevrilecek puan miktarı
     * @return İşlem sonucunu içeren Map
     */
    @Transactional
    public Map<String, Object> convertPointsToEth(User user, String walletAddress, int pointsToConvert) {
        Map<String, Object> result = new HashMap<>();

        logger.info("Converting {} points to ETH for user: {}, wallet: {}",
                pointsToConvert, user.getEmail(), walletAddress);

        try {
            // Parametre doğrulamaları
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

            if (user.getWalletAddress() == null || user.getWalletAddress().isEmpty()) {
                logger.warn("User has no linked wallet address: {}", user.getEmail());
                result.put("success", false);
                result.put("message", "Önce bir Ethereum cüzdanı bağlamalısınız");
                return result;
            }

            if (!user.getWalletAddress().equalsIgnoreCase(walletAddress)) {
                logger.warn("Wallet address mismatch. User wallet: {}, Provided wallet: {}",
                        user.getWalletAddress(), walletAddress);
                result.put("success", false);
                result.put("message", "Girilen cüzdan adresi sizin cüzdanınızla eşleşmiyor");
                return result;
            }

            // ETH gönderimi - Test ortamında Tenderly Balance Service kullanarak
            try {
                // Cüzdan adresini düzenle - 0x kontrolü
                if (!walletAddress.startsWith("0x")) {
                    walletAddress = "0x" + walletAddress;
                    logger.info("Wallet address formatted with 0x prefix: {}", walletAddress);
                }
                
                // Önce cüzdana ETH yükle (test ortamı)
                logger.info("Setting ETH balance using Tenderly for wallet: {}, amount: {}, URL: {}", 
                        walletAddress, pointsToConvert, tenderlyRpcUrl);
                
                // Doğrudan boolean değer döndürmek yerine, try-catch ile işlem yapma
                try {
                    boolean balanceSet = tenderlyBalanceService.addBalanceEth(walletAddress, pointsToConvert);
                    
                    if (!balanceSet) {
                        logger.error("Failed to set ETH balance for wallet: {}", walletAddress);
                        result.put("success", false);
                        result.put("message", "Test ETH yüklemesi başarısız oldu. Lütfen daha sonra tekrar deneyin.");
                        return result;
                    }
                    
                    logger.info("Successfully added ETH to wallet: {}", walletAddress);
                } catch (Exception e) {
                    logger.error("Tenderly setBalance exception: ", e);
                    result.put("success", false);
                    result.put("message", "Tenderly işlemi sırasında hata: " + e.getMessage());
                    return result;
                }
                
                // Gerçek bir transaction oluşturmayı simüle et (test ortamında gerçek tx gerekmez)
                String txHash = "0x" + java.util.UUID.randomUUID().toString().replace("-", "");
                logger.info("Test transaction simulated with hash: {}", txHash);
                
                // Kullanıcı puanlarını güncelle
                userService.updateUserPoints(user, -pointsToConvert);
                logger.info("Deducted {} points from user: {}", pointsToConvert, user.getEmail());

                // Başarılı sonuç
                result.put("success", true);
                result.put("message", pointsToConvert + " puan başarıyla " + pointsToConvert + " ETH'ye çevrildi ve cüzdanınıza gönderildi");
                result.put("txHash", txHash);
                result.put("ethAmount", pointsToConvert);
                result.put("walletAddress", walletAddress);

                return result;
                
            } catch (Exception e) {
                logger.error("Failed to send ETH: {} (Exception type: {})", e.getMessage(), e.getClass().getName(), e);
                result.put("success", false);

                if (e.getMessage() != null && (e.getMessage().contains("Connection timed out") || e.getMessage().contains("I/O error"))) {
                    result.put("message", "Ethereum ağına bağlanırken bir sorun yaşandı. Lütfen internet bağlantınızı kontrol edin ve daha sonra tekrar deneyin. Sorun devam ederse yöneticiye başvurun.");
                } else if (e instanceof IllegalArgumentException) {
                    result.put("message", "Giriş parametrelerinde hata: " + e.getMessage());
                } else {
                    result.put("message", "ETH gönderimi sırasında bir hata oluştu. Lütfen daha sonra tekrar deneyin.");
                }
                return result;
            }

        } catch (Exception e) {
            logger.error("Error converting points to ETH: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "ETH dönüşümü sırasında bir hata oluştu: " + e.getMessage());
            return result;
        }
    }
}
