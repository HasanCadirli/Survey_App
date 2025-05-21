package com.example.surveyapp.repository;

import com.example.surveyapp.model.Question;
import com.example.surveyapp.model.User;
import com.example.surveyapp.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    boolean existsByUserAndQuestion(User user, Question question);
    List<Vote> findByQuestionSurveyId(Long surveyId);
}