package com.m2i.exam.org.service;

import com.m2i.exam.common.domain.Ids;
import com.m2i.exam.common.domain.RecordStatus;
import com.m2i.exam.common.security.CurrentUser;
import com.m2i.exam.common.web.ApiException;
import com.m2i.exam.org.domain.Organisation;
import com.m2i.exam.org.domain.OrganisationRepository;
import com.m2i.exam.org.web.OrgDtos.OrgRequest;
import com.m2i.exam.org.web.OrgDtos.OrgResponse;
import java.util.List;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganisationService {

    private final OrganisationRepository orgs;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public OrganisationService(OrganisationRepository orgs) {
        this.orgs = orgs;
    }

    @Transactional
    public OrgResponse create(OrgRequest req) {
        String email = req.email().trim().toLowerCase();
        if (orgs.existsByEmail(email)) {
            throw ApiException.conflict("An organisation with e-mail " + email + " already exists");
        }
        // org_password is NOT NULL in the legacy schema but organisation-level login is retired:
        // store an unusable random hash so the column never holds a real credential.
        Organisation org = new Organisation(Ids.newId(), req.name().trim(), email, encoder.encode(Ids.newId()),
                CurrentUser.email(), RecordStatus.ACTIVE);
        return OrgResponse.from(orgs.save(org));
    }

    @Transactional(readOnly = true)
    public List<OrgResponse> list() {
        if (CurrentUser.isSuperAdmin()) {
            return orgs.findByStatusOrderByName(RecordStatus.ACTIVE).stream().map(OrgResponse::from).toList();
        }
        return orgs.findById(CurrentUser.orgId()).map(OrgResponse::from).stream().toList();
    }

    @Transactional(readOnly = true)
    public OrgResponse get(String id) {
        return OrgResponse.from(loadActive(id));
    }

    @Transactional
    public OrgResponse update(String id, OrgRequest req) {
        Organisation org = loadActive(id);
        org.setName(req.name().trim());
        org.setEmail(req.email().trim().toLowerCase());
        org.setUpdatedBy(CurrentUser.email());
        return OrgResponse.from(org);
    }

    @Transactional
    public void deactivate(String id) {
        Organisation org = loadActive(id);
        org.setStatus(RecordStatus.DELETED);
        org.setUpdatedBy(CurrentUser.email());
    }

    /** Loads an active organisation the caller may access; used by the catalog service too. */
    @Transactional(readOnly = true)
    public Organisation loadActive(String id) {
        CurrentUser.assertOrgAccess(id);
        Organisation org = orgs.findById(id).orElseThrow(() -> ApiException.notFound("Organisation", id));
        if (!RecordStatus.isActive(org.getStatus())) {
            throw ApiException.notFound("Organisation", id);
        }
        return org;
    }
}
