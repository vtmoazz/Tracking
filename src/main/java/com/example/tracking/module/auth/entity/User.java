package com.example.tracking.module.auth.entity;

import com.example.tracking.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private Role role = Role.BUYER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", length = 10)
    private OAuthProvider oauthProvider;

    @Column(name = "oauth_provider_id", length = 255)
    private String oauthProviderId;

    public enum Role {
        BUYER, SELLER, SHIPPER, ADMIN
    }

    public enum Status {
        ACTIVE, INACTIVE, BANNED
    }

    public enum OAuthProvider {
        GOOGLE, FACEBOOK
    }
}
