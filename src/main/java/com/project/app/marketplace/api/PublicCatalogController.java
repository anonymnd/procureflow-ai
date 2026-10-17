package com.project.app.marketplace.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.app.marketplace.api.PublicViews.CategoryView;
import com.project.app.marketplace.api.PublicViews.PageResponse;
import com.project.app.marketplace.api.PublicViews.PublicOfferingView;
import com.project.app.marketplace.api.PublicViews.PublicSupplierView;
import com.project.app.marketplace.service.PublicCatalogService;

@RestController
@RequestMapping("/api/public")
public class PublicCatalogController {
    private final PublicCatalogService catalog;
    public PublicCatalogController(PublicCatalogService catalog) { this.catalog = catalog; }

    @GetMapping("/categories")
    public List<CategoryView> categories() {
        return catalog.categories();
    }

    @GetMapping("/offerings")
    public PageResponse<PublicOfferingView> search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        return catalog.search(q, category, page, size);
    }

    @GetMapping("/offerings/{id}")
    public PublicOfferingView offering(@PathVariable Long id) {
        return catalog.getOffering(id);
    }

    @GetMapping("/suppliers/{id}")
    public PublicSupplierView supplier(@PathVariable Long id) {
        return catalog.getSupplier(id);
    }
}
