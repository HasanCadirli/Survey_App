package com.example.surveyapp.service;

import com.example.surveyapp.model.Survey;
import com.example.surveyapp.model.User;
import com.example.surveyapp.model.Vote;

import java.util.List;

public interface SurveyService {
    Survey createSurvey(Survey survey, User user);
    List<Survey> getAllActiveSurveys();
    Survey getSurveyById(Long id);
    void vote(Long questionId, Long optionId, User user);
    List<Vote> getVotesBySurvey(Long surveyId);
    List<Survey> getSurveysByUser(User user);
    void endSurvey(Long surveyId, User user);
}