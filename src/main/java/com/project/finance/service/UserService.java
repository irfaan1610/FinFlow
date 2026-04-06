package com.project.finance.service;

import com.project.finance.dto.response.UserResponse;
import com.project.finance.exception.ResourceNotFoundException;
import com.project.finance.model.Role;
import com.project.finance.model.User;
import com.project.finance.model.UserStatus;
import com.project.finance.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    public UserResponse updateRole(Long id, Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setRole(role);
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse updateStatus(Long id, UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setStatus(status);
        return UserResponse.from(userRepository.save(user));
    }
}
