package com.store.inventory.infrastructure;

import com.store.inventory.api.ProductCategory;
import com.store.inventory.domain.Product;
import com.store.inventory.domain.ReservationEntry;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryInventoryRepository implements InventoryRepository {

    private final Map<String, Product> products = new HashMap<>();
    private final Map<String, ReservationEntry> reservationsByOrderId = new HashMap<>();

    @Override
    public void registerProduct(String sku, ProductCategory category) {
        products.putIfAbsent(sku, new Product(sku, category, 0));
    }

    @Override
    public void addStock(String sku, int quantity) {
        Product product = products.get(sku);
        products.put(sku, product.withStock(product.physicalStock() + quantity));
    }

    @Override
    public void reduceStock(String sku, int quantity) {
        Product product = products.get(sku);
        products.put(sku, product.withStock(product.physicalStock() - quantity));
    }

    @Override
    public Optional<Product> findProduct(String sku) {
        return Optional.ofNullable(products.get(sku));
    }

    @Override
    public boolean isRegistered(String sku) {
        return products.containsKey(sku);
    }

    @Override
    public void saveReservation(ReservationEntry reservation) {
        reservationsByOrderId.put(reservation.orderId(), reservation);
    }

    @Override
    public Optional<ReservationEntry> findReservation(String orderId) {
        return Optional.ofNullable(reservationsByOrderId.get(orderId));
    }

    @Override
    public void removeReservation(String orderId) {
        reservationsByOrderId.remove(orderId);
    }

    @Override
    public int reservedQuantityForSku(String sku) {
        return reservationsByOrderId.values().stream()
                .filter(reservation -> reservation.sku().equals(sku))
                .mapToInt(ReservationEntry::quantity)
                .sum();
    }

    @Override
    public void expireReservationsBefore(Instant instant) {
        reservationsByOrderId.entrySet().removeIf(
                entry -> !entry.getValue().expiresAt().isAfter(instant));
    }
}
