package com.m2i.exam.auth.web;

import com.m2i.exam.auth.service.UserService;
import com.m2i.exam.auth.web.AuthDtos.AdminResponse;
import com.m2i.exam.auth.web.AuthDtos.CreateAdminRequest;
import com.m2i.exam.common.security.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Super admins", description = "Platform administrators (admin_info)")
@RestController
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/api/admins")
    public List<AdminResponse> list() {
        return userService.listAdmins();
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/api/admins")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminResponse create(@Valid @RequestBody CreateAdminRequest request) {
        return userService.createAdmin(request);
    }

    @Operation(summary = "Roles that can be assigned to organisation users")
    @GetMapping("/api/roles")
    public List<Map<String, String>> roles() {
        return Arrays.stream(Role.values())
                .filter(r -> r != Role.SERVICE && r != Role.SUPER_ADMIN)
                .map(r -> Map.of("code", r.name(), "label", r.label()))
                .toList();
    }
}
