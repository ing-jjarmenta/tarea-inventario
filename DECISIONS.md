# Decisiones de diseño

## Supuestos

- Cada pedido (`orderId`) reserva un único producto (`sku`), como indica el contrato.
- Reintentos de la app con el mismo `orderId`, `sku` y cantidad son idempotentes: se devuelve la misma reserva activa sin reservar stock adicional.
- Si llega un reintento con el mismo `orderId` pero distinto `sku` o cantidad, se rechaza con `IllegalStateException`.
- Las reservas expiran cuando `expiresAt <= now` (evaluación lazy en cada operación).
- El umbral de stock bajo es 5 unidades **disponibles** (stock físico menos reservas activas).
- Tras reabastecer por encima del umbral, el aviso puede volver a emitirse si el disponible baja otra vez.

## Arquitectura

- **`Inventory.create()`** es el composition root: ensambla repository, reglas y notificaciones con inyección manual (sin framework).
- **`InventoryRepository`** abstrae la persistencia; hoy `InMemoryInventoryRepository`, mañana JDBC u otro backend.
- **`CategoryRules`** concentra TTL y límites por categoría para facilitar cambios de negocio.
- **`LowStockNotifier`** encapsula la regla de no repetir alertas por SKU.

## Qué quedó fuera

- Persistencia en base de datos.
- Concurrencia distribuida entre instancias.
- Scheduler/background job para expiración (se usa expiración lazy).
- API de cancelación explícita de reservas.
- Métricas, tracing y reintentos de alertas fallidas.

## Antes de producción cambiaría

1. **Persistencia transaccional** con bloqueo optimista o pesimista por SKU.
2. **Expiración proactiva** (job o TTL en DB) además de la evaluación lazy.
3. **Idempotencia durable** con clave de idempotencia persistida y ventana de retención.
4. **Outbox/eventos** para alertas de stock bajo en lugar de callback síncrono.
5. **Configuración externa** de umbrales, TTL y límites por categoría (sin redeploy).
6. **Tests de carga/concurrencia** para validar comportamiento bajo picos de demanda.
