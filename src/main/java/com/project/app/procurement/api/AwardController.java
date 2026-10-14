package com.project.app.procurement.api;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.project.app.identity.security.AccountPrincipal;
import com.project.app.procurement.api.ComparisonViews.ComparisonView;
import com.project.app.procurement.api.OrderViews.OrderView;
import com.project.app.procurement.service.AwardService;
@RestController @RequestMapping("/api/procurement/rfqs/{rfqId}")
public class AwardController {
    private final AwardService awards;
    public AwardController(AwardService awards){this.awards=awards;}
    @GetMapping("/comparison") public ComparisonView comparison(@AuthenticationPrincipal AccountPrincipal buyer,@PathVariable Long rfqId){return awards.comparison(buyer,rfqId);}
    @PostMapping("/quotations/{quotationId}/accept") public OrderView accept(@AuthenticationPrincipal AccountPrincipal buyer,@PathVariable Long rfqId,@PathVariable Long quotationId){return awards.accept(buyer,rfqId,quotationId);}
}
