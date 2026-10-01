package com.store.inventory.infrastructure;

import com.store.inventory.api.ProductCategory;
import com.store.inventory.domain.Product;
import com.store.inventory.domain.ReservationEntry;
import java.time.Instant;
import java.util.Optional;

public interface InventoryRepository {

    void registerProduct(String sku, ProductCategory category);

    void addStock(String sku, int quantity);

    void reduceStock(String sku, int quantity);

    Optional<Product> findProduct(String sku);

    boolean isRegistered(String sku);

    void saveReservation(ReservationEntry reservation);

    Optional<ReservationEntry> findReservation(String orderId);

    void removeReservation(String orderId);

    int reservedQuantityForSku(String sku);

    void expireReservationsBefore(Instant instant);
}
