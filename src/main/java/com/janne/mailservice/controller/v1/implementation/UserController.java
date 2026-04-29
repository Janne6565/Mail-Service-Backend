package com.janne.mailservice.controller.v1.implementation;

import com.janne.mailservice.controller.v1.schema.UserApi;
import com.janne.mailservice.entity.UserEntity;
import com.janne.mailservice.model.action.ChangePasswordDto;
import com.janne.mailservice.model.action.UpdateUserDto;
import com.janne.mailservice.model.core.UserDto;
import com.janne.mailservice.model.core.UserSummaryDto;
import com.janne.mailservice.services.auth.SecurityContextService;
import com.janne.mailservice.services.core.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final SecurityContextService securityContextService;
    private final UserService userService;

    @Override
    public ResponseEntity<UserDto> getMe() {
        UserEntity user = securityContextService.requireUser();
        return ResponseEntity.ok(toDto(user));
    }

    @Override
    public ResponseEntity<UserDto> updateMe(UpdateUserDto dto) {
        UserEntity user = securityContextService.requireUser();
        UserEntity updated = userService.updateUser(user.getUuid(), dto);
        return ResponseEntity.ok(toDto(updated));
    }

    @Override
    public ResponseEntity<Void> changePassword(ChangePasswordDto dto) {
        UserEntity user = securityContextService.requireUser();
        userService.changePassword(user.getUuid(), dto);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<UserSummaryDto>> listUsers() {
        securityContextService.requireUser();
        return ResponseEntity.ok(userService.getAllUserSummaries());
    }

    private UserDto toDto(UserEntity user) {
        return UserDto.builder()
                .uuid(user.getUuid())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
