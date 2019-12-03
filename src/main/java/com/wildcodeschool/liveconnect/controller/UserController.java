package com.wildcodeschool.liveconnect.controller;

import com.google.common.hash.Hashing;
import com.wildcodeschool.liveconnect.entity.User;
import com.wildcodeschool.liveconnect.repository.UserRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

@Controller
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/")
    public String index(@CookieValue(name = "sessionId", required = false) String sessionId,
                        HttpSession session) {

        if (sessionId != null && !sessionId.isEmpty()) {
            Optional<User> optionalUser = userRepository.findBySession(sessionId);
            if (optionalUser.isPresent()) {
                User user = optionalUser.get();
                if (user.getSessionExpiration().after(new Date())) {
                    session.setAttribute("sessionUser", user);
                    return "redirect:/user/profile";
                }
            }
        }

        return "redirect:/user/sign-in";
    }

    @GetMapping("/user/sign-in")
    public String getSignIn(Model out) {
        out.addAttribute("user", new User());

        return "sign-in";
    }

    @PostMapping("/user/sign-in")
    public String postSignIn(@ModelAttribute User user, HttpSession session, HttpServletResponse response) {

        // TODO : save salted keyword in config file
        String encrypedPassword = Hashing.sha256()
                .hashString("!t4c0$" + user.getPassword(), StandardCharsets.UTF_8)
                .toString();
        user.setPassword(encrypedPassword);
        Optional<User> optionalUser = userRepository.findByEmailAndPassword(user.getEmail(), user.getPassword());
        if (optionalUser.isPresent()) {
            user = optionalUser.get();
            user = refreshSession(user, response);
            userRepository.save(user);

            session.setAttribute("sessionUser", user);
            // TODO : store the token

            return "redirect:/user/profile";
        }

        return "sign-in";
    }

    private static User refreshSession(User user, HttpServletResponse response) {
        String sessionId = RandomStringUtils.randomAlphabetic(30);
        user.setSession(sessionId);

        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.MONTH, 1);
        user.setSessionExpiration(c.getTime());

        Cookie sessionCookie = new Cookie("sessionId", user.getSession());
        sessionCookie.setMaxAge(30 * 24 * 60 * 60);
        sessionCookie.setPath("/"); // cookie is accessible from everywhere
        response.addCookie(sessionCookie);

        return user;
    }

    @GetMapping("/user/sign-up")
    public String getSignUp(Model out) {
        out.addAttribute("user", new User());

        return "sign-up";
    }

    @PostMapping("/user/sign-up")
    public String postSignUp(@ModelAttribute User user, HttpSession session, HttpServletResponse response) {

        // TODO : test if mail already exists instead of try/catch
        try {
            // TODO : save salted keyword in config file
            String encrypedPassword = Hashing.sha256()
                    .hashString("!t4c0$" + user.getPassword(), StandardCharsets.UTF_8)
                    .toString();
            user.setPassword(encrypedPassword);
            user = refreshSession(user, response);
            userRepository.save(user);
        } catch (Exception e) {
            // TODO : add error message
            return "sign-up";
        }

        session.setAttribute("sessionUser", user);

        return "redirect:/user/profile";
    }

    @GetMapping("/user/profile")
    public String getProfile(HttpSession session, Model out) {
        User user = (User) session.getAttribute("sessionUser");
        if (user == null) {
            return "redirect:/";
        }
        out.addAttribute("user", user);

        return "profile";
    }

    @GetMapping("/user/sign-out")
    public String getSignOut(HttpSession session, HttpServletResponse response) {
        session.removeAttribute("sessionUser");

        Cookie sessionCookie = new Cookie("sessionId", null);
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge(0);
        response.addCookie(sessionCookie);

        return "redirect:/user/sign-in";
    }
}
