package com.janne.mailservice.services.core;

import com.janne.mailservice.entity.Role;
import com.janne.mailservice.entity.UserEntity;
import com.janne.mailservice.model.action.ChangePasswordDto;
import com.janne.mailservice.model.action.UpdateUserDto;
import com.janne.mailservice.model.action.UserCreationDto;
import com.janne.mailservice.model.core.UserSummaryDto;
import com.janne.mailservice.repository.ConnectionAccessRepository;
import com.janne.mailservice.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ConnectionAccessRepository connectionAccessRepository;
    private final PasswordEncoder passwordEncoder;

    public UserEntity getUserByUuid(String uuid) {
        return userRepository
                .findById(uuid)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public UserEntity getUserByEmail(String email) {
        return userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public UserEntity createUser(UserCreationDto dto) {
        if (userRepository.existsByEmailIgnoreCase(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
        if (userRepository.existsByUsernameIgnoreCase(dto.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        UserEntity user =
                UserEntity.builder()
                        .username(dto.getUsername())
                        .email(dto.getEmail())
                        .passwordHash(passwordEncoder.encode(dto.getPassword()))
                        .build();
        return userRepository.save(user);
    }

    public UserEntity createAdminUser(String username, String email, String plainPassword) {
        UserEntity admin =
                UserEntity.builder()
                        .username(username)
                        .email(email)
                        .passwordHash(passwordEncoder.encode(plainPassword))
                        .role(Role.ADMIN)
                        .build();
        return userRepository.save(admin);
    }

    public UserEntity updateUser(String uuid, UpdateUserDto dto) {
        UserEntity user = getUserByUuid(uuid);
        if (dto.getUsername() != null && !dto.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsernameIgnoreCase(dto.getUsername())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
            }
            user.setUsername(dto.getUsername());
        }
        if (dto.getEmail() != null && !dto.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmailIgnoreCase(dto.getEmail())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
            }
            user.setEmail(dto.getEmail());
        }
        return userRepository.save(user);
    }

    public void changePassword(String uuid, ChangePasswordDto dto) {
        UserEntity user = getUserByUuid(uuid);
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Current password incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void adminDeleteUser(String uuid) {
        UserEntity user = getUserByUuid(uuid);
        if (user.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete admin user");
        }
        connectionAccessRepository.deleteAllByUserUuid(uuid);
        userRepository.deleteById(uuid);
    }

    public List<UserSummaryDto> getAllUserSummaries() {
        return userRepository.findAll().stream()
                .map(
                        u ->
                                UserSummaryDto.builder()
                                        .uuid(u.getUuid())
                                        .username(u.getUsername())
                                        .build())
                .toList();
    }

    public UserEntity toDto(UserEntity user) {
        return user;
    }
}
