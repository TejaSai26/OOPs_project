package com.crsms.controller;

import com.crsms.dto.ApiDtos.DecisionNote;
import com.crsms.dto.ApiDtos.ReservationRequest;
import com.crsms.dto.ApiDtos.ReservationResponse;
import com.crsms.service.CurrentUserService;
import com.crsms.service.ReservationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    private final ReservationService reservations;
    private final CurrentUserService current;

    public ReservationController(ReservationService reservations, CurrentUserService current) {
        this.reservations = reservations;
        this.current = current;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@Valid @RequestBody ReservationRequest request) {
        return reservations.request(current.require(), request);
    }

    @GetMapping("/my")
    public List<ReservationResponse> mine() {
        return reservations.mine(current.require());
    }

    @GetMapping("/incoming")
    public List<ReservationResponse> incoming() {
        return reservations.incoming(current.require());
    }

    @GetMapping("/{id}")
    public ReservationResponse one(@PathVariable Long id) {
        return reservations.get(current.require(), id);
    }

    @PostMapping("/{id}/approve")
    public ReservationResponse approve(@PathVariable Long id) {
        return reservations.approve(current.require(), id);
    }

    @PostMapping("/{id}/reject")
    public ReservationResponse reject(@PathVariable Long id, @RequestBody(required = false) DecisionNote note) {
        return reservations.reject(current.require(), id, note == null ? null : note.note());
    }

    @PostMapping("/{id}/cancel")
    public ReservationResponse cancel(@PathVariable Long id) {
        return reservations.cancel(current.require(), id);
    }
}
