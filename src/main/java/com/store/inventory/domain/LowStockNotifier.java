package com.store.inventory.domain;

import com.store.inventory.api.StockAlertListener;
import java.util.HashSet;
import java.util.Set;

public final class LowStockNotifier {

    private final StockAlertListener listener;
    private final int threshold;
    private final Set<String> alertSentForSku = new HashSet<>();

    public LowStockNotifier(StockAlertListener listener, int threshold) {
        this.listener = listener;
        this.threshold = threshold;
    }

    public void onStockChanged(String sku, int availableUnits) {
        if (availableUnits > threshold) {
            alertSentForSku.remove(sku);
            return;
        }
        if (!alertSentForSku.contains(sku)) {
            listener.onLowStock(sku, availableUnits);
            alertSentForSku.add(sku);
        }
    }
}
