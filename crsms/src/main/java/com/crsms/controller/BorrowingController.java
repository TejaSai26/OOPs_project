package com.crsms.controller;

import com.crsms.dto.ApiDtos.BorrowingResponse;
import com.crsms.dto.ApiDtos.ReturnRequest;
import com.crsms.service.BorrowingService;
import com.crsms.service.CurrentUserService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/borrowings")
public class BorrowingController {
    private final BorrowingService borrowings;
    private final CurrentUserService current;

    public BorrowingController(BorrowingService borrowings, CurrentUserService current) {
        this.borrowings = borrowings;
        this.current = current;
    }

    @PostMapping("/{id}/pickup")
    public BorrowingResponse pickup(@PathVariable Long id) {
        return borrowings.pickup(current.require(), id);
    }

    @PostMapping("/{id}/return")
    public BorrowingResponse giveBack(@PathVariable Long id, @RequestBody(required = false) ReturnRequest request) {
        return borrowings.giveBack(current.require(), id, request);
    }

    @GetMapping("/my")
    public List<BorrowingResponse> mine() {
        return borrowings.mine(current.require());
    }

    @GetMapping("/{id}")
    public BorrowingResponse one(@PathVariable Long id) {
        return borrowings.get(current.require(), id);
    }
}
