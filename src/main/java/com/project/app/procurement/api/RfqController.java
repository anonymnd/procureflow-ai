package com.project.app.procurement.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.app.identity.security.AccountPrincipal;
import com.project.app.procurement.api.ProcurementRequests.CreateRfq;
import com.project.app.procurement.api.ProcurementRequests.RfqItemInput;
import com.project.app.procurement.api.ProcurementRequests.SendRfq;
import com.project.app.procurement.api.ProcurementRequests.UpdateRfq;
import com.project.app.procurement.api.ProcurementViews.OrganizationSummaryView;
import com.project.app.procurement.api.ProcurementViews.RfqItemView;
import com.project.app.procurement.api.ProcurementViews.RfqView;
import com.project.app.procurement.service.RfqService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/procurement")
public class RfqController {
    private final RfqService rfqService;
    public RfqController(RfqService rfqService) { this.rfqService = rfqService; }

    @GetMapping("/recipients")
    public List<OrganizationSummaryView> searchRecipients(@RequestParam(name = "q", required = false) String q) {
        return rfqService.searchRecipients(q);
    }

    @GetMapping("/rfqs")
    public List<RfqView> list(@AuthenticationPrincipal AccountPrincipal buyer) {
        return rfqService.listOwn(buyer);
    }

    @GetMapping("/rfqs/{id}")
    public RfqView get(@AuthenticationPrincipal AccountPrincipal buyer, @PathVariable Long id) {
        return rfqService.getOwn(buyer, id);
    }

    @PostMapping("/rfqs")
    public ResponseEntity<RfqView> create(@AuthenticationPrincipal AccountPrincipal buyer, @Valid @RequestBody CreateRfq request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rfqService.create(buyer, request));
    }

    @PutMapping("/rfqs/{id}")
    public RfqView updateTitle(@AuthenticationPrincipal AccountPrincipal buyer, @PathVariable Long id,
            @Valid @RequestBody UpdateRfq request) {
        return rfqService.updateTitle(buyer, id, request);
    }

    @PostMapping("/rfqs/{id}/items")
    public ResponseEntity<RfqItemView> addItem(@AuthenticationPrincipal AccountPrincipal buyer, @PathVariable Long id,
            @Valid @RequestBody RfqItemInput request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rfqService.addItem(buyer, id, request));
    }

    @PutMapping("/rfqs/{id}/items/{itemId}")
    public RfqItemView updateItem(@AuthenticationPrincipal AccountPrincipal buyer, @PathVariable Long id,
            @PathVariable Long itemId, @Valid @RequestBody RfqItemInput request) {
        return rfqService.updateItem(buyer, id, itemId, request);
    }

    @DeleteMapping("/rfqs/{id}/items/{itemId}")
    public ResponseEntity<Void> removeItem(@AuthenticationPrincipal AccountPrincipal buyer, @PathVariable Long id,
            @PathVariable Long itemId) {
        rfqService.removeItem(buyer, id, itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rfqs/{id}/send")
    public RfqView send(@AuthenticationPrincipal AccountPrincipal buyer, @PathVariable Long id,
            @Valid @RequestBody SendRfq request) {
        return rfqService.send(buyer, id, request);
    }
}
