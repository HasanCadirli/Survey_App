package com.example.surveyapp.service;

import com.example.surveyapp.model.User;
import com.example.surveyapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Transactional
    @Override
    public void registerUser(User user) throws Exception {
        logger.info("Registering user with email: {}", user.getEmail());
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            logger.warn("User with email {} already exists", user.getEmail());
            throw new Exception("Bu e-posta adresiyle bir kullanıcı zaten kayıtlı!");
        }
        userRepository.save(user);
        logger.info("User registered successfully: {}", user.getEmail());
    }

    @Override
    public User loginUser(String email, String password) throws Exception {
        logger.debug("Logging in user with email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("Kullanıcı bulunamadı!"));
        logger.debug("Found user: {}", user.getEmail());
        logger.debug("Stored password: {}, Provided password: {}", user.getPassword(), password);
        if (!user.getPassword().equals(password)) {
            logger.warn("Invalid password for user: {}", email);
            throw new Exception("Geçersiz şifre!");
        }
        logger.info("User authenticated successfully: {}", email);
        return user;
    }

    @Override
    public User findUserByEmail(String email) {
        logger.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + email));
    }

    @Transactional
    @Override
    public void updateUserPoints(User user, int pointsToAdd) {
        logger.info("Updating points for user: {}, adding {} points", user.getEmail(), pointsToAdd);
        user.setPoints(user.getPoints() + pointsToAdd);
        userRepository.save(user);
        logger.info("User points updated successfully: {} now has {} points", user.getEmail(), user.getPoints());
    }

    @Override
    public List<User> findAllUsers() {
        logger.info("Fetching all users");
        List<User> users = userRepository.findAll();
        logger.info("Found {} users", users.size());
        return users;
    }
    
    @Transactional
    @Override
    public void deleteUser(Long userId) {
        logger.info("Deleting user with ID: {}", userId);
        if (!userRepository.existsById(userId)) {
            logger.warn("User with ID {} not found", userId);
            throw new RuntimeException("Kullanıcı bulunamadı: ID " + userId);
        }
        userRepository.deleteById(userId);
        logger.info("User with ID {} deleted successfully", userId);
    }

    @Transactional
    @Override
    public void registerUserWithWallet(User user, String signature) throws Exception {
        logger.info("Registering user with wallet address: {}", user.getWalletAddress());
        
        // Wallet adresi boş olmamalı
        if (user.getWalletAddress() == null || user.getWalletAddress().isEmpty()) {
            logger.warn("Wallet address is empty");
            throw new Exception("Cüzdan adresi boş olamaz!");
        }
        
        // Bu wallet adresiyle kayıtlı kullanıcı var mı kontrolü
        if (userRepository.findByWalletAddress(user.getWalletAddress()).isPresent()) {
            logger.warn("User with wallet address {} already exists", user.getWalletAddress());
            throw new Exception("Bu cüzdan adresiyle bir kullanıcı zaten kayıtlı!");
        }
        
        // E-posta adresi verilmişse, bu e-posta ile kayıtlı kullanıcı var mı kontrolü
        if (user.getEmail() != null && !user.getEmail().isEmpty() && 
            userRepository.findByEmail(user.getEmail()).isPresent()) {
            logger.warn("User with email {} already exists", user.getEmail());
            throw new Exception("Bu e-posta adresiyle bir kullanıcı zaten kayıtlı!");
        }
        
        // İmza doğrulama - basit bir kontrol yapıyoruz
        // Gerçek dünyada burada imza kriptografik olarak doğrulanmalı
        if (signature == null || signature.length() < 10) {
            logger.warn("Invalid signature");
            throw new Exception("Geçersiz imza!");
        }
        
        // Kullanıcıyı kaydet
        userRepository.save(user);
        logger.info("User registered successfully with wallet: {}", user.getWalletAddress());
    }

    @Override
    public User loginUserWithWallet(String walletAddress, String signature) throws Exception {
        logger.debug("Logging in user with wallet address: {}", walletAddress);
        
        // Cüzdan adresi ile kullanıcı bul
        User user = userRepository.findByWalletAddress(walletAddress)
                .orElseThrow(() -> new Exception("Bu cüzdan adresiyle kayıtlı kullanıcı bulunamadı!"));
        
        logger.debug("Found user with wallet: {}", user.getWalletAddress());
        
        // İmza doğrulama - basit bir kontrol yapıyoruz
        // Gerçek dünyada burada imza kriptografik olarak doğrulanmalı
        if (signature == null || signature.length() < 10) {
            logger.warn("Invalid signature for wallet login: {}", walletAddress);
            throw new Exception("Geçersiz imza!");
        }
        
        logger.info("User authenticated successfully with wallet: {}", walletAddress);
        return user;
    }

    @Override
    public User findUserByWalletAddress(String walletAddress) {
        logger.debug("Finding user by wallet address: {}", walletAddress);
        return userRepository.findByWalletAddress(walletAddress)
                .orElseThrow(() -> new RuntimeException("Bu cüzdan adresiyle kayıtlı kullanıcı bulunamadı!"));
    }

    @Override
    public boolean verifyWalletSignature(String walletAddress, String signature, String message) {
        try {
            // Basit bir doğrulama - gerçek bir uygulamada burada kriptografik doğrulama yapılmalı
            // Şu an sadece girdilerin boş olup olmadığını kontrol ediyoruz
            if (walletAddress == null || walletAddress.isEmpty() || 
                signature == null || signature.isEmpty() ||
                message == null || message.isEmpty()) {
                logger.warn("Invalid inputs for signature verification");
                return false;
            }
            
            // Şu an her zaman başarılı kabul ediyoruz
            logger.info("Signature verified for wallet: {}", walletAddress);
            return true;
        } catch (Exception e) {
            logger.error("Error during signature verification: {}", e.getMessage(), e);
            return false;
        }
    }

    private Optional<User> findByWalletAddressOptional(String walletAddress) {
        return userRepository.findByWalletAddress(walletAddress);
    }
} 