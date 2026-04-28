package com.janne.mailservice.controller.v1.schema;

import com.janne.mailservice.model.admin.AdminUserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Admin — Users")
@RequestMapping("/v1/admin/users")
public interface AdminUserApi {

    @Operation(summary = "List all users")
    @GetMapping
    ResponseEntity<List<AdminUserDto>> listUsers();

    @Operation(summary = "Delete a user account")
    @ApiResponse(responseCode = "204")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteUser(@PathVariable String id);
}
