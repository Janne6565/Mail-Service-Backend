package com.janne.mailservice.controller.v1.implementation;

import com.janne.mailservice.controller.v1.schema.AdminUserApi;
import com.janne.mailservice.model.admin.AdminUserDto;
import com.janne.mailservice.services.core.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminUserController implements AdminUserApi {

    private final UserService userService;

    @Override
    public ResponseEntity<List<AdminUserDto>> listUsers() {
        return ResponseEntity.ok(
                userService.getAllUsers().stream()
                        .map(
                                u ->
                                        AdminUserDto.builder()
                                                .uuid(u.getUuid())
                                                .username(u.getUsername())
                                                .email(u.getEmail())
                                                .role(u.getRole().name())
                                                .createdAt(u.getCreatedAt())
                                                .build())
                        .toList());
    }

    @Override
    public ResponseEntity<Void> deleteUser(String id) {
        userService.adminDeleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
