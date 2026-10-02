package com.rental.rental_room;

import com.rental.rental_room.api.CatalogService;
import com.rental.rental_room.api.Requests;
import com.rental.rental_room.billing.InvoiceService;
import com.rental.rental_room.billing.ReadingService;
import com.rental.rental_room.common.BusinessException;
import com.rental.rental_room.common.Database;
import com.rental.rental_room.contract.Contract;
import com.rental.rental_room.contract.ContractStatus;
import com.rental.rental_room.invoice.Invoice;
import com.rental.rental_room.room.RoomRepository;
import com.rental.rental_room.water.WaterCalculationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@Testcontainers(disabledWithoutDocker = true)
class ConcurrentBillingTests {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired RoomRepository rooms;
    @Autowired CatalogService catalog;
    @Autowired com.rental.rental_room.contract.ContractService contracts;
    @Autowired ReadingService readings;
    @Autowired InvoiceService invoices;
    @Autowired Database db;

    @Test
    void concurrentActivationAndInvoiceGenerationCreateOnlyOneEach() throws Exception {
        Long roomId = rooms.findAll().getFirst().getId();
        var a = catalog.tenant(null, new Requests.TenantInput("A", null, null, null, null, null, null, null));
        var b = catalog.tenant(null, new Requests.TenantInput("B", null, null, null, null, null, null, null));
        var period = YearMonth.from(LocalDate.now(java.time.ZoneId.of("Asia/Bangkok")));
        var contractA = new Requests.ContractInput(roomId, a.id(), period.atDay(1), null, new BigDecimal("1000"),
                BigDecimal.ZERO, ContractStatus.ACTIVE, 5, null, BigDecimal.ONE,
                WaterCalculationType.PER_PERSON, BigDecimal.ONE);
        var contractB = new Requests.ContractInput(roomId, b.id(), period.atDay(1), null, new BigDecimal("1000"),
                BigDecimal.ZERO, ContractStatus.ACTIVE, 5, null, BigDecimal.ONE,
                WaterCalculationType.PER_PERSON, BigDecimal.ONE);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var first = workers.submit(() -> { start.await(); try { contracts.write(null, contractA); return true; }
                catch (BusinessException ex) { return false; } });
            var second = workers.submit(() -> { start.await(); try { contracts.write(null, contractB); return true; }
                catch (BusinessException ex) { return false; } });
            start.countDown();
            assertThat((first.get(30, TimeUnit.SECONDS) ? 1 : 0) + (second.get(30, TimeUnit.SECONDS) ? 1 : 0)).isEqualTo(1);
        }
        assertThat(db.query("select c from Contract c where c.room.id=:id and c.status=:status", Contract.class,
                "id", roomId, "status", ContractStatus.ACTIVE)).hasSize(1);
        readings.electricity(new Requests.ElectricityInput(roomId, period.getMonthValue(), period.getYear(),
                new BigDecimal("110"), new BigDecimal("100"), null));
        readings.water(new Requests.WaterInput(roomId, period.getMonthValue(), period.getYear(), null, null, 1));
        var input = new Requests.Generate(roomId, period.getMonthValue(), period.getYear(), BigDecimal.ZERO, BigDecimal.ZERO);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var first = workers.submit(() -> { start.await(); try { invoices.generate(input); return true; }
                catch (BusinessException ex) { return false; } });
            var second = workers.submit(() -> { start.await(); try { invoices.generate(input); return true; }
                catch (BusinessException ex) { return false; } });
            start.countDown();
            assertThat((first.get(30, TimeUnit.SECONDS) ? 1 : 0) + (second.get(30, TimeUnit.SECONDS) ? 1 : 0)).isEqualTo(1);
        }
        assertThat(db.query("select i from Invoice i where i.room.id=:id", Invoice.class, "id", roomId)).hasSize(1);
    }
}
