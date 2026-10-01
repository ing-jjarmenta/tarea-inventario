package com.store.inventory.application;

import com.store.inventory.api.InsufficientStockException;
import com.store.inventory.api.InventoryService;
import com.store.inventory.api.ProductCategory;
import com.store.inventory.api.Reservation;
import com.store.inventory.domain.CategoryRules;
import com.store.inventory.domain.LowStockNotifier;
import com.store.inventory.domain.Product;
import com.store.inventory.domain.ReservationEntry;
import com.store.inventory.infrastructure.InventoryRepository;
import java.time.Clock;

public final class InventoryServiceImpl implements InventoryService {

    private final Clock clock;
    private final InventoryRepository repository;
    private final CategoryRules categoryRules;
    private final LowStockNotifier lowStockNotifier;

    public InventoryServiceImpl(
            Clock clock,
            InventoryRepository repository,
            CategoryRules categoryRules,
            LowStockNotifier lowStockNotifier) {
        this.clock = clock;
        this.repository = repository;
        this.categoryRules = categoryRules;
        this.lowStockNotifier = lowStockNotifier;
    }

    @Override
    public void registerProduct(String sku, ProductCategory category) {
        repository.registerProduct(sku, category);
    }

    @Override
    public void addStock(String sku, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (!repository.isRegistered(sku)) {
            throw new IllegalArgumentException("Product not registered: " + sku);
        }
        repository.addStock(sku, quantity);
        evaluateStockAlert(sku);
    }

    @Override
    public Reservation reserve(String orderId, String sku, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        expireReservations();

        var existing = repository.findReservation(orderId);
        if (existing.isPresent()) {
            return toReservation(existing.get(), sku, quantity);
        }

        int availableUnits = computeAvailable(sku);
        if (quantity > availableUnits) {
            throw new InsufficientStockException(sku, quantity, availableUnits);
        }

        Product product = repository.findProduct(sku)
                .orElseThrow(() -> new InsufficientStockException(sku, quantity, availableUnits));
        categoryRules.checkOrderLimit(sku, product.category(), quantity);

        var expiresAt = clock.instant().plus(categoryRules.paymentWindow(product.category()));
        var reservation = new ReservationEntry(orderId, sku, quantity, expiresAt);
        repository.saveReservation(reservation);
        evaluateStockAlert(sku);
        return new Reservation(orderId, sku, quantity, expiresAt);
    }

    @Override
    public void confirm(String orderId) {
        expireReservations();

        ReservationEntry reservation = repository.findReservation(orderId)
                .orElseThrow(() -> new IllegalStateException("No active reservation for order: " + orderId));

        String sku = reservation.sku();
        repository.reduceStock(sku, reservation.quantity());
        repository.removeReservation(orderId);
        evaluateStockAlert(sku);
    }

    @Override
    public int available(String sku) {
        expireReservations();
        return computeAvailable(sku);
    }

    private Reservation toReservation(ReservationEntry reservation, String sku, int quantity) {
        if (!reservation.sku().equals(sku) || reservation.quantity() != quantity) {
            throw new IllegalStateException("Order already has a different reservation: " + reservation.orderId());
        }
        return new Reservation(
                reservation.orderId(), reservation.sku(), reservation.quantity(), reservation.expiresAt());
    }

    private void evaluateStockAlert(String sku) {
        lowStockNotifier.onStockChanged(sku, computeAvailable(sku));
    }

    private int computeAvailable(String sku) {
        return repository.findProduct(sku)
                .map(product -> product.physicalStock() - repository.reservedQuantityForSku(sku))
                .orElse(0);
    }

    private void expireReservations() {
        repository.expireReservationsBefore(clock.instant());
    }
}
