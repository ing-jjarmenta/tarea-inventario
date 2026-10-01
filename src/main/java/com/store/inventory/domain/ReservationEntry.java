package com.store.inventory.domain;

import java.time.Instant;

public record ReservationEntry(String orderId, String sku, int quantity, Instant expiresAt) {
}
