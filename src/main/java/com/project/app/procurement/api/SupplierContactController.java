package com.project.app.procurement.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.app.identity.security.AccountPrincipal;
import com.project.app.procurement.api.ProcurementRequests.CreateContact;
import com.project.app.procurement.api.ProcurementViews.ContactView;
import com.project.app.procurement.service.SupplierContactService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/procurement/contacts")
public class SupplierContactController {
    private final SupplierContactService contacts;
    public SupplierContactController(SupplierContactService contacts) { this.contacts = contacts; }

    @GetMapping
    public List<ContactView> list(@AuthenticationPrincipal AccountPrincipal buyer) {
        return contacts.list(buyer);
    }

    @PostMapping
    public ResponseEntity<ContactView> create(@AuthenticationPrincipal AccountPrincipal buyer,
            @Valid @RequestBody CreateContact request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contacts.create(buyer, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal AccountPrincipal buyer, @PathVariable Long id) {
        contacts.remove(buyer, id);
        return ResponseEntity.noContent().build();
    }
}
