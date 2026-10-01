package com.store.inventory.domain;

import com.store.inventory.api.OrderLimitExceededException;
import com.store.inventory.api.ProductCategory;
import java.time.Duration;

public final class CategoryRules {

    public Duration paymentWindow(ProductCategory category) {
        return switch (category) {
            case STANDARD -> Duration.ofMinutes(15);
            case PRE_ORDER -> Duration.ofHours(24);
            case FLASH_SALE -> Duration.ofMinutes(5);
        };
    }

    public void checkOrderLimit(String sku, ProductCategory category, int quantity) {
        if (category == ProductCategory.FLASH_SALE && quantity > 2) {
            throw new OrderLimitExceededException(sku, quantity, 2);
        }
    }
}
