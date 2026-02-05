package com.example.minipg.domain.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.minipg.domain.Merchant;

public interface MerchantRepository extends JpaRepository<Merchant, String> {
}
