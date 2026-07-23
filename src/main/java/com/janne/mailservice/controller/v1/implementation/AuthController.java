package com.janne.mailservice.controller.v1.implementation;

import com.janne.mailservice.controller.v1.schema.AuthApi;
import com.janne.mailservice.entity.UserEntity;
import com.janne.mailservice.model.action.LoginDto;
import com.janne.mailservice.model.action.TokenMode;
import com.janne.mailservice.model.action.UserCreationDto;
import com.janne.mailservice.model.core.InvitePreviewDto;
import com.janne.mailservice.model.core.LoginResponseDto;
import com.janne.mailservice.services.auth.AuthService;
import com.janne.mailservice.services.auth.RefreshCookieFactory;
import com.janne.mailservice.services.core.InviteService;
import com.janne.mailservice.services.core.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;
    private final UserService userService;
    private final InviteService inviteService;
    private final RefreshCookieFactory refreshCookieFactory;

    @Override
    public ResponseEntity<LoginResponseDto> login(LoginDto loginDto, TokenMode tokenMode) {
        log.info("Login attempt for user: {}", loginDto.getEmail());
        String refreshToken = authService.loginUser(loginDto.getEmail(), loginDto.getPassword());
        return buildRefreshTokenResponse(refreshToken, tokenMode, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<InvitePreviewDto> previewInvite(String token) {
        return ResponseEntity.ok(inviteService.previewInvite(token));
    }

    @Override
    public ResponseEntity<LoginResponseDto> register(
            UserCreationDto dto, String inviteToken, TokenMode tokenMode) {
        UserEntity user = userService.createUser(dto);
        inviteService.validateAndConsume(inviteToken, user.getUuid());
        String refreshToken = authService.generateRefreshToken(user.getUuid());
        return buildRefreshTokenResponse(refreshToken, tokenMode, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<String> fetchToken(String refreshToken) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache")
                .body(authService.fetchIdentityTokenFromRefreshToken(refreshToken));
    }

    @Override
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.expire().toString())
                .build();
    }

    private ResponseEntity<LoginResponseDto> buildRefreshTokenResponse(
            String refreshToken, TokenMode tokenMode, HttpStatus successStatus) {
        if (tokenMode == TokenMode.DIRECT) {
            return ResponseEntity.status(successStatus)
                    .cacheControl(CacheControl.noStore())
                    .header("Pragma", "no-cache")
                    .body(LoginResponseDto.builder().refreshToken(refreshToken).build());
        }

        ResponseCookie responseCookie = refreshCookieFactory.createStrict(refreshToken);
        return ResponseEntity.status(successStatus)
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .build();
    }
}
