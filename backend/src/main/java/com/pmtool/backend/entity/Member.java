package com.pmtool.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "cognito_sub", unique = true, length = 255)
    private String cognitoSub;

    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    protected Member() {
    }

    public Member(String name, String email, String cognitoSub) {
        this.name = name;
        this.email = email;
        this.cognitoSub = cognitoSub;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getCognitoSub() {
        return cognitoSub;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}