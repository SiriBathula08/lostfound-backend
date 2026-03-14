package com.portal.lostfound.controller;

import com.portal.lostfound.dto.ApiResponse;
import com.portal.lostfound.dto.AuthResponse;
import com.portal.lostfound.dto.LoginRequest;
import com.portal.lostfound.dto.RegisterRequest;
import com.portal.lostfound.model.User;
import com.portal.lostfound.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** POST /api/auth/register */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful", response));
    }

    /** POST /api/auth/login */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    /** GET /api/auth/me */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(UserProfileResponse.from(user)));
    }

    /** PUT /api/auth/profile */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String phoneNumber) {
        User updated = authService.updateProfile(
                userDetails.getUsername(), fullName, phoneNumber);
        return ResponseEntity.ok(
                ApiResponse.ok("Profile updated", UserProfileResponse.from(updated)));
    }

    // ── Inner response DTO ─────────────────────────────────────

    public static class UserProfileResponse {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String phoneNumber;
        private String role;

        public UserProfileResponse() {}

        public UserProfileResponse(Long id, String username, String email,
                                   String fullName, String phoneNumber, String role) {
            this.id          = id;
            this.username    = username;
            this.email       = email;
            this.fullName    = fullName;
            this.phoneNumber = phoneNumber;
            this.role        = role;
        }

        public static UserProfileResponse from(User user) {
            return new UserProfileResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getPhoneNumber(),
                    user.getRole().name()
            );
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}
