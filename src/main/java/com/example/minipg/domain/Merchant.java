package com.example.minipg.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "merchants")
public class Merchant extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private boolean active = true;

    public static Merchant create(String name, String email) {
        Merchant merchant = new Merchant();
        merchant.name = name;
        merchant.email = email;
        merchant.active = true;
        return merchant;
    }
}
