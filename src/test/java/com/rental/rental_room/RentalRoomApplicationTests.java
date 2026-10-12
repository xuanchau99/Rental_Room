package com.rental.rental_room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rental.rental_room.api.*;
import com.rental.rental_room.auth.AuthService;
import com.rental.rental_room.auth.UserAdminService;
import com.rental.rental_room.billing.*;
import com.rental.rental_room.common.BusinessException;
import com.rental.rental_room.contract.ContractStatus;
import com.rental.rental_room.payment.PaymentMethod;
import com.rental.rental_room.room.RoomRepository;
import com.rental.rental_room.service.CalculationType;
import com.rental.rental_room.service.ServiceUsageController;
import com.rental.rental_room.water.WaterCalculationType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
      "app.bank.bin=970436",
      "app.bank.account=123456789",
      "app.bank.name=NGUYEN VAN A"
    })
@ActiveProfiles("local")
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureMockMvc
class RentalRoomApplicationTests {
  @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired RoomRepository rooms;

  @Autowired CatalogService catalog;
  @Autowired com.rental.rental_room.contract.ContractService contracts;
  @Autowired ReadingService readings;
  @Autowired InvoiceService invoices;
  @Autowired MonthlyInvoiceService monthly;
  @Autowired InvoiceExportController exports;
  @Autowired ServiceUsageController serviceUsages;
  @Autowired PaymentQrController qr;
  @Autowired com.rental.rental_room.dashboard.DashboardService dashboard;
  @Autowired AuthService auth;
  @Autowired UserAdminService users;
  @Autowired MockMvc mvc;
  @Autowired EntityManagerFactory emf;
  @Autowired EntityManager em;

  @Test
  void migrationsAndMappingsLoadNineRooms() {
    assertThat(rooms.findAll())
        .hasSize(9)
        .allSatisfy(
            room -> {
              assertThat(room.getAreaM2()).isEqualByComparingTo("20.00");
              assertThat(room.isHasMezzanine()).isTrue();
              assertThat(room.isHasAirConditioner()).isTrue();
            });
    assertThat(catalog.services(0, 100))
        .anySatisfy(
            service -> {
              assertThat(service.name()).isEqualTo("Wi-Fi");
              assertThat(service.calculationType()).isEqualTo(CalculationType.FIXED);
              assertThat(service.active()).isTrue();
            });
  }

