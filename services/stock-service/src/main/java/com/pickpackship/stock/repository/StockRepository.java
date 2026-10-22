package com.pickpackship.stock.repository;

import com.pickpackship.stock.domain.StockItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StockRepository extends JpaRepository<StockItem, UUID> {
}
