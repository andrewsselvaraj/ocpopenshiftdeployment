package com.m2i.exam.auth.service;

import com.m2i.exam.auth.domain.AdminAccount;
import com.m2i.exam.auth.domain.AdminRepository;
import com.m2i.exam.common.domain.Ids;
import com.m2i.exam.common.domain.RecordStatus;
import com.m2i.exam.common.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first super admin on an empty database (BOOTSTRAP_ADMIN_EMAIL / BOOTSTRAP_ADMIN_PASSWORD).
 * Does nothing when the variables are unset or the account already exists, so it is safe on every start.
 */
@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final AdminRepository admins;
    private final PasswordService passwords;
    private final String email;
    private final String password;

    public BootstrapAdminRunner(AdminRepository admins, PasswordService passwords,
                                @Value("${app.bootstrap.admin-email:}") String email,
                                @Value("${app.bootstrap.admin-password:}") String password) {
        this.admins = admins;
        this.passwords = passwords;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        String normalised = email.trim().toLowerCase();
        if (admins.existsByEmail(normalised)) {
            return;
        }
        admins.save(new AdminAccount(Ids.newId(), "Platform Admin", normalised, passwords.hash(password),
                Role.SUPER_ADMIN.label(), RecordStatus.ACTIVE));
        log.info("Bootstrap super admin {} created", normalised);
    }
}
