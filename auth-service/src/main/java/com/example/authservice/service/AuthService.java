package com.example.authservice.service;

import org.springframework.stereotype.Service;
import java.util.Optional;
import com.example.authservice.dto.AuthRequestDTO;
import com.example.authservice.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import io.jsonwebtoken.JwtException;
@Service
public class AuthService {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;


    public AuthService(UserService userService, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<String> authenticate(AuthRequestDTO authRequestDTO) {
        Optional<String> token = userService
        .findByEmail(authRequestDTO.getEmail())
        .filter(u -> passwordEncoder.matches(authRequestDTO.getPassword(), u.getPassword()))
        .map(u -> jwtUtil.generateToken(u.getEmail(),u.getRole()));
        return token;
    }

    public boolean validateToken(String token) {
        try {
            jwtUtil.validateToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}

