package com.crsms.service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CampusFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void duplicateEmailIsRejected() throws Exception {
        register("dup@campus.edu", "STUDENT", "S-1");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("dup@campus.edu", "STUDENT", "S-2")))
                .andExpect(status().isConflict());
    }

    @Test
    void fullBorrowAndMaintenanceBlock() throws Exception {
        register("owner@campus.edu", "STUDENT", "OWN-1");
        register("borrower@campus.edu", "STUDENT", "BOR-1");
        String owner = login("owner@campus.edu");
        String borrower = login("borrower@campus.edu");
        if (owner.isBlank() || borrower.isBlank()) {
            throw new IllegalStateException("login failed");
        }
        JsonNode categories = json.readTree(mvc.perform(get("/api/categories")).andReturn().getResponse().getContentAsString());
        long bikes = 0;
        for (JsonNode c : categories) {
            if ("Bicycles".equals(c.get("name").asText())) {
                bikes = c.get("id").asLong();
            }
        }
        var listing = mvc.perform(post("/api/listings").contentType(MediaType.APPLICATION_JSON).session(loginSession("owner@campus.edu"))
                        .content("""
                                {"title":"Demo bike","description":"City bicycle","categoryId":%d,"pricingType":"HOURLY","rate":10}
                                """.formatted(bikes)))
                .andExpect(status().isCreated())
                .andReturn();
        long listingId = json.readTree(listing.getResponse().getContentAsString()).get("id").asLong();
        var unit = mvc.perform(post("/api/listings/" + listingId + "/units").session(loginSession("owner@campus.edu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetCode\":\"BIKE-TEST\",\"condition\":\"GOOD\",\"locationNote\":\"rack\"}"))
                .andExpect(status().isCreated()).andReturn();
        long unitId = json.readTree(unit.getResponse().getContentAsString()).get("id").asLong();

        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(7200);
        String body = "{\"unitId\":" + unitId + ",\"startAt\":\"" + start + "\",\"endAt\":\"" + end + "\"}";
        var reservation = mvc.perform(post("/api/reservations").session(loginSession("borrower@campus.edu"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        long reservationId = json.readTree(reservation.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/reservations/" + reservationId + "/approve").session(loginSession("borrower@campus.edu")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/borrowings/" + reservationId + "/pickup").session(loginSession("borrower@campus.edu")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/reservations/" + reservationId + "/approve").session(loginSession("owner@campus.edu")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        var borrowing = mvc.perform(post("/api/borrowings/" + reservationId + "/pickup").session(loginSession("borrower@campus.edu")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE")).andReturn();
        long borrowingId = json.readTree(borrowing.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(post("/api/borrowings/" + reservationId + "/pickup").session(loginSession("borrower@campus.edu")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/borrowings/" + borrowingId + "/return").session(loginSession("borrower@campus.edu"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"damaged\":true,\"damageNote\":\"flat tyre\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RETURNED"));
        mvc.perform(post("/api/borrowings/" + borrowingId + "/return").session(loginSession("borrower@campus.edu"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"damaged\":false}"))
                .andExpect(status().isConflict());

        var maint = mvc.perform(get("/api/maintenance").session(loginSession("borrower@campus.edu"))).andReturn();
        // admin is not seeded in tests; report already created on return. Mark via a registered admin role is not available.
        // A second reservation must fail because the unit is MAINTENANCE_REQUIRED.
        Instant start2 = end.plusSeconds(3600);
        String body2 = "{\"unitId\":" + unitId + ",\"startAt\":\"" + start2 + "\",\"endAt\":\"" + start2.plusSeconds(3600) + "\"}";
        register("other@campus.edu", "STUDENT", "OTH-1");
        mvc.perform(post("/api/reservations").session(loginSession("other@campus.edu"))
                        .contentType(MediaType.APPLICATION_JSON).content(body2))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/reviews").session(loginSession("borrower@campus.edu"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"borrowingId\":" + borrowingId + ",\"rating\":5,\"comment\":\"smooth ride\"}"))
                .andExpect(status().isCreated());
    }

    private void register(String email, String role, String member) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(userJson(email, role, member))).andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Student@123\"}")).andExpect(status().isOk());
        return email;
    }

    private MockHttpSession loginSession(String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Student@123\"}")).andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String userJson(String email, String role, String member) {
        return "{\"fullName\":\"Test User\",\"email\":\"" + email + "\",\"password\":\"Student@123\",\"role\":\""
                + role + "\",\"department\":\"CS\",\"memberId\":\"" + member + "\"}";
    }
}
