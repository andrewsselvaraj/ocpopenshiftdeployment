package com.m2i.exam.auth.service;

import com.m2i.exam.auth.domain.AdminAccount;
import com.m2i.exam.auth.domain.AdminRepository;
import com.m2i.exam.auth.domain.UserAccount;
import com.m2i.exam.auth.domain.UserRepository;
import com.m2i.exam.auth.web.AuthDtos.AdminResponse;
import com.m2i.exam.auth.web.AuthDtos.ChangePasswordRequest;
import com.m2i.exam.auth.web.AuthDtos.CreateAdminRequest;
import com.m2i.exam.auth.web.AuthDtos.CreateUserRequest;
import com.m2i.exam.auth.web.AuthDtos.UpdateUserRequest;
import com.m2i.exam.auth.web.AuthDtos.UserResponse;
import com.m2i.exam.common.domain.Ids;
import com.m2i.exam.common.domain.RecordStatus;
import com.m2i.exam.common.security.CurrentUser;
import com.m2i.exam.common.security.Role;
import com.m2i.exam.common.web.ApiException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final AdminRepository admins;
    private final PasswordService passwords;

    public UserService(UserRepository users, AdminRepository admins, PasswordService passwords) {
        this.users = users;
        this.admins = admins;
        this.passwords = passwords;
    }

    @Transactional
    public UserResponse create(CreateUserRequest req) {
        CurrentUser.assertOrgAccess(req.orgId());
        String email = req.email().trim().toLowerCase();
        assertEmailFree(email);
        UserAccount user = new UserAccount(Ids.newId(), req.orgId(), req.name().trim(), email,
                passwords.hash(req.password()), Role.fromLabel(req.role()).label(), CurrentUser.email(),
                RecordStatus.ACTIVE);
        return UserResponse.from(users.save(user));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(String orgId, Pageable pageable) {
        String org = CurrentUser.resolveOrg(orgId);
        Page<UserAccount> page = org == null
                ? users.findByStatus(RecordStatus.ACTIVE, pageable)
                : users.findByOrgIdAndStatus(org, RecordStatus.ACTIVE, pageable);
        return page.map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse get(String id) {
        return UserResponse.from(load(id));
    }

    @Transactional
    public UserResponse update(String id, UpdateUserRequest req) {
        UserAccount user = load(id);
        if (req.name() != null) {
            user.setName(req.name().trim());
        }
        if (req.role() != null) {
            user.setRole(Role.fromLabel(req.role()).label());
        }
        user.setUpdatedBy(CurrentUser.email());
        return UserResponse.from(user);
    }

    @Transactional
    public void deactivate(String id) {
        UserAccount user = load(id);
        user.setStatus(RecordStatus.DELETED);
        user.setUpdatedBy(CurrentUser.email());
    }

    @Transactional
    public void changeOwnPassword(ChangePasswordRequest req) {
        String email = CurrentUser.email();
        if (CurrentUser.isSuperAdmin()) {
            AdminAccount admin = admins.findFirstByEmailAndStatusOrderByUpdatedAtDesc(email, RecordStatus.ACTIVE)
                    .orElseThrow(() -> ApiException.notFound("Admin", email));
            checkCurrent(req.currentPassword(), admin.getPassword());
            admin.setPassword(passwords.hash(req.newPassword()));
        } else {
            UserAccount user = users.findFirstByEmailAndStatusOrderByUpdatedAtDesc(email, RecordStatus.ACTIVE)
                    .orElseThrow(() -> ApiException.notFound("User", email));
            checkCurrent(req.currentPassword(), user.getPassword());
            user.setPassword(passwords.hash(req.newPassword()));
        }
    }

    @Transactional
    public AdminResponse createAdmin(CreateAdminRequest req) {
        String email = req.email().trim().toLowerCase();
        assertEmailFree(email);
        AdminAccount admin = new AdminAccount(Ids.newId(), req.name().trim(), email,
                passwords.hash(req.password()), Role.SUPER_ADMIN.label(), RecordStatus.ACTIVE);
        return AdminResponse.from(admins.save(admin));
    }

    @Transactional(readOnly = true)
    public List<AdminResponse> listAdmins() {
        return admins.findByStatusOrderByName(RecordStatus.ACTIVE).stream().map(AdminResponse::from).toList();
    }

    /** Loads a user and enforces that non-super-admins only touch their own organisation. */
    private UserAccount load(String id) {
        UserAccount user = users.findById(id).orElseThrow(() -> ApiException.notFound("User", id));
        CurrentUser.assertOrgAccess(user.getOrgId());
        return user;
    }

    private void assertEmailFree(String email) {
        if (users.existsByEmail(email) || admins.existsByEmail(email)) {
            throw ApiException.conflict("E-mail " + email + " is already registered");
        }
    }

    private void checkCurrent(String raw, String stored) {
        if (!passwords.matches(raw, stored)) {
            throw ApiException.badRequest("Current password is incorrect");
        }
    }
}
