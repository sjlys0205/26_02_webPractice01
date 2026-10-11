package com.example.ksl.wsd2025.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// PUT /api/v1/items/{id}/price 요청 body
public class ItemPriceUpdateRequest {
    @NotNull(message = "price is required")
    @PositiveOrZero(message = "price must be 0 or greater")
    private Integer price;

    // getter / setter
    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }
}
