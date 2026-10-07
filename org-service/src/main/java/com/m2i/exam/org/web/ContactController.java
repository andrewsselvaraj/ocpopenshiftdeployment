package com.m2i.exam.org.web;

import com.m2i.exam.common.domain.Ids;
import com.m2i.exam.common.domain.RecordStatus;
import com.m2i.exam.org.domain.ContactMessage;
import com.m2i.exam.org.domain.ContactMessageRepository;
import com.m2i.exam.org.web.OrgDtos.ContactRequest;
import com.m2i.exam.org.web.OrgDtos.ContactResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Contact", description = "Website contact form (contact_m2i)")
@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final ContactMessageRepository messages;

    public ContactController(ContactMessageRepository messages) {
        this.messages = messages;
    }

    @Operation(summary = "Public: submit a contact message")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ContactResponse submit(@Valid @RequestBody ContactRequest request) {
        return ContactResponse.from(messages.save(new ContactMessage(Ids.newId(), request.name().trim(),
                request.email().trim(), request.subject().trim(), request.message().trim(), RecordStatus.ACTIVE)));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping
    @Transactional(readOnly = true)
    public Page<ContactResponse> list(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                                      Pageable pageable) {
        return messages.findByStatus(RecordStatus.ACTIVE, pageable).map(ContactResponse::from);
    }
}
