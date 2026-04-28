package com.janne.mailservice.controller.v1.schema;

import com.janne.mailservice.model.core.MailDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Mails", description = "Read sent mail history via API key (scope: READ_MAILS)")
@RequestMapping("/v1/mails")
@SecurityRequirement(name = "apiKey")
public interface MailApi {

    @Operation(summary = "List mails sent through the connection bound to the API key")
    @ApiResponse(responseCode = "200", description = "Page of mail records")
    @ApiResponse(responseCode = "403", description = "API key lacks READ_MAILS scope")
    @GetMapping
    ResponseEntity<Page<MailDto>> listMails(
            @Parameter(description = "Zero-based page number") @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20")
                    int size);

    @Operation(summary = "Get a single mail record by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Mail record"),
        @ApiResponse(responseCode = "403", description = "API key lacks READ_MAILS scope"),
        @ApiResponse(responseCode = "404", description = "Mail not found")
    })
    @GetMapping("/{id}")
    ResponseEntity<MailDto> getMail(@PathVariable String id);
}
