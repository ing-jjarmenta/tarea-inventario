package com.store.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.store.inventory.api.InventoryService;
import com.store.inventory.api.OrderLimitExceededException;
import com.store.inventory.api.ProductCategory;
import com.store.inventory.api.Reservation;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventoryServiceExtendedTest {

    private static final Instant START = Instant.parse("2024-06-01T12:00:00Z");

    private List<String> alertedSkus;
    private List<Integer> alertedAvailable;
    private InventoryService service;

    @BeforeEach
    void setUp() {
        alertedSkus = new ArrayList<>();
        alertedAvailable = new ArrayList<>();
        service = createService(Clock.fixed(START, ZoneOffset.UTC));
        service.registerProduct("SKU-1", ProductCategory.STANDARD);
    }

    @Test
    void sendsLowStockAlertWhenAvailableDropsToThreshold() {
        service.addStock("SKU-1", 6);
        service.reserve("ORDER-1", "SKU-1", 1);

        assertEquals(List.of("SKU-1"), alertedSkus);
        assertEquals(List.of(5), alertedAvailable);
    }

    @Test
    void doesNotRepeatLowStockAlertWhileStillLow() {
        service.addStock("SKU-1", 6);
        service.reserve("ORDER-1", "SKU-1", 1);
        service.reserve("ORDER-2", "SKU-1", 1);

        assertEquals(1, alertedSkus.size());
        assertEquals(4, service.available("SKU-1"));
    }

    @Test
    void sendsNewLowStockAlertAfterRestock() {
        service.addStock("SKU-1", 6);
        service.reserve("ORDER-1", "SKU-1", 1);
        service.addStock("SKU-1", 10);
        service.reserve("ORDER-2", "SKU-1", 10);

        assertEquals(List.of("SKU-1", "SKU-1"), alertedSkus);
        assertEquals(List.of(5, 5), alertedAvailable);
    }

    @Test
    void reserveIsIdempotentForSameOrder() {
        service.addStock("SKU-1", 10);

        Reservation first = service.reserve("ORDER-1", "SKU-1", 3);
        Reservation second = service.reserve("ORDER-1", "SKU-1", 3);

        assertEquals(first, second);
        assertEquals(7, service.available("SKU-1"));
    }

    @Test
    void rejectsConflictingReservationForSameOrder() {
        service.addStock("SKU-1", 10);
        service.reserve("ORDER-1", "SKU-1", 3);

        assertThrows(IllegalStateException.class, () -> service.reserve("ORDER-1", "SKU-1", 2));
    }

    @Test
    void flashSaleRejectsOrdersAboveLimit() {
        service.registerProduct("FLASH-1", ProductCategory.FLASH_SALE);
        service.addStock("FLASH-1", 10);

        assertThrows(OrderLimitExceededException.class, () -> service.reserve("ORDER-1", "FLASH-1", 3));
    }

    @Test
    void expiredReservationReleasesStock() {
        MutableClock clock = new MutableClock(START, ZoneOffset.UTC);
        InventoryService timedService = createService(clock);
        timedService.registerProduct("SKU-1", ProductCategory.STANDARD);
        timedService.addStock("SKU-1", 5);
        timedService.reserve("ORDER-1", "SKU-1", 3);

        assertEquals(2, timedService.available("SKU-1"));

        clock.advance(Duration.ofMinutes(16));
        assertEquals(5, timedService.available("SKU-1"));
    }

    @Test
    void cannotConfirmExpiredReservation() {
        MutableClock clock = new MutableClock(START, ZoneOffset.UTC);
        InventoryService timedService = createService(clock);
        timedService.registerProduct("SKU-1", ProductCategory.STANDARD);
        timedService.addStock("SKU-1", 5);
        timedService.reserve("ORDER-1", "SKU-1", 2);

        clock.advance(Duration.ofMinutes(16));

        assertThrows(IllegalStateException.class, () -> timedService.confirm("ORDER-1"));
    }

    private InventoryService createService(Clock clock) {
        return Inventory.create(clock, (sku, available) -> {
            alertedSkus.add(sku);
            alertedAvailable.add(available);
        });
    }
}
