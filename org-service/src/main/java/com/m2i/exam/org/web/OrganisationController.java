package com.m2i.exam.org.web;

import com.m2i.exam.org.service.OrganisationService;
import com.m2i.exam.org.web.OrgDtos.OrgRequest;
import com.m2i.exam.org.web.OrgDtos.OrgResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Organisations", description = "Tenants (org_info)")
@RestController
@RequestMapping("/api/orgs")
public class OrganisationController {

    private final OrganisationService service;

    public OrganisationController(OrganisationService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrgResponse create(@Valid @RequestBody OrgRequest request) {
        return service.create(request);
    }

    @Operation(summary = "Super admins see all organisations, everybody else only their own")
    @GetMapping
    public List<OrgResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public OrgResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PutMapping("/{id}")
    public OrgResponse update(@PathVariable String id, @Valid @RequestBody OrgRequest request) {
        return service.update(id, request);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.deactivate(id);
    }
}
