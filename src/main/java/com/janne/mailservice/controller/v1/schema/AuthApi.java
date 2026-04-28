package com.janne.mailservice.controller.v1.schema;

import com.janne.mailservice.model.action.LoginDto;
import com.janne.mailservice.model.action.TokenMode;
import com.janne.mailservice.model.action.UserCreationDto;
import com.janne.mailservice.model.core.InvitePreviewDto;
import com.janne.mailservice.model.core.LoginResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Auth", description = "Authentication and account creation")
@RequestMapping("/v1/auth")
public interface AuthApi {

    @Operation(summary = "Login with email and password")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginDto loginDto,
            @Parameter(description = "COOKIE sets httpOnly cookie, DIRECT returns token in body")
                    @RequestParam(value = "tokenMode", defaultValue = "COOKIE")
                    TokenMode tokenMode);

    @Operation(summary = "Preview an invite link before showing the registration form")
    @ApiResponse(responseCode = "200", description = "Preview returned (check valid field)")
    @GetMapping("/invite/{token}")
    ResponseEntity<InvitePreviewDto> previewInvite(@PathVariable String token);

    @Operation(summary = "Register a new account using an invite link")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired invite token"),
        @ApiResponse(responseCode = "409", description = "Email or username already in use")
    })
    @PostMapping("/register")
    ResponseEntity<LoginResponseDto> register(
            @Valid @RequestBody UserCreationDto dto,
            @Parameter(description = "Invite token from the invite link")
                    @RequestParam("inviteToken")
                    String inviteToken,
            @Parameter(description = "COOKIE sets httpOnly cookie, DIRECT returns token in body")
                    @RequestParam(value = "tokenMode", defaultValue = "COOKIE")
                    TokenMode tokenMode);

    @Operation(summary = "Exchange refresh token cookie for a short-lived identity token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Identity token returned"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid refresh token")
    })
    @GetMapping("/token")
    ResponseEntity<String> fetchToken(@CookieValue(value = "refreshToken") String refreshToken);

    @Operation(summary = "Logout — clears the refresh token cookie")
    @ApiResponse(responseCode = "204", description = "Logged out")
    @PostMapping("/logout")
    ResponseEntity<Void> logout();
}
