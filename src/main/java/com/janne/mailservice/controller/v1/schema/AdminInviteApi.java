package com.janne.mailservice.controller.v1.schema;

import com.janne.mailservice.model.action.CreateInviteDto;
import com.janne.mailservice.model.core.InviteDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Admin — Invites")
@RequestMapping("/v1/admin/invites")
public interface AdminInviteApi {

    @Operation(summary = "List all invite links")
    @GetMapping
    ResponseEntity<List<InviteDto>> listInvites();

    @Operation(summary = "Create a new invite link")
    @ApiResponse(responseCode = "201")
    @PostMapping
    ResponseEntity<InviteDto> createInvite(@Valid @RequestBody CreateInviteDto dto);

    @Operation(summary = "Delete an invite link")
    @ApiResponse(responseCode = "204")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteInvite(@PathVariable String id);
}
