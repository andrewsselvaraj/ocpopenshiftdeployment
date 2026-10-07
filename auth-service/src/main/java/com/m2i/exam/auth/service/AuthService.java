package com.m2i.exam.auth.service;

import com.m2i.exam.auth.domain.AdminAccount;
import com.m2i.exam.auth.domain.AdminRepository;
import com.m2i.exam.auth.domain.UserAccount;
import com.m2i.exam.auth.domain.UserRepository;
import com.m2i.exam.auth.web.AuthDtos.LoginRequest;
import com.m2i.exam.auth.web.AuthDtos.LoginResponse;
import com.m2i.exam.auth.web.AuthDtos.Profile;
import com.m2i.exam.auth.web.AuthDtos.UserResponse;
import com.m2i.exam.common.domain.RecordStatus;
import com.m2i.exam.common.security.CurrentUser;
import com.m2i.exam.common.security.Role;
import com.m2i.exam.common.security.TokenService;
import com.m2i.exam.common.security.TokenService.IssuedToken;
import com.m2i.exam.common.web.ApiException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final AdminRepository admins;
    private final UserRepository users;
    private final PasswordService passwords;
    private final TokenService tokens;

    public AuthService(AdminRepository admins, UserRepository users, PasswordService passwords, TokenService tokens) {
        this.admins = admins;
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
    }

    /** Super admins (admin_info) are checked first, then organisation users (user_info). */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim();

        Optional<AdminAccount> admin = admins.findFirstByEmailAndStatusOrderByUpdatedAtDesc(email, RecordStatus.ACTIVE);
        if (admin.isPresent()) {
            AdminAccount a = admin.get();
            verify(request.password(), a.getPassword(), email);
            if (passwords.needsUpgrade(a.getPassword())) {
                a.setPassword(passwords.hash(request.password()));
                log.info("Upgraded legacy plain-text password to BCrypt for admin {}", a.getId());
            }
            return issue(new Profile(a.getId(), a.getName(), a.getEmail(), Role.SUPER_ADMIN, null));
        }

        Optional<UserAccount> user = users.findFirstByEmailAndStatusOrderByUpdatedAtDesc(email, RecordStatus.ACTIVE);
        if (user.isEmpty()) {
            passwords.simulateCheck(request.password());
            throw ApiException.unauthorized(INVALID_CREDENTIALS);
        }
        UserAccount u = user.get();
        verify(request.password(), u.getPassword(), email);
        if (passwords.needsUpgrade(u.getPassword())) {
            u.setPassword(passwords.hash(request.password()));
            log.info("Upgraded legacy plain-text password to BCrypt for user {}", u.getId());
        }
        UserResponse view = UserResponse.from(u);
        return issue(new Profile(u.getId(), u.getName(), u.getEmail(), view.role(), u.getOrgId()));
    }

    private void verify(String raw, String stored, String email) {
        if (!passwords.matches(raw, stored)) {
            log.warn("Failed login for {}", email);
            throw ApiException.unauthorized(INVALID_CREDENTIALS);
        }
    }

    private LoginResponse issue(Profile profile) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CurrentUser.USER_ID_CLAIM, profile.id());
        claims.put(CurrentUser.NAME_CLAIM, profile.name());
        claims.put(CurrentUser.ORG_CLAIM, profile.orgId());
        IssuedToken token = tokens.issue(profile.email(), List.of(profile.role()), claims);
        return new LoginResponse(token.value(), "Bearer", token.expiresInSeconds(), profile);
    }
}
