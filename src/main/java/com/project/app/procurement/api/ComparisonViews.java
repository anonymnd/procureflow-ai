package com.project.app.procurement.api;
import java.util.List;
import com.project.app.procurement.api.QuotationViews.QuotationView;
public final class ComparisonViews {
    private ComparisonViews() { }
    public record ComparisonEntry(@com.fasterxml.jackson.annotation.JsonUnwrapped QuotationView quotation,boolean eligible,boolean lowestEligible) { }
    public record CurrencyGroup(String currency,List<ComparisonEntry> quotations) { }
    public record ComparisonView(Long rfqId,String status,Long awardedQuotationId,Long orderId,List<CurrencyGroup> groups) { }
}
