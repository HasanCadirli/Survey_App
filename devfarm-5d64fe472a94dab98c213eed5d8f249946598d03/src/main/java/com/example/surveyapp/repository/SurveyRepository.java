package com.example.surveyapp.repository;

import com.example.surveyapp.model.Survey;
import com.example.surveyapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface SurveyRepository extends JpaRepository<Survey, Long> {
    List<Survey> findByActiveTrue();
    List<Survey> findByCreatedBy(User user);
}