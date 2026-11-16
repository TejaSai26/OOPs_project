package com.crsms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.crsms.model.enums.UnitStatus;
import com.crsms.model.enums.UserRole;
import com.crsms.model.user.Staff;
import com.crsms.repository.BorrowingRecordRepository;
import com.crsms.repository.ResourceUnitRepository;
import com.crsms.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CampusAcceptanceTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired ResourceUnitRepository units;
    @Autowired BorrowingRecordRepository borrowings;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired ReservationService reservationService;

    private TransactionTemplate transactions;

    @BeforeEach
    void ensureAdmin() {
        transactions = new TransactionTemplate(transactionManager);
        if (users.findByEmailIgnoreCase("admin-test@campus.edu").isEmpty()) {
            users.save(new Staff("admin-test@campus.edu", encoder.encode("Admin@123"),
                    "Test Admin", "Administration", "ADM-TEST", "Registry", UserRole.ADMIN));
        }
    }

    @Test
    void anonymousAndStudentCannotUseAdminApis() throws Exception {
        mvc.perform(get("/api/admin/analytics")).andExpect(status().isUnauthorized());
        register("sec-student@campus.edu", "S-SEC");
        mvc.perform(get("/api/admin/users").session(login("sec-student@campus.edu", "Student@123")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/audit").session(login("sec-student@campus.edu", "Student@123")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/penalties/1/waive").session(login("sec-student@campus.edu", "Student@123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"please\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotReserveOwnUnitOrAnOverlapOrAUnitUnderRepair() throws Exception {
        register("own-owner@campus.edu", "OWN-A");
        register("own-other@campus.edu", "OWN-B");
        long unitId = listingWithUnit("own-owner@campus.edu", "OWN-BIKE", "HOURLY", "12");
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(7200);
        String body = body(unitId, start, end, false);
        mvc.perform(post("/api/reservations").session(login("own-owner@campus.edu", "Student@123"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/reservations").session(login("own-other@campus.edu", "Student@123"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/reservations").session(login("own-other@campus.edu", "Student@123"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        long repair = listingWithUnit("own-owner@campus.edu", "REPAIR-1", "FREE", "0");
        transactions.executeWithoutResult(status -> {
            var unit = units.findById(repair).orElseThrow();
            unit.changeStatus(UnitStatus.UNDER_REPAIR);
            unit.changeCondition(com.crsms.model.enums.ResourceCondition.UNDER_MAINTENANCE);
        });
        mvc.perform(post("/api/reservations").session(login("own-other@campus.edu", "Student@123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(repair, start.plusSeconds(20000), end.plusSeconds(20000), false)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void simultaneousReservationsAllowOnlyOne() throws Exception {
        register("race-owner@campus.edu", "RACE-O");
        register("race-a@campus.edu", "RACE-A");
        register("race-b@campus.edu", "RACE-B");
        long unitId = listingWithUnit("race-owner@campus.edu", "RACE-UNIT", "DAILY", "5");
        Instant start = Instant.now().plusSeconds(5000);
        Instant end = start.plusSeconds(3600);
        var request = new com.crsms.dto.ApiDtos.ReservationRequest(unitId, start, end, false);
        var first = users.findByEmailIgnoreCase("race-a@campus.edu").orElseThrow();
        var second = users.findByEmailIgnoreCase("race-b@campus.edu").orElseThrow();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();
        for (var user : List.of(first, second)) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                if (!go.await(10, TimeUnit.SECONDS)) {
                    return 0;
                }
                try {
                    reservationService.request(user, request);
                    return 201;
                } catch (com.crsms.exception.ApiException ex) {
                    return ex.getStatus();
                }
            }));
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        go.countDown();
        List<Integer> codes = new ArrayList<>();
        for (Future<Integer> future : futures) {
            codes.add(future.get(20, TimeUnit.SECONDS));
        }
        pool.shutdown();
        assertEquals(1, codes.stream().filter(code -> code == 201).count());
        assertEquals(1, codes.stream().filter(code -> code == 409).count());
    }

    @Test
    void lateReturnCreatesPenaltyThatMustBePaidInFullAndCanBeWaived() throws Exception {
        register("late-owner@campus.edu", "LATE-O");
        register("late-user@campus.edu", "LATE-U");
        long unitId = listingWithUnit("late-owner@campus.edu", "LATE-BIKE", "HOURLY", "10");
        long borrowingId = approvedPickup("late-owner@campus.edu", "late-user@campus.edu", unitId);
        transactions.executeWithoutResult(status -> entityManager.createQuery(
                        "UPDATE BorrowingRecord b SET b.dueAt = :due WHERE b.id = :id")
                .setParameter("due", Instant.now().minusSeconds(5400))
                .setParameter("id", borrowingId)
                .executeUpdate());
        JsonNode returned = json.readTree(mvc.perform(post("/api/borrowings/" + borrowingId + "/return")
                        .session(login("late-user@campus.edu", "Student@123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"damaged\":false,\"condition\":\"GOOD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.late").value(true))
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andReturn().getResponse().getContentAsString());
        assertTrue(returned.get("penaltyAmount").decimalValue().doubleValue() > 0);
        long penaltyId = json.readTree(mvc.perform(get("/api/penalties/my")
                        .session(login("late-user@campus.edu", "Student@123")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get(0).get("id").asLong();
        mvc.perform(post("/api/penalties/" + penaltyId + "/pay")
                        .session(login("late-user@campus.edu", "Student@123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":1.00}"))
                .andExpect(status().isBadRequest());
        BigDecimal due = new BigDecimal(returned.get("penaltyAmount").asText());
        mvc.perform(post("/api/penalties/" + penaltyId + "/pay")
                        .session(login("late-user@campus.edu", "Student@123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":" + due.toPlainString() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));

        long secondBorrowing = approvedPickup("late-owner@campus.edu", "late-user@campus.edu", unitId,
                Instant.now().plusSeconds(80000));
        transactions.executeWithoutResult(status -> entityManager.createQuery(
                        "UPDATE BorrowingRecord b SET b.dueAt = :due WHERE b.id = :id")
                .setParameter("due", Instant.now().minusSeconds(7200))
                .setParameter("id", secondBorrowing)
                .executeUpdate());
        mvc.perform(post("/api/borrowings/" + secondBorrowing + "/return")
                        .session(login("late-user@campus.edu", "Student@123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"damaged\":false}"))
                .andExpect(status().isOk());
        JsonNode penalties = json.readTree(mvc.perform(get("/api/penalties/my")
                        .session(login("late-user@campus.edu", "Student@123")))
                .andReturn().getResponse().getContentAsString());
        long waiveId = 0;
        for (JsonNode node : penalties) {
            if ("UNPAID".equals(node.get("paymentStatus").asText())) {
                waiveId = node.get("id").asLong();
            }
        }
        assertTrue(waiveId > 0);
        mvc.perform(post("/api/admin/penalties/" + waiveId + "/waive")
                        .session(login("late-user@campus.edu", "Student@123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"hardship\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/penalties/" + waiveId + "/waive")
                        .session(login("admin-test@campus.edu", "Admin@123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"documented hardship\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("WAIVED"));
    }

    @Test
    void missingBorrowingDuplicateReviewNotificationsAndAudit() throws Exception {
        register("flow-owner@campus.edu", "FLOW-O");
        register("flow-user@campus.edu", "FLOW-U");
        mvc.perform(post("/api/borrowings/999999/return").session(login("flow-user@campus.edu", "Student@123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"damaged\":false}"))
                .andExpect(status().isNotFound());
        long unitId = listingWithUnit("flow-owner@campus.edu", "FLOW-1", "FREE", "0");
        long borrowingId = approvedPickup("flow-owner@campus.edu", "flow-user@campus.edu", unitId);
        mvc.perform(post("/api/borrowings/" + borrowingId + "/return").session(login("flow-user@campus.edu", "Student@123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"damaged\":false}"))
                .andExpect(status().isOk());
        String review = "{\"borrowingId\":" + borrowingId + ",\"rating\":4,\"comment\":\"works\"}";
        mvc.perform(post("/api/reviews").session(login("flow-user@campus.edu", "Student@123"))
                        .contentType(MediaType.APPLICATION_JSON).content(review))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/reviews").session(login("flow-user@campus.edu", "Student@123"))
                        .contentType(MediaType.APPLICATION_JSON).content(review))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/notifications/count").session(login("flow-owner@campus.edu", "Student@123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread").isNumber());
        mvc.perform(post("/api/notifications/read-all").session(login("flow-owner@campus.edu", "Student@123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread").value(0));
        mvc.perform(get("/api/admin/audit").session(login("admin-test@campus.edu", "Admin@123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.action == 'USER_REGISTERED')]").exists());
        mvc.perform(get("/api/admin/analytics").session(login("admin-test@campus.edu", "Admin@123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").isNumber())
                .andExpect(jsonPath("$.reservationTrend.length()").value(6));
    }

    @Test
    void waitingListPromotesWhenTheBlockingRequestIsCancelled() throws Exception {
        register("wait-owner@campus.edu", "WAIT-O");
        register("wait-a@campus.edu", "WAIT-A");
        register("wait-b@campus.edu", "WAIT-B");
        long unitId = listingWithUnit("wait-owner@campus.edu", "WAIT-1", "FREE", "0");
        Instant start = Instant.now().plusSeconds(4000);
        Instant end = start.plusSeconds(3600);
        long first = json.readTree(mvc.perform(post("/api/reservations").session(login("wait-a@campus.edu", "Student@123"))
                        .contentType(MediaType.APPLICATION_JSON).content(body(unitId, start, end, false)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        long queued = json.readTree(mvc.perform(post("/api/reservations").session(login("wait-b@campus.edu", "Student@123"))
                        .contentType(MediaType.APPLICATION_JSON).content(body(unitId, start, end, true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITLISTED"))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(post("/api/reservations/" + first + "/cancel").session(login("wait-a@campus.edu", "Student@123")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/reservations/" + queued).session(login("wait-b@campus.edu", "Student@123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    private long approvedPickup(String owner, String borrower, long unitId) throws Exception {
        return approvedPickup(owner, borrower, unitId, Instant.now().plusSeconds(3600));
    }

    private long approvedPickup(String owner, String borrower, long unitId, Instant start) throws Exception {
        Instant end = start.plusSeconds(7200);
        long reservationId = json.readTree(mvc.perform(post("/api/reservations")
                        .session(login(borrower, "Student@123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(unitId, start, end, false)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(post("/api/reservations/" + reservationId + "/approve").session(login(owner, "Student@123")))
                .andExpect(status().isOk());
        return json.readTree(mvc.perform(post("/api/borrowings/" + reservationId + "/pickup")
                        .session(login(borrower, "Student@123")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    private long listingWithUnit(String owner, String asset, String pricing, String rate) throws Exception {
        JsonNode categories = json.readTree(mvc.perform(get("/api/categories")).andReturn().getResponse().getContentAsString());
        long categoryId = categories.get(0).get("id").asLong();
        long listingId = json.readTree(mvc.perform(post("/api/listings").session(login(owner, "Student@123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + asset + " bike\",\"description\":\"Campus loaner\",\"categoryId\":"
                                + categoryId + ",\"pricingType\":\"" + pricing + "\",\"rate\":" + rate + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        return json.readTree(mvc.perform(post("/api/listings/" + listingId + "/units").session(login(owner, "Student@123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetCode\":\"" + asset + "\",\"condition\":\"GOOD\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    private void register(String email, String member) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Test User\",\"email\":\"" + email
                                + "\",\"password\":\"Student@123\",\"role\":\"STUDENT\",\"department\":\"CS\",\"memberId\":\""
                                + member + "\"}"))
                .andExpect(status().isCreated());
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String body(long unitId, Instant start, Instant end, boolean wait) {
        return "{\"unitId\":" + unitId + ",\"startAt\":\"" + start + "\",\"endAt\":\"" + end + "\",\"joinWaitlist\":" + wait + "}";
    }
}
