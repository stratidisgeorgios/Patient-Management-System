package com.example.authservice.service;
import org.springframework.stereotype.Service;
import java.util.Optional;
import com.example.authservice.model.User;
import com.example.authservice.repository.UserRepository;
@Service
public class UserService {
    private final UserRepository userRepository;
    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
