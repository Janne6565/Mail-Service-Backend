package com.janne.mailservice.controller.v1.schema;

import com.janne.mailservice.model.action.SendMailDto;
import com.janne.mailservice.model.core.MailDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Send", description = "Send email via API key (scope: SEND)")
@RequestMapping("/v1/send")
@SecurityRequirement(name = "apiKey")
public interface SendApi {

    @Operation(summary = "Send an email through the connection bound to the API key")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Mail record returned (check success field)"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid API key"),
        @ApiResponse(responseCode = "403", description = "API key lacks SEND scope")
    })
    @PostMapping
    ResponseEntity<MailDto> sendMail(@Valid @RequestBody SendMailDto dto);
}
