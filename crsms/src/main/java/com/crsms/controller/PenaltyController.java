package com.crsms.controller;

import com.crsms.dto.ApiDtos.PayRequest;
import com.crsms.dto.ApiDtos.PenaltyResponse;
import com.crsms.service.CurrentUserService;
import com.crsms.service.PenaltyService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/penalties")
public class PenaltyController {
    private final PenaltyService penalties;
    private final CurrentUserService current;

    public PenaltyController(PenaltyService penalties, CurrentUserService current) {
        this.penalties = penalties;
        this.current = current;
    }

    @GetMapping("/my")
    public List<PenaltyResponse> mine() {
        return penalties.mine(current.require());
    }

    @GetMapping("/balance")
    public Map<String, BigDecimal> balance() {
        return Map.of("outstanding", penalties.outstanding(current.require()));
    }

    @PostMapping("/{id}/pay")
    public PenaltyResponse pay(@PathVariable Long id, @Valid @RequestBody PayRequest request) {
        return penalties.pay(current.require(), id, request);
    }
}