  @Test
  @Transactional
  void monthlyInvoiceAndPartialPaymentFollowBusinessRules() throws Exception {
    var room = rooms.findAll().getFirst();
    var period = YearMonth.from(LocalDate.now(ZoneId.of("Asia/Bangkok")));
    var tenant =
        catalog.tenant(
            null,
            new Requests.TenantInput(
                "Nguyen Van A", "0900000000", "000000001", null, null, null, null, null));
    var wifi =
        catalog.services(0, 100).stream()
            .filter(service -> service.name().equals("Wi-Fi"))
            .findFirst()
            .orElseThrow();
    var service2 =
        catalog.service(
            null,
            new Requests.ServiceInput(
                "Trash", CalculationType.FIXED, new BigDecimal("30000"), true));
    var contract =
        contracts.write(
            null,
            new Requests.ContractInput(
                room.getId(),
                tenant.id(),
                period.atDay(1),
                null,
                new BigDecimal("3000000"),
                new BigDecimal("3000000"),
                ContractStatus.ACTIVE,
                5,
                null,
                new BigDecimal("3500"),
                WaterCalculationType.PER_PERSON,
                new BigDecimal("100000"),
                List.of(
                    new Requests.Subscription(wifi.id(), new BigDecimal("100000"), BigDecimal.ONE),
                    new Requests.Subscription(service2.id(), service2.price(), BigDecimal.ONE))));
    assertThat(
            contracts.subscriptions(
                contract.id(),
                List.of(
                    new Requests.Subscription(wifi.id(), new BigDecimal("120000"), BigDecimal.ONE),
                    new Requests.Subscription(service2.id(), service2.price(), BigDecimal.ONE))))
        .filteredOn(item -> item.serviceId().equals(wifi.id()))
        .singleElement()
        .satisfies(item -> assertThat(item.price()).isEqualByComparingTo("120000"));
    contracts.subscriptions(
        contract.id(),
        List.of(
            new Requests.Subscription(wifi.id(), new BigDecimal("100000"), BigDecimal.ONE),
            new Requests.Subscription(service2.id(), service2.price(), BigDecimal.ONE)));
    assertThatThrownBy(
            () ->
                contracts.write(
                    null,
                    new Requests.ContractInput(
                        room.getId(),
                        tenant.id(),
                        period.atDay(1),
                        null,
                        new BigDecimal("3000000"),
                        BigDecimal.ZERO,
                        ContractStatus.ACTIVE,
                        5,
                        null,
                        BigDecimal.ZERO,
                        WaterCalculationType.PER_PERSON,
                        BigDecimal.ZERO)))
        .isInstanceOf(BusinessException.class);
    var electricity =
        readings.electricity(
            new Requests.ElectricityInput(
                room.getId(),
                period.getMonthValue(),
                period.getYear(),
                new BigDecimal("1370"),
                new BigDecimal("1250"),
                null));
    assertThat(electricity.usage()).isEqualByComparingTo("120");
    assertThat(electricity.amount()).isEqualByComparingTo("420000");
    readings.water(
        new Requests.WaterInput(
            room.getId(), period.getMonthValue(), period.getYear(), null, null, 2));
    assertThatThrownBy(
            () ->
                readings.electricity(
                    new Requests.ElectricityInput(
                        room.getId(),
                        period.getMonthValue(),
                        period.getYear(),
                        new BigDecimal("1380"),
                        new BigDecimal("1370"),
                        null)))
        .isInstanceOf(BusinessException.class);
    var invoice =
        invoices.generate(
            new Requests.Generate(
                room.getId(),
                period.getMonthValue(),
                period.getYear(),
                BigDecimal.ZERO,
                BigDecimal.ZERO));
    assertThat(invoice.totalAmount()).isEqualByComparingTo("3750000");
    assertThat(invoice.serviceAmount()).isEqualByComparingTo("130000");
    assertThat(invoice.items())
        .filteredOn(item -> item.description().equals("Wi-Fi"))
        .singleElement()
        .satisfies(item -> assertThat(item.amount()).isEqualByComparingTo("100000"));
    assertThat(invoice.items()).hasSize(5);
    assertThat(exports.pdf(invoice.id()).getBody())
        .startsWith("%PDF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    assertThat(exports.excel(period.getMonthValue(), period.getYear(), null).getBody())
        .startsWith(new byte[] {'P', 'K'});
    assertThat(qr.qr(invoice.id())).startsWith(new byte[] {(byte) 0x89, 'P', 'N', 'G'});
    assertThatThrownBy(
            () ->
                invoices.generate(
                    new Requests.Generate(
                        room.getId(),
                        period.getMonthValue(),
                        period.getYear(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO)))
        .isInstanceOf(BusinessException.class);
    assertThat(
            monthly
                .generate(new Requests.Monthly(null, period.getMonthValue(), period.getYear()))
                .existing())
        .contains(room.getId());
    var paidAt = Instant.now();
    invoices.pay(
        invoice.id(),
        new Requests.Pay(new BigDecimal("1000000"), PaymentMethod.CASH, paidAt, "first", null));
    assertThat(invoices.get(invoice.id()).outstandingAmount()).isEqualByComparingTo("2750000");
    assertThatThrownBy(
            () ->
                invoices.pay(
                    invoice.id(),
                    new Requests.Pay(
                        new BigDecimal("2750001"), PaymentMethod.CASH, paidAt, "too-much", null)))
        .isInstanceOf(BusinessException.class);
    invoices.pay(
        invoice.id(),
        new Requests.Pay(
            new BigDecimal("2750000"), PaymentMethod.BANK_TRANSFER, paidAt, "last", null));
    assertThat(invoices.get(invoice.id()).status())
        .isEqualTo(com.rental.rental_room.invoice.InvoiceStatus.PAID);
    assertThat(dashboard.summary(period.getMonthValue(), period.getYear(), null).paid())
        .isEqualByComparingTo("3750000");
  }

  @Test
  @Transactional
  void invoiceChargesMultipleMeteredServicesOnce() {
    var room = rooms.findAll().get(3);
    var period = YearMonth.from(LocalDate.now(ZoneId.of("Asia/Bangkok")));
    var tenant =
        catalog.tenant(
            null,
            new Requests.TenantInput(
                "Metered Service Tenant", null, null, null, null, null, null, null));
    var parking =
        catalog.service(
            null,
            new Requests.ServiceInput(
                "Parking power", CalculationType.METER, new BigDecimal("2500"), true));
    var laundry =
        catalog.service(
            null,
            new Requests.ServiceInput(
                "Laundry", CalculationType.METER, new BigDecimal("5000"), true));
    var contract =
        contracts.write(
            null,
            new Requests.ContractInput(
                room.getId(),
                tenant.id(),
                period.atDay(1),
                null,
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                ContractStatus.ACTIVE,
                31,
                null,
                BigDecimal.ONE,
                WaterCalculationType.PER_PERSON,
                BigDecimal.ONE,
                List.of(
                    new Requests.Subscription(parking.id(), parking.price(), BigDecimal.ONE),
                    new Requests.Subscription(laundry.id(), laundry.price(), BigDecimal.ONE))));
    readings.electricity(
        new Requests.ElectricityInput(
            room.getId(),
            period.getMonthValue(),
            period.getYear(),
            BigDecimal.ONE,
            BigDecimal.ZERO,
            null));
    readings.water(
        new Requests.WaterInput(
            room.getId(), period.getMonthValue(), period.getYear(), null, null, 1));
    serviceUsages.create(
        new ServiceUsageController.Input(
            contract.id(),
            parking.id(),
            period.getMonthValue(),
            period.getYear(),
            new BigDecimal("3")));
    serviceUsages.create(
        new ServiceUsageController.Input(
            contract.id(),
            laundry.id(),
            period.getMonthValue(),
            period.getYear(),
            new BigDecimal("2")));
    var invoice =
        invoices.generate(
            new Requests.Generate(
                room.getId(),
                period.getMonthValue(),
                period.getYear(),
                BigDecimal.ZERO,
                BigDecimal.ZERO));
    assertThat(invoice.serviceAmount()).isEqualByComparingTo("17500");
    assertThat(invoice.items())
        .filteredOn(
            item ->
                item.description().equals("Parking power") || item.description().equals("Laundry"))
        .hasSize(2);
    assertThat(
            invoice.items().stream()
                .map(Views.ItemView::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
        .isEqualByComparingTo(invoice.totalAmount());
  }

  @Test
  @Transactional
  void tenantContactFieldsRejectMalformedInput() throws Exception {
    var access = auth.login("admin", "admin").accessToken();
    mvc.perform(
            post("/api/v1/tenants")
                .header("Authorization", "Bearer " + access)
                .contentType("application/json")
                .content("{\"fullName\":\"Test\",\"phone\":\"abc\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("phone")));
    mvc.perform(
            post("/api/v1/tenants")
                .header("Authorization", "Bearer " + access)
                .contentType("application/json")
                .content("{\"fullName\":\"Test\",\"citizenId\":\"123\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("citizenId")));
    var tenant =
        catalog.tenant(
            null,
            new Requests.TenantInput(
                "Test", "0987654321", "012345678901", null, null, null, null, null));
    mvc.perform(
            put("/api/v1/tenants/{id}", tenant.id())
                .header("Authorization", "Bearer " + access)
                .contentType("application/json")
                .content("{\"fullName\":\"Test\",\"phone\":\"098765432\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("phone")));
    assertThat(catalog.tenant(tenant.id()).phone()).isEqualTo("0987654321");
  }

  @Test
  @Transactional
  void jwtRotationAndApiAuthorizationWork() throws Exception {
    mvc.perform(get("/")).andExpect(status().isOk());
    mvc.perform(get("/app.css")).andExpect(status().isOk());
    mvc.perform(get("/app.js")).andExpect(status().isOk());
    mvc.perform(get("/extra.js")).andExpect(status().isOk());
    mvc.perform(get("/api/v1/rooms")).andExpect(status().isUnauthorized());
    var tokens = auth.login("admin", "admin");
    mvc.perform(get("/api/v1/rooms").header("Authorization", "Bearer " + tokens.accessToken()))
        .andExpect(status().isOk());
    mvc.perform(
            get("/api/v1/invoices/export.xlsx")
                .param("month", "13")
                .param("year", "2026")
                .header("Authorization", "Bearer " + tokens.accessToken()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("month must be between 1 and 12"));
    mvc.perform(
            post("/api/v1/invoices/999/payments")
                .header("Authorization", "Bearer " + tokens.accessToken())
                .contentType("application/json")
                .content(
                    "{\"amount\":1000,\"paymentMethod\":\"TRANSFER\",\"paymentDate\":\"2026-01-01T00:00:00Z\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail")
                .value("paymentMethod must be one of [CASH, BANK_TRANSFER, OTHER]"));
    var rotated = auth.refresh(tokens.refreshToken());
    assertThatThrownBy(() -> auth.refresh(tokens.refreshToken()))
        .isInstanceOf(BusinessException.class);
    auth.logout(rotated.refreshToken());
    assertThatThrownBy(() -> auth.refresh(rotated.refreshToken()))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  @Transactional
  void ownerCreatesStaffAndStaffChangesPassword() throws Exception {
    var owner = auth.login("admin", "admin");
    mvc.perform(
            post("/api/v1/users")
                .header("Authorization", "Bearer " + owner.accessToken())
                .contentType("application/json")
                .content(
                    "{\"username\":\"staff_demo\",\"password\":\"staff_password_2026\",\"role\":\"STAFF\"}"))
        .andExpect(status().isCreated());
    assertThat(users.list())
        .anySatisfy(user -> assertThat(user.username()).isEqualTo("staff_demo"));

    var staff = auth.login("staff_demo", "staff_password_2026");
    mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + staff.accessToken()))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/auth/change-password")
                .header("Authorization", "Bearer " + staff.accessToken())
                .contentType("application/json")
                .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"admin\"}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/v1/auth/change-password")
                .header("Authorization", "Bearer " + staff.accessToken())
                .contentType("application/json")
                .content("{\"currentPassword\":\"staff_password_2026\",\"newPassword\":\"admin\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty());
    assertThatThrownBy(() -> auth.login("staff_demo", "staff_password_2026"))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> auth.refresh(staff.refreshToken()))
        .isInstanceOf(BusinessException.class);
    assertThat(auth.login("staff_demo", "admin").accessToken()).isNotBlank();
  }

  @Test
  @Transactional
  void ownerCanCorrectAnUnbilledElectricityReadingWithHistory() throws Exception {
    var room = rooms.findAll().get(2);
    var period = YearMonth.from(LocalDate.now(ZoneId.of("Asia/Bangkok")));
    var tenant =
        catalog.tenant(
            null,
            new Requests.TenantInput(
                "Correction Tenant", null, null, null, null, null, null, null));
    contracts.write(
        null,
        new Requests.ContractInput(
            room.getId(),
            tenant.id(),
            period.atDay(1),
            null,
            new BigDecimal("1000"),
            BigDecimal.ZERO,
            ContractStatus.ACTIVE,
            5,
            null,
            new BigDecimal("2"),
            WaterCalculationType.PER_PERSON,
            BigDecimal.ONE));
    var reading =
        readings.electricity(
            new Requests.ElectricityInput(
                room.getId(),
                period.getMonthValue(),
                period.getYear(),
                new BigDecimal("110"),
                new BigDecimal("100"),
                null));
    var access = auth.login("admin", "admin").accessToken();
    mvc.perform(
            put("/api/v1/electricity-readings/{id}", reading.id())
                .header("Authorization", "Bearer " + access)
                .contentType("application/json")
                .content("{\"currentReading\":120,\"reason\":\"Verified against meter photo\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.usage").value(20));
    mvc.perform(
            get("/api/v1/electricity-readings/{id}/history", reading.id())
                .header("Authorization", "Bearer " + access))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].reason").value("Verified against meter photo"));
  }

  @Test
  @Transactional
  void meteredWaterDiscountAndOverdueItemTotalsMatch() {
    var room = rooms.findAll().get(1);
    var period = YearMonth.from(LocalDate.now(ZoneId.of("Asia/Bangkok"))).minusMonths(1);
    var tenant =
        catalog.tenant(
            null,
            new Requests.TenantInput(
                "Water Meter Tenant", null, null, null, null, null, null, null));
    contracts.write(
        null,
        new Requests.ContractInput(
            room.getId(),
            tenant.id(),
            period.atDay(1),
            null,
            new BigDecimal("3000000"),
            BigDecimal.ZERO,
            ContractStatus.ACTIVE,
            5,
            null,
            new BigDecimal("3500"),
            WaterCalculationType.METER,
            new BigDecimal("10000")));
    readings.electricity(
        new Requests.ElectricityInput(
            room.getId(),
            period.getMonthValue(),
            period.getYear(),
            new BigDecimal("1370"),
            new BigDecimal("1250"),
            null));
    var water =
        readings.water(
            new Requests.WaterInput(
                room.getId(),
                period.getMonthValue(),
                period.getYear(),
                new BigDecimal("7"),
                new BigDecimal("5"),
                null));
    assertThat(water.usage()).isEqualByComparingTo("2");
    assertThat(water.amount()).isEqualByComparingTo("20000");
    var invoice =
        invoices.generate(
            new Requests.Generate(
                room.getId(),
                period.getMonthValue(),
                period.getYear(),
                BigDecimal.ZERO,
                new BigDecimal("100")));
    assertThat(invoice.totalAmount()).isEqualByComparingTo("3439900");
    assertThat(invoice.status()).isEqualTo(com.rental.rental_room.invoice.InvoiceStatus.OVERDUE);
    assertThat(
            invoice.items().stream()
                .map(Views.ItemView::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
        .isEqualByComparingTo(invoice.totalAmount());
    // A newer unpaid invoice must not hide this overdue invoice during pagination.
    var other = rooms.findAll().getFirst();
    var current = period.plusMonths(1);
    var otherTenant =
        catalog.tenant(
            null,
            new Requests.TenantInput("Current Tenant", null, null, null, null, null, null, null));
    contracts.write(
        null,
        new Requests.ContractInput(
            other.getId(),
            otherTenant.id(),
            current.atDay(1),
            null,
            new BigDecimal("1000"),
            BigDecimal.ZERO,
            ContractStatus.ACTIVE,
            31,
            null,
            BigDecimal.ONE,
            WaterCalculationType.PER_PERSON,
            BigDecimal.ONE));
    readings.electricity(
        new Requests.ElectricityInput(
            other.getId(),
            current.getMonthValue(),
            current.getYear(),
            BigDecimal.TEN,
            BigDecimal.ZERO,
            null));
    readings.water(
        new Requests.WaterInput(
            other.getId(), current.getMonthValue(), current.getYear(), null, null, 1));
    invoices.generate(
        new Requests.Generate(
            other.getId(),
            current.getMonthValue(),
            current.getYear(),
            BigDecimal.ZERO,
            BigDecimal.ZERO));
    assertThat(
            invoices.list(
                null, null, null, com.rental.rental_room.invoice.InvoiceStatus.OVERDUE, 0, 1))
        .extracting(Views.InvoiceView::id)
        .containsExactly(invoice.id());
    em.flush();
    em.clear();
    var statistics = emf.unwrap(SessionFactory.class).getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();
    assertThat(contracts.list(null, null, 0, 20)).hasSize(2);
    assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    statistics.clear();
    assertThat(readings.electricity(null, null, null, 0, 20)).hasSize(2);
    assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    statistics.clear();
    assertThat(invoices.list(null, null, null, null, 0, 20)).hasSize(2);
    assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    statistics.clear();
    assertThat(catalog.tenants(0, 20)).hasSize(2);
    assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    statistics.clear();
    assertThat(readings.water(null, null, null, 0, 20)).hasSize(2);
    assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    statistics.clear();
    assertThat(contracts.occupants(room.getId())).hasSize(1);
    assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    statistics.clear();
    assertThat(dashboard.summary(current.getMonthValue(), current.getYear(), null).totalRooms())
        .isEqualTo(9);
    assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(5);
  }

  @Test
  @Transactional(readOnly = true)
  void roomListDoesNotFetchOnePropertyPerRoom() {
    var statistics = emf.unwrap(SessionFactory.class).getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();
    assertThat(catalog.rooms(null, null, 0, 20)).hasSize(9);
    assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
  }
}
