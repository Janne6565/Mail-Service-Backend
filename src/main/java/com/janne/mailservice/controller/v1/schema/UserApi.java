package com.janne.mailservice.controller.v1.schema;

import com.janne.mailservice.model.action.ChangePasswordDto;
import com.janne.mailservice.model.action.UpdateUserDto;
import com.janne.mailservice.model.core.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "User", description = "Own profile management")
@RequestMapping("/v1/user")
public interface UserApi {

    @Operation(summary = "Get own profile")
    @ApiResponse(responseCode = "200", description = "Profile returned")
    @GetMapping("/me")
    ResponseEntity<UserDto> getMe();

    @Operation(summary = "Update own profile (username and/or email)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile updated"),
        @ApiResponse(responseCode = "409", description = "Username or email already in use")
    })
    @PatchMapping("/me")
    ResponseEntity<UserDto> updateMe(@Valid @RequestBody UpdateUserDto dto);

    @Operation(summary = "Change own password")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Password changed"),
        @ApiResponse(responseCode = "401", description = "Current password incorrect")
    })
    @PostMapping("/me/change-password")
    ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordDto dto);
}
