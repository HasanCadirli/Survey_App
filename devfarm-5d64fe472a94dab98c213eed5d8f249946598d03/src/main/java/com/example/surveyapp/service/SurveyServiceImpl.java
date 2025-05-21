package com.example.surveyapp.service;

import com.example.surveyapp.model.Option;
import com.example.surveyapp.model.Question;
import com.example.surveyapp.model.Survey;
import com.example.surveyapp.model.User;
import com.example.surveyapp.model.Vote;
import com.example.surveyapp.repository.OptionRepository;
import com.example.surveyapp.repository.QuestionRepository;
import com.example.surveyapp.repository.SurveyRepository;
import com.example.surveyapp.repository.VoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SurveyServiceImpl implements SurveyService {

    private static final Logger logger = LoggerFactory.getLogger(SurveyServiceImpl.class);

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private UserService userService;

    @Override
    public Survey createSurvey(Survey survey, User user) {
        logger.info("Creating survey: {} by user: {}", survey.getTitle(), user.getEmail());
        Survey savedSurvey = surveyRepository.save(survey);
        logger.info("Survey created successfully with ID: {}", savedSurvey.getId());
        return savedSurvey;
    }

    @Override
    public List<Survey> getAllActiveSurveys() {
        logger.info("Fetching all active surveys");
        List<Survey> surveys = surveyRepository.findByActiveTrue();
        logger.info("Found {} active surveys", surveys.size());
        if (surveys.isEmpty()) {
            logger.warn("No active surveys found in the database");
        } else {
            surveys.forEach(survey -> logger.debug("Survey: ID={}, Title={}, Active={}", survey.getId(), survey.getTitle(), survey.getActive()));
        }
        return surveys;
    }

    @Override
    @Transactional(readOnly = true)
    public Survey getSurveyById(Long id) {
        logger.info("Fetching survey with ID: {}", id);
        try {
            if (id == null) {
                logger.error("Null ID provided to getSurveyById");
                throw new IllegalArgumentException("Survey ID cannot be null");
            }
            
            Survey survey = surveyRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.error("Survey not found with ID: {}", id);
                        return new RuntimeException("Survey not found with ID: " + id);
                    });
                    
            // Lazy loading'i zorla
            if (survey.getCreatedBy() != null) {
                survey.getCreatedBy().getEmail(); // Initialize User proxy
            }
            
            if (survey.getQuestions() != null) {
                for (Question question : survey.getQuestions()) {
                    if (question != null && question.getOptions() != null) {
                        question.getOptions().size(); // Initialize options
                    }
                }
            }
            
            return survey;
        } catch (Exception e) {
            logger.error("Error retrieving survey with ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error retrieving survey: " + e.getMessage(), e);
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Override
    public void vote(Long questionId, Long optionId, User user) {
        logger.info("Voting for question ID: {} with option ID: {} by user: {}", questionId, optionId, user != null ? user.getEmail() : "null");

        try {
            // Parametre kontrolü
            if (questionId == null) {
                logger.error("Question ID is null");
                throw new IllegalArgumentException("Question ID cannot be null");
            }
            
            if (optionId == null) {
                logger.error("Option ID is null");
                throw new IllegalArgumentException("Option ID cannot be null");
            }
            
            if (user == null) {
                logger.error("User is null");
                throw new IllegalArgumentException("User cannot be null");
            }

            // Soru ve oylamayı kontrol et
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> {
                        logger.error("Question not found with ID: {}", questionId);
                        return new RuntimeException("Soru bulunamadı (ID: " + questionId + ")");
                    });
                    
            // Sorunun anketi aktif mi kontrol et
            if (question.getSurvey() == null) {
                logger.error("Question {} has no associated survey", questionId);
                throw new RuntimeException("Bu soru herhangi bir ankete ait değil");
            }
            
            if (!question.getSurvey().getActive()) {
                logger.warn("Survey is not active for question ID: {}", questionId);
                throw new RuntimeException("Bu anket artık aktif değil");
            }
            
            // Kullanıcı anketin sahibi mi kontrol et
            if (question.getSurvey().getCreatedBy() != null && 
                question.getSurvey().getCreatedBy().getId().equals(user.getId())) {
                logger.warn("User {} is trying to vote on their own survey", user.getEmail());
                throw new RuntimeException("Kendi anketinize oy veremezsiniz");
            }
            
            // Kullanıcı daha önce oy vermiş mi kontrol et
            if (voteRepository.existsByUserAndQuestion(user, question)) {
                logger.warn("User {} has already voted for question ID: {}", user.getEmail(), questionId);
                throw new RuntimeException("Bu soruya zaten oy verdiniz");
            }

            // Seçenek var mı kontrol et
            Option option = optionRepository.findById(optionId)
                    .orElseThrow(() -> {
                        logger.error("Option not found with ID: {}", optionId);
                        return new RuntimeException("Seçenek bulunamadı (ID: " + optionId + ")");
                    });
                    
            // Seçeneğin soruya ait olup olmadığını kontrol et
            if (option.getQuestion() == null || !option.getQuestion().getId().equals(questionId)) {
                logger.error("Option ID: {} does not belong to Question ID: {}", optionId, questionId);
                throw new RuntimeException("Geçersiz seçenek: Bu seçenek bu soruya ait değil");
            }

            // Oy sayısını güncelle
            Integer currentVoteCount = option.getVoteCount();
            if (currentVoteCount == null) {
                currentVoteCount = 0;
            }
            option.setVoteCount(currentVoteCount + 1);
            optionRepository.save(option);
            logger.info("Vote recorded for option ID: {}", optionId);

            // Oyu kaydet
            Vote vote = new Vote();
            vote.setUser(user);
            vote.setQuestion(question);
            vote.setOption(option);
            voteRepository.save(vote);
            logger.info("Vote saved to Vote table for user: {}, question: {}, option: {}", user.getEmail(), questionId, optionId);
            
            // Kullanıcıya 5 puan ekle
            userService.updateUserPoints(user, 5);
            logger.info("Added 5 points to user: {} for voting on question ID: {}", user.getEmail(), questionId);
        } catch (RuntimeException e) {
            logger.error("Error in vote method: {}", e.getMessage(), e);
            throw e; // Orijinal hatayı yeniden fırlat
        } catch (Exception e) {
            logger.error("Unexpected error in vote method: {}", e.getMessage(), e);
            throw new RuntimeException("Oy verme işlemi sırasında beklenmeyen bir hata oluştu: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vote> getVotesBySurvey(Long surveyId) {
        logger.info("Fetching votes for survey ID: {}", surveyId);
        try {
            Survey survey = surveyRepository.findById(surveyId)
                    .orElseThrow(() -> new RuntimeException("Survey not found with ID: " + surveyId));
                    
            List<Vote> votes = voteRepository.findByQuestionSurveyId(surveyId);
            
            // Lazy loading nesnelerini başlat
            for (Vote vote : votes) {
                if (vote.getUser() != null) {
                    vote.getUser().getEmail(); // User nesnesini başlat
                }
                if (vote.getOption() != null) {
                    vote.getOption().getText(); // Option nesnesini başlat
                }
            }
            
            logger.info("Found {} votes for survey ID: {}", votes.size(), surveyId);
            return votes;
        } catch (Exception e) {
            logger.error("Error retrieving votes for survey ID {}: {}", surveyId, e.getMessage(), e);
            throw new RuntimeException("Error retrieving votes: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Survey> getSurveysByUser(User user) {
        logger.info("Fetching surveys for user: {}", user.getEmail());
        List<Survey> surveys = surveyRepository.findByCreatedBy(user);
        logger.info("Found {} surveys for user: {}", surveys.size(), user.getEmail());
        if (surveys.isEmpty()) {
            logger.warn("No surveys found for user: {}", user.getEmail());
        } else {
            surveys.forEach(survey -> logger.debug("Survey: ID={}, Title={}, Active={}", survey.getId(), survey.getTitle(), survey.getActive()));
        }
        return surveys;
    }

    @Override
    @Transactional
    public void endSurvey(Long surveyId, User user) {
        logger.info("Ending survey ID: {} by user: {}", surveyId, user.getEmail());
        try {
            Survey survey = surveyRepository.findById(surveyId)
                    .orElseThrow(() -> new RuntimeException("Survey not found with ID: " + surveyId));

            if (!survey.getCreatedBy().getEmail().equals(user.getEmail()) && !"admin".equals(user.getRole())) {
                logger.warn("User {} is not authorized to end survey {}", user.getEmail(), surveyId);
                throw new RuntimeException("Bu anketi sonlandırma yetkiniz yok.");
            }

            survey.setActive(false);
            surveyRepository.save(survey);
            logger.info("Survey ID: {} ended successfully by user: {}", surveyId, user.getEmail());
        } catch (Exception e) {
            logger.error("Error ending survey ID {}: {}", surveyId, e.getMessage(), e);
            throw new RuntimeException("Error ending survey: " + e.getMessage(), e);
        }
    }
}