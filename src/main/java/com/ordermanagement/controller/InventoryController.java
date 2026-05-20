package com.ordermanagement.controller;

import com.ordermanagement.dto.response.ApiResponse;
import com.ordermanagement.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory Management", description = "APIs for managing inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/{productId}/add")
    @Operation(summary = "Add stock to a product")
    public ResponseEntity<ApiResponse<Void>> addStock(
            @PathVariable UUID productId,
            @RequestParam int quantity,
            @RequestParam(required = false, defaultValue = "Manual stock addition") String note) {
        inventoryService.addStock(productId, quantity, note);
        return ResponseEntity.ok(ApiResponse.success("Stock added successfully", null));
    }

    @PostMapping("/{productId}/adjust")
    @Operation(summary = "Adjust stock for a product")
    public ResponseEntity<ApiResponse<Void>> adjustStock(
            @PathVariable UUID productId,
            @RequestParam int newQuantity,
            @RequestParam(required = false, defaultValue = "Manual stock adjustment") String note) {
        inventoryService.adjustStock(productId, newQuantity, note);
        return ResponseEntity.ok(ApiResponse.success("Stock adjusted successfully", null));
    }
}
