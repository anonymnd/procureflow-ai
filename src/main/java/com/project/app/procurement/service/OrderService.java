package com.project.app.procurement.service;

import java.util.List;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import com.project.app.identity.repository.AccountRepository;
import com.project.app.procurement.model.OrderEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.project.app.identity.security.AccountPrincipal;
import com.project.app.procurement.api.OrderViews.OrderView;
import com.project.app.procurement.model.PurchaseOrder;
import com.project.app.procurement.repository.*;

@Service
public class OrderService {
    public enum Action {
        CONFIRM("confirm","CREATED","CONFIRMED",false),
        START_PROCESSING("start-processing","CONFIRMED","PROCESSING",false),
        DISPATCH("dispatch","PROCESSING","DISPATCHED",false),
        RECEIVE("receive","DISPATCHED","RECEIVED",true);
        final String path,predecessor,target;final boolean buyer;
        Action(String path,String predecessor,String target,boolean buyer){this.path=path;this.predecessor=predecessor;this.target=target;this.buyer=buyer;}
    }
    private final PurchaseOrderRepository orders;
    private final PurchaseOrderItemRepository items;
    private final OrderEventRepository events;
    private final AccountRepository accounts;
    private final Clock clock;
    public OrderService(PurchaseOrderRepository orders,PurchaseOrderItemRepository items,OrderEventRepository events,AccountRepository accounts,Clock clock){this.orders=orders;this.items=items;this.events=events;this.accounts=accounts;this.clock=clock;}
    @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ) public List<OrderView> list(AccountPrincipal participant){return orders.findParticipant(participant.getOrganizationId(),participant.isBuyerGrant(),participant.isSupplierGrant()).stream().map(order->view(order,participant)).toList();}
    @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ) public OrderView get(AccountPrincipal participant,Long orderId){return view(orders.findParticipantById(orderId,participant.getOrganizationId(),participant.isBuyerGrant(),participant.isSupplierGrant()).orElseThrow(ProcurementException::notFound),participant);}
    @Transactional public OrderView transition(AccountPrincipal actor,Long orderId,Action action){
        PurchaseOrder order=orders.findLockedParticipant(orderId,actor.getOrganizationId()).orElseThrow(ProcurementException::notFound);
        if(!authorized(order,actor,action))throw new ProcurementException(HttpStatus.FORBIDDEN,"ACCESS_DENIED","Only the permitted order participant can perform this action.");
        if(events.existsByOrderIdAndTargetStatus(orderId,action.target))return view(order,actor);
        order.transition(action.predecessor,action.target);
        events.saveAndFlush(new OrderEvent(order,action.target,clock.instant(),accounts.getReferenceById(actor.getAccountId())));
        return view(order,actor);
    }
    private boolean authorized(PurchaseOrder order,AccountPrincipal actor,Action action){return action.buyer
        ?actor.isBuyerGrant()&&order.getBuyerOrganization().getId().equals(actor.getOrganizationId())
        :actor.isSupplierGrant()&&order.getSupplierOrganization().getId().equals(actor.getOrganizationId());}
    public OrderView view(PurchaseOrder order,AccountPrincipal actor){
        List<String> allowed=java.util.Arrays.stream(Action.values()).filter(action->action.predecessor.equals(order.getStatus())&&authorized(order,actor,action)).map(action->action.path).toList();
        return OrderView.from(order,items.findByOrderIdOrderByRfqItem_IdAsc(order.getId()),events.findByOrderIdOrderByIdAsc(order.getId()),allowed);
    }
}
