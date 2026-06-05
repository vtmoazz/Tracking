package com.example.tracking.module.auth.entity;


import com.example.tracking.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@Getter
@Setter
public class Cart extends BaseEntity {
    @Column(name = "user_id", length = 36, nullable = false, unique = true)
    private String userId;
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CartItem> items = new ArrayList<>();
}