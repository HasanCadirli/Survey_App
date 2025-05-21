package com.example.surveyapp.dbtest;

import java.sql.Connection;
import java.sql.DriverManager;

public class TestDB {
    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://35.195.19.175:3306/devfarm_db?useSSL=false&serverTimezone=UTC",
                    "devfarm-project", "Narucu.12");
            System.out.println("Bağlantı başarılı!");
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}