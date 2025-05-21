package com.example.surveyapp.model;

import jakarta.persistence.*;
import java.util.Objects;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;


@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullName;

    @Column(unique = true)
    private String email;

    private String password;
    
    @Column(unique = true)
    private String walletAddress;
    
    private int points;
    private String role = "user";

    // Getters ve Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getWalletAddress() { return walletAddress; }
    public void setWalletAddress(String walletAddress) { this.walletAddress = walletAddress; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    // toString, equals, hashCode
    @Override
    public String toString() {
        return "User{id=" + id + ", email='" + email + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}