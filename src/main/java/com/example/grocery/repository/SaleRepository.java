package com.example.grocery.repository;

import com.example.grocery.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    // ===== Daily sales (group by date) - Postgres native =====
    // return rows: [date, sum]
    @Query(
            value = """
            SELECT (s.sold_at AT TIME ZONE 'UTC')::date AS d, COALESCE(SUM(s.total_price), 0) AS total
            FROM sales s
            WHERE s.sold_at >= :from
            GROUP BY d
            ORDER BY d
        """,
            nativeQuery = true
    )
    List<Object[]> sumDailySales(@Param("from") Instant from);

    // ===== Monthly sales (group by YYYY-MM) - Postgres native =====
    // return rows: [yyyy_mm, sum]
    @Query(
            value = """
            SELECT TO_CHAR(DATE_TRUNC('month', (s.sold_at AT TIME ZONE 'UTC')), 'YYYY-MM') AS ym,
                   COALESCE(SUM(s.total_price), 0) AS total
            FROM sales s
            WHERE s.sold_at >= :from
            GROUP BY ym
            ORDER BY ym
        """,
            nativeQuery = true
    )
    List<Object[]> sumMonthlySales(@Param("from") Instant from);
}