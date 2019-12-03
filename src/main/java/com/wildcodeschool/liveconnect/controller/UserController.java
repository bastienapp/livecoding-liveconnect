package com.wildcodeschool.liveconnect.controller;

import com.google.common.hash.Hashing;
import com.wildcodeschool.liveconnect.entity.User;
import com.wildcodeschool.liveconnect.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Controller
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/")
    public String index() {
        // TODO : check token and log user if possible
        return "redirect:/user/sign-in";
    }

    @GetMapping("/user/sign-in")
    public String getSignIn(Model out) {
        out.addAttribute("user", new User());

        return "sign-in";
    }

    @PostMapping("/user/sign-in")
    public String postSignIn(@ModelAttribute User user, HttpSession session) {

        // TODO : save salted keyword in config file
        String encrypedPassword = Hashing.sha256()
                .hashString("!t4c0$" + user.getPassword(), StandardCharsets.UTF_8)
                .toString();
        user.setPassword(encrypedPassword);
        Optional<User> optionalUser = userRepository.findByEmailAndPassword(user.getEmail(), user.getPassword());
        if (optionalUser.isPresent()) {
            user = optionalUser.get();

            session.setAttribute("sessionUser", user);
            // TODO : store the token

            return "redirect:/user/profile";
        }

        return "sign-in";
    }

    @GetMapping("/user/sign-up")
    public String getSignUp(Model out) {
        out.addAttribute("user", new User());

        return "sign-up";
    }

    @PostMapping("/user/sign-up")
    public String postSignUp(@ModelAttribute User user, HttpSession session) {

        // TODO : test if mail already exists instead of try/catch
        try {
            // TODO : save salted keyword in config file
            String encrypedPassword = Hashing.sha256()
                    .hashString("!t4c0$" + user.getPassword(), StandardCharsets.UTF_8)
                    .toString();
            user.setPassword(encrypedPassword);
            user = userRepository.save(user);
            // TODO : store the token
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
            return "redirect:/user/sign-in";
        }
        out.addAttribute("user", user);

        return "profile";
    }

    @GetMapping("/user/sign-out")
    public String getSignOut(HttpSession session) {
        session.removeAttribute("sessionUser");

        return "redirect:/user/sign-in";
    }
}
