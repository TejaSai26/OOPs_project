package com.crsms.model.user;

import com.crsms.model.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "staff")
public class Staff extends User {

    @Column(name = "employee_number", nullable = false, unique = true, length = 40)
    private String employeeNumber;

    @Column(length = 80)
    private String office;

    protected Staff() {
    }

    public Staff(String email, String passwordHash, String fullName, String department,
                 String employeeNumber, String office, UserRole role) {
        super(email, passwordHash, fullName, department, role);
        if (role != UserRole.STAFF && role != UserRole.ADMIN) {
            throw new IllegalArgumentException("Staff row requires STAFF or ADMIN role");
        }
        this.employeeNumber = employeeNumber;
        this.office = office;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public String getOffice() {
        return office;
    }

    @Override
    public String memberLabel() {
        return getRole() == UserRole.ADMIN ? "Administrator" : "Staff " + employeeNumber;
    }
}
