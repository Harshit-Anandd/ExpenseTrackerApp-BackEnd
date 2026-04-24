package com.spendsmart.auth.controller;

import com.spendsmart.auth.dto.*;
import com.spendsmart.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

    private final AuthService authService;

    @GetMapping
    public ResponseEntity<List<AdminUserDto>> getUsers(@RequestParam(required = false) String query,
                                                       @RequestParam(required = false) Boolean active,
                                                       @RequestParam(required = false) String role,
                                                       @RequestParam(required = false) String subscriptionType) {
        return ResponseEntity.ok(authService.getAllUsersForAdmin(query, active, role, subscriptionType));
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<AdminUserDto> updateUserStatus(@PathVariable Long userId,
                                                         @Valid @RequestBody AdminUserStatusUpdateDto request) {
        log.info("Admin updating user {} status to {}", userId, request.getActive());
        return ResponseEntity.ok(authService.updateUserStatus(userId, request.getActive()));
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<AdminUserDto> updateUserRole(@PathVariable Long userId,
                                                       @Valid @RequestBody AdminUserRoleUpdateDto request) {
        log.info("Admin updating user {} role to {}", userId, request.getRole());
        return ResponseEntity.ok(authService.updateUserRole(userId, request.getRole()));
    }

    @PutMapping("/{userId}/subscription")
    public ResponseEntity<AdminUserDto> updateUserSubscription(@PathVariable Long userId,
                                                               @Valid @RequestBody AdminUserSubscriptionUpdateDto request) {
        log.info("Admin updating user {} subscription to {}", userId, request.getSubscriptionType());
        return ResponseEntity.ok(authService.updateUserSubscription(userId, request.getSubscriptionType()));
    }
}

