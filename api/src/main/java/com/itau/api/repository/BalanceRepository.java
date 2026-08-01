package com.itau.api.repository;

import com.itau.api.model.Balance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BalanceRepository extends JpaRepository<Balance, UUID> {
}
