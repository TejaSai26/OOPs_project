package com.crsms.model.user;

import com.crsms.model.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "faculty")
public class Faculty extends User {

    @Column(name = "employee_number", nullable = false, unique = true, length = 40)
    private String employeeNumber;

    @Column(length = 80)
    private String designation;

    protected Faculty() {
    }

    public Faculty(String email, String passwordHash, String fullName, String department,
                   String employeeNumber, String designation) {
        super(email, passwordHash, fullName, department, UserRole.FACULTY);
        this.employeeNumber = employeeNumber;
        this.designation = designation;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public String getDesignation() {
        return designation;
    }

    @Override
    public String memberLabel() {
        return "Faculty " + employeeNumber;
    }
}
