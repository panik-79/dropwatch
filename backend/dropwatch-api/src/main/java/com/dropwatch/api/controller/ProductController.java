package com.dropwatch.api.controller;

import com.dropwatch.api.dto.ProductDto;
import com.dropwatch.api.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Product parsing, catalog search, and time-series price history")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/parse-url")
    @Operation(summary = "Parse merchant URL (Myntra/Flipkart) and return extracted product details")
    public ResponseEntity<ProductDto.ProductResponse> parseUrl(@RequestBody ProductDto.ParseUrlRequest req) {
        return ResponseEntity.ok(productService.parseAndSaveProductUrl(req.url()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product details by ID")
    public ResponseEntity<ProductDto.ProductResponse> getProduct(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Search product catalog by title query")
    public ResponseEntity<List<ProductDto.ProductResponse>> search(@RequestParam String query) {
        return ResponseEntity.ok(productService.searchProducts(query));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get time-series price snapshot history for product variant")
    public ResponseEntity<ProductDto.PriceHistoryResponse> getPriceHistory(
            @PathVariable String id,
            @RequestParam(defaultValue = "STANDARD") String variantSku
    ) {
        return ResponseEntity.ok(productService.getPriceHistory(id, variantSku));
    }
}
