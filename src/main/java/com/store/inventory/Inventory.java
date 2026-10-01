package com.store.inventory;

import com.store.inventory.api.InventoryService;
import com.store.inventory.api.StockAlertListener;
import com.store.inventory.application.InventoryServiceImpl;
import com.store.inventory.domain.CategoryRules;
import com.store.inventory.domain.LowStockNotifier;
import com.store.inventory.infrastructure.InMemoryInventoryRepository;
import java.time.Clock;

/**
 * Entry point used by our automated tests. Keep this signature exactly as it is,
 * and build your implementation here.
 */
public final class Inventory {

    private Inventory() {
    }

    public static InventoryService create(Clock clock, StockAlertListener alertListener) {
        var repository = new InMemoryInventoryRepository();
        var categoryRules = new CategoryRules();
        var lowStockNotifier = new LowStockNotifier(alertListener, 5);
        return new InventoryServiceImpl(clock, repository, categoryRules, lowStockNotifier);
    }
}
