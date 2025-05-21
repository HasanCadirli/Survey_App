package com.example.surveyapp.controller;

import com.example.surveyapp.model.Survey;
import com.example.surveyapp.model.User;
import com.example.surveyapp.service.SurveyService;
import com.example.surveyapp.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private SurveyService surveyService;

    @GetMapping
    public String showAdminPanel(Model model, HttpSession session,
                              @RequestParam(value = "success", required = false) String success,
                              @RequestParam(value = "error", required = false) String error,
                              @RequestParam(value = "viewType", required = false, defaultValue = "thymeleaf") String viewType) {
        logger.info("GET /admin - Showing admin panel, viewType: {}", viewType);
        String loggedInUserEmail = (String) session.getAttribute("loggedInUser");
        logger.debug("loggedInUserEmail: {}", loggedInUserEmail);
        if (loggedInUserEmail == null) {
            logger.warn("No logged-in user found, redirecting to login");
            return "redirect:/login?error=" + URLEncoder.encode("Lütfen önce giriş yapın.", StandardCharsets.UTF_8);
        }

        User user = userService.findUserByEmail(loggedInUserEmail);
        logger.debug("User found: {}, role: {}", user.getEmail(), user.getRole());
        if (!"admin".equals(user.getRole())) {
            logger.warn("User {} does not have admin privileges, redirecting to surveys", loggedInUserEmail);
            return "redirect:/surveys?error=" + URLEncoder.encode("Bu sayfaya erişim yetkiniz yok.", StandardCharsets.UTF_8);
        }

        // Tüm kullanıcıları getir (admin hariç)
        List<User> users = userService.findAllUsers().stream()
                .filter(u -> !"admin".equals(u.getRole()))
                .collect(Collectors.toList());
        logger.debug("Found {} non-admin users", users.size());

        // Tüm anketleri getir
        List<Survey> surveys = surveyService.getAllActiveSurveys();
        logger.debug("Found {} active surveys", surveys.size());

        model.addAttribute("users", users);
        model.addAttribute("surveys", surveys);
        model.addAttribute("loggedInUser", loggedInUserEmail);
        
        if (success != null) {
            model.addAttribute("success", success);
        }
        if (error != null) {
            model.addAttribute("error", error);
        }
        
        // Debug: Model içeriğini konsola yazdır
        System.out.println("=== MODEL CONTENT ===");
        for (String key : model.asMap().keySet()) {
            System.out.println(key + ": " + model.asMap().get(key));
        }
        System.out.println("=====================");
        
        // JSP ve Thymeleaf arasında geçiş yapmak için parametre kontrol et
        if ("jsp".equals(viewType)) {
            logger.info("Using JSP view for admin panel");
            return "forward:/admin.jsp";
        } else {
            logger.info("Using Thymeleaf view for admin panel");
            
            // Thymeleaf ile ilgili bilgileri konsola yazdır
            System.out.println("Trying to render Thymeleaf template: admin-panel.html");
            System.out.println("Template location: classpath:/templates/admin-panel.html");
            
            return "admin-panel";
        }
    }

    @PostMapping("/deactivate-survey")
    public String deactivateSurvey(@RequestParam("surveyId") Long surveyId, HttpSession session) {
        logger.info("POST /admin/deactivate-survey - Deactivating survey with ID: {}", surveyId);
        String loggedInUserEmail = (String) session.getAttribute("loggedInUser");
        if (loggedInUserEmail == null) {
            logger.warn("No logged-in user found, redirecting to login");
            return "redirect:/login?error=" + URLEncoder.encode("Lütfen önce giriş yapın.", StandardCharsets.UTF_8);
        }

        User user = userService.findUserByEmail(loggedInUserEmail);
        if (!"admin".equals(user.getRole())) {
            logger.warn("User {} does not have admin privileges", loggedInUserEmail);
            return "redirect:/surveys?error=" + URLEncoder.encode("Bu işlemi gerçekleştirme yetkiniz yok.", StandardCharsets.UTF_8);
        }

        try {
            surveyService.endSurvey(surveyId, user);
            return "redirect:/admin?success=" + URLEncoder.encode("Anket başarıyla pasif yapıldı!", StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            logger.error("Error deactivating survey: {}", e.getMessage(), e);
            return "redirect:/admin?error=" + URLEncoder.encode("Anket pasif yapılırken bir hata oluştu: " + e.getMessage(), StandardCharsets.UTF_8);
        }
    }

    @PostMapping("/delete-user")
    public String deleteUser(@RequestParam("userId") Long userId, HttpSession session) {
        logger.info("POST /admin/delete-user - Deleting user with ID: {}", userId);
        String loggedInUserEmail = (String) session.getAttribute("loggedInUser");
        if (loggedInUserEmail == null) {
            logger.warn("No logged-in user found, redirecting to login");
            return "redirect:/login?error=" + URLEncoder.encode("Lütfen önce giriş yapın.", StandardCharsets.UTF_8);
        }

        User admin = userService.findUserByEmail(loggedInUserEmail);
        if (!"admin".equals(admin.getRole())) {
            logger.warn("User {} does not have admin privileges", loggedInUserEmail);
            return "redirect:/surveys?error=" + URLEncoder.encode("Bu işlemi gerçekleştirme yetkiniz yok.", StandardCharsets.UTF_8);
        }

        try {
            userService.deleteUser(userId);
            return "redirect:/admin?success=" + URLEncoder.encode("Kullanıcı başarıyla silindi!", StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            logger.error("Error deleting user: {}", e.getMessage(), e);
            return "redirect:/admin?error=" + URLEncoder.encode("Kullanıcı silinirken bir hata oluştu: " + e.getMessage(), StandardCharsets.UTF_8);
        }
    }
}