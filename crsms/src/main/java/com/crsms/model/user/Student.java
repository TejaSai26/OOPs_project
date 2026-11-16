package com.crsms.model.user;

import com.crsms.model.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "students")
public class Student extends User {

    @Column(name = "student_number", nullable = false, unique = true, length = 40)
    private String studentNumber;

    protected Student() {
    }

    public Student(String email, String passwordHash, String fullName, String department, String studentNumber) {
        super(email, passwordHash, fullName, department, UserRole.STUDENT);
        this.studentNumber = studentNumber;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    @Override
    public String memberLabel() {
        return "Student " + studentNumber;
    }
}
