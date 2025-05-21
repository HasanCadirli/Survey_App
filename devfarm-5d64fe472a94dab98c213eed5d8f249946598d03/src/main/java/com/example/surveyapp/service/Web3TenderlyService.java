package com.example.surveyapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@Service
public class Web3TenderlyService {

    private static final Logger logger = LoggerFactory.getLogger(Web3TenderlyService.class);
    private static final String TENDERLY_BASE_URL = "https://virtual.sepolia.rpc.tenderly.co/d88ec9b6-956d-486e-b852-76fcd37861b3";

    // Test cuzdani private key (demo amacli - gercek uygulamada guvenli bir sekilde saklanmalidir)
    private static final String SENDER_PRIVATE_KEY = "0x4f3edf983ac636a65a842ce7c78d9aa706d3b113bce9c46f30d7d21715b23b1d";
    private static final String SENDER_ADDRESS = "0x90F8bf6A479f320ead074411a4B0e7944Ea8c9C1";

    private final RestTemplate restTemplate;

    public Web3TenderlyService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Tenderly API kullanarak belirtilen adrese ETH gonderir
     *
     * @param walletAddress Alici cuzdan adresi
     * @param ethAmount Gonderilecek ETH miktari
     * @return Islem hash'i
     */
    public String sendEthToWallet(String walletAddress, int ethAmount) {
        logger.info("Sending {} ETH to wallet: {}", ethAmount, walletAddress);

        try {
            // JSON-RPC istegi olustur
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("jsonrpc", "2.0");
            requestBody.put("method", "eth_sendTransaction");
            requestBody.put("id", 1);

            // Transaction parametreleri
            Map<String, String> params = new HashMap<>();
            params.put("from", SENDER_ADDRESS);
            params.put("to", walletAddress);

            // ETH miktarini wei'ye cevir (1 ETH = 10^18 wei)
            String valueInWei = "0x" + BigInteger.TEN.pow(18).multiply(BigInteger.valueOf(ethAmount)).toString(16);
            params.put("value", valueInWei);
            params.put("gas", "0x76c0"); // 30400 gas
            params.put("gasPrice", "0x9184e72a000"); // 10000000000000 wei

            requestBody.put("params", new Object[]{params});

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Tenderly API'a istek gonder
            ResponseEntity<Map> response = restTemplate.postForEntity(TENDERLY_BASE_URL, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String txHash = (String) response.getBody().get("result");
                logger.info("Transaction successful. Hash: {}", txHash);
                return txHash;
            } else {
                logger.error("Failed to send transaction: {}", response.getBody());
                throw new RuntimeException("ETH gonderimi basarisiz oldu: " + response.getBody());
            }

        } catch (Exception e) {
            logger.error("Error sending ETH: {}", e.getMessage(), e);
            throw new RuntimeException("ETH gonderimi sirasinda bir hata olustu: " + e.getMessage());
        }
    }
}