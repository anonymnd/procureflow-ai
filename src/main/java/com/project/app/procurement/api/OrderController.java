package com.project.app.procurement.api;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.project.app.identity.security.AccountPrincipal;
import com.project.app.procurement.api.OrderViews.OrderView;
import com.project.app.procurement.service.OrderService;
@RestController @RequestMapping("/api/procurement/orders")
public class OrderController {
    private final OrderService orders;
    public OrderController(OrderService orders){this.orders=orders;}
    @GetMapping public List<OrderView> list(@AuthenticationPrincipal AccountPrincipal participant){return orders.list(participant);}
    @GetMapping("/{orderId}") public OrderView get(@AuthenticationPrincipal AccountPrincipal participant,@PathVariable Long orderId){return orders.get(participant,orderId);}
    @PostMapping("/{orderId}/confirm") public OrderView confirm(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable Long orderId){return orders.transition(actor,orderId,OrderService.Action.CONFIRM);}
    @PostMapping("/{orderId}/start-processing") public OrderView start(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable Long orderId){return orders.transition(actor,orderId,OrderService.Action.START_PROCESSING);}
    @PostMapping("/{orderId}/dispatch") public OrderView dispatch(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable Long orderId){return orders.transition(actor,orderId,OrderService.Action.DISPATCH);}
    @PostMapping("/{orderId}/receive") public OrderView receive(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable Long orderId){return orders.transition(actor,orderId,OrderService.Action.RECEIVE);}
}
