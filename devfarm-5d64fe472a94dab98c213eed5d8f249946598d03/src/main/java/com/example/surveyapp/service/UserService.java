package com.example.surveyapp.service;

import com.example.surveyapp.model.User;

import java.util.List;

public interface UserService {
    void registerUser(User user) throws Exception;
    User loginUser(String email, String password) throws Exception;
    User findUserByEmail(String email);
    void updateUserPoints(User user, int pointsToAdd);
    List<User> findAllUsers();
    void deleteUser(Long userId);
    
    // Web3/Metamask için yeni metodlar
    void registerUserWithWallet(User user, String signature) throws Exception;
    User loginUserWithWallet(String walletAddress, String signature) throws Exception;
    User findUserByWalletAddress(String walletAddress);
    boolean verifyWalletSignature(String walletAddress, String signature, String message);
    void updateUserWalletAddress(User user, String walletAddress);
}