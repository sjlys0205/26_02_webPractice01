package com.example.ksl.wsd2025.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

// POST /api/v1/items/bulk 요청 body: { "items": [ {name, price}, ... ] }
public class ItemBulkCreateRequest {
    @NotEmpty(message = "items must not be empty")
    @Size(max = 100, message = "at most 100 items per request")
    private List<@Valid ItemCreateRequest> items;

    // getter / setter
    public List<ItemCreateRequest> getItems() { return items; }
    public void setItems(List<ItemCreateRequest> items) { this.items = items; }
}
