package com.project.app.procurement.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.project.app.identity.repository.AccountRepository;
import com.project.app.identity.security.AccountPrincipal;
import com.project.app.procurement.api.ComparisonViews.*;
import com.project.app.procurement.api.OrderViews.OrderView;
import com.project.app.procurement.api.QuotationViews.QuotationView;
import com.project.app.procurement.model.*;
import com.project.app.procurement.repository.*;

@Service
public class AwardService {
    private final RfqRepository rfqs;
    private final RfqItemRepository requiredItems;
    private final QuotationRepository quotations;
    private final QuotationItemRepository quoteItems;
    private final PurchaseOrderRepository orders;
    private final PurchaseOrderItemRepository orderItems;
    private final AccountRepository accounts;
    private final OrderService orderService;
    private final Clock clock;
    private final jakarta.persistence.EntityManager entityManager;
    public AwardService(RfqRepository rfqs,RfqItemRepository requiredItems,QuotationRepository quotations,
            QuotationItemRepository quoteItems,PurchaseOrderRepository orders,PurchaseOrderItemRepository orderItems,
            AccountRepository accounts,OrderService orderService,Clock clock,jakarta.persistence.EntityManager entityManager){this.rfqs=rfqs;this.requiredItems=requiredItems;this.quotations=quotations;this.quoteItems=quoteItems;this.orders=orders;this.orderItems=orderItems;this.accounts=accounts;this.orderService=orderService;this.clock=clock;this.entityManager=entityManager;}
    @Transactional(readOnly=true)
    public ComparisonView comparison(AccountPrincipal buyer,Long rfqId){
        Rfq rfq=rfqs.findByIdAndOrganizationId(rfqId,buyer.getOrganizationId()).orElseThrow(ProcurementException::notFound);
        if(rfq.getStatus()==Rfq.Status.DRAFT)throw ProcurementException.conflict("Send this RFQ before comparing quotations.");
        var now=clock.instant();
        List<Quotation> responses=quotations.findSubmittedOwn(rfqId,buyer.getOrganizationId());
        List<CurrencyGroup> groups=new ArrayList<>();
        for(String currency:List.of("MAD","EUR","USD")){
            List<Quotation> sorted=responses.stream().filter(q->currency.equals(q.getCurrency())).sorted(Comparator.comparing(this::subtotal).thenComparing(Quotation::getId)).toList();
            if(sorted.isEmpty())continue;
            List<ComparisonEntry> entries=new ArrayList<>();boolean lowest=false;
            for(Quotation q:sorted){List<QuotationItem> lines=quoteItems.findByQuotationIdOrderByRfqItemIdAsc(q.getId());boolean eligible=rfq.getStatus()==Rfq.Status.SENT&&q.getStatus()==Quotation.Status.SUBMITTED&&q.getValidUntil()!=null&&q.getValidUntil().isAfter(now)&&complete(rfq,lines);boolean highlight=eligible&&!lowest;if(highlight)lowest=true;entries.add(new ComparisonEntry(QuotationView.from(q,lines,now),eligible,highlight));}
            groups.add(new CurrencyGroup(currency,entries));
        }
        return new ComparisonView(rfqId,rfq.getStatus().name(),rfq.getWinningQuotationId(),orders.findByRfqId(rfqId).map(PurchaseOrder::getId).orElse(null),groups);
    }
    @Transactional
    public OrderView accept(AccountPrincipal buyer,Long rfqId,Long quotationId){
        Rfq rfq=rfqs.findLockedByIdAndOrganizationId(rfqId,buyer.getOrganizationId()).orElseThrow(ProcurementException::notFound);
        Optional<PurchaseOrder> existing=orders.findLockedByRfqId(rfqId);
        if(existing.isPresent()){
            // RFQ's inverse relation may have loaded the order before this lock waited.
            // Refresh while holding the row lock so replay returns committed progress.
            entityManager.refresh(existing.get(),jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            if(!existing.get().getQuotation().getId().equals(quotationId))throw ProcurementException.conflict("This RFQ already has a different winning quotation.");
            return orderService.view(existing.get(),buyer);
        }
        if(rfq.getStatus()!=Rfq.Status.SENT)throw ProcurementException.conflict("Only a sent RFQ can be awarded.");
        Quotation quotation=quotations.findLockedBuyer(quotationId,rfqId,buyer.getOrganizationId()).orElseThrow(ProcurementException::notFound);
        List<QuotationItem> lines=quoteItems.findByQuotationIdOrderByRfqItemIdAsc(quotationId);
        var now=clock.instant();
        if(quotation.getStatus()!=Quotation.Status.SUBMITTED||quotation.getValidUntil()==null||!quotation.getValidUntil().isAfter(now)
                ||!List.of("MAD","EUR","USD").contains(quotation.getCurrency())||!complete(rfq,lines))
            throw ProcurementException.conflict("Choose a complete submitted quotation that is still valid.");
        quotation.accept();rfq.award(quotation);
        PurchaseOrder order=orders.saveAndFlush(new PurchaseOrder(rfq,quotation,accounts.getReferenceById(buyer.getAccountId()),now));
        orderItems.saveAllAndFlush(lines.stream().map(line->new PurchaseOrderItem(order,line)).toList());
        return orderService.view(order,buyer);
    }
    private boolean complete(Rfq rfq,List<QuotationItem> lines){
        Set<Long> required=new HashSet<>();requiredItems.findByRfqIdOrderByIdAsc(rfq.getId()).forEach(i->required.add(i.getId()));
        Set<Long> priced=new HashSet<>();
        for(QuotationItem line:lines){if(line.getUnitPrice()==null||line.getUnitPrice().signum()<=0)return false;priced.add(line.getRfqItem().getId());}
        return !required.isEmpty()&&required.equals(priced)&&lines.size()==required.size();
    }
    private BigDecimal subtotal(Quotation q){return quoteItems.findByQuotationIdOrderByRfqItemIdAsc(q.getId()).stream().map(i->i.getUnitPrice().multiply(BigDecimal.valueOf(i.getRfqItem().getQuantity()))).reduce(BigDecimal.ZERO,BigDecimal::add);}
}
