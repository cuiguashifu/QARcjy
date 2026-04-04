package com.qar.securitysystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "person_records")
public class PersonRecordEntity {
    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "person_no", length = 64, nullable = false, unique = true)
    private String personNo;

    @Column(name = "full_name", length = 80, nullable = false)
    private String fullName;

    @Column(name = "id_last4", length = 8, nullable = false)
    private String idLast4;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "department", length = 120)
    private String department;

    @Column(name = "airline", length = 120)
    private String airline;

    @Column(name = "position_title", length = 120)
    private String positionTitle;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPersonNo() {
        return personNo;
    }

    public void setPersonNo(String personNo) {
        this.personNo = personNo;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getIdLast4() {
        return idLast4;
    }

    public void setIdLast4(String idLast4) {
        this.idLast4 = idLast4;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getAirline() {
        return airline;
    }

    public void setAirline(String airline) {
        this.airline = airline;
    }

    public String getPositionTitle() {
        return positionTitle;
    }

    public void setPositionTitle(String positionTitle) {
        this.positionTitle = positionTitle;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
