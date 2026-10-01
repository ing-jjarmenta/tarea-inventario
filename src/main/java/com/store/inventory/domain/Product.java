package com.store.inventory.domain;

import com.store.inventory.api.ProductCategory;

public record Product(String sku, ProductCategory category, int physicalStock) {

    public Product withStock(int newStock) {
        return new Product(sku, category, newStock);
    }
}
