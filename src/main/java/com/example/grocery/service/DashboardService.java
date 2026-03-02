package com.example.grocery.service;

import com.example.grocery.entity.AuditLog;
import com.example.grocery.entity.Product;
import com.example.grocery.entity.Sale;
import com.example.grocery.entity.SaleItem;
import com.example.grocery.entity.User;
import com.example.grocery.repository.AuditLogRepository;
import com.example.grocery.repository.ProductRepository;
import com.example.grocery.repository.SaleRepository;
import com.example.grocery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    private static final int LOW_STOCK_LIMIT = 5;
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Bangkok");

    // =========================
    // SUMMARY
    // =========================
    public Map<String, Object> summary(Long userId) {
        requireOwner(userId); // ✅ ตอนนี้ OWNER/ADMIN เข้าได้

        List<Sale> sales = saleRepository.findAll();

        Map<Long, Product> productMap = loadProductMap();
        Set<Long> existingProductIds = productMap.keySet();

        LocalDate today = LocalDate.now(APP_ZONE);

        BigDecimal todaySales = BigDecimal.ZERO;
        BigDecimal monthSales = BigDecimal.ZERO;
        BigDecimal lastMonthSales = BigDecimal.ZERO;

        Set<LocalDate> soldDaysThisMonth = new HashSet<>();
        LocalDate lastMonthDate = today.minusMonths(1);

        for (Sale s : sales) {
            LocalDate d = toLocalDate(s.getSoldAt());

            BigDecimal saleTotal = saleTotalOnlyExistingProducts(s, existingProductIds);

            if (d.isEqual(today)) {
                todaySales = todaySales.add(saleTotal);
            }

            if (d.getYear() == today.getYear() && d.getMonth() == today.getMonth()) {
                if (saleTotal.compareTo(BigDecimal.ZERO) > 0) {
                    monthSales = monthSales.add(saleTotal);
                    soldDaysThisMonth.add(d);
                }
            }

            if (d.getYear() == lastMonthDate.getYear() && d.getMonth() == lastMonthDate.getMonth()) {
                if (saleTotal.compareTo(BigDecimal.ZERO) > 0) {
                    lastMonthSales = lastMonthSales.add(saleTotal);
                }
            }
        }

        BigDecimal monthGrowthPercent = null;
        if (lastMonthSales.compareTo(BigDecimal.ZERO) > 0) {
            monthGrowthPercent = monthSales
                    .subtract(lastMonthSales)
                    .divide(lastMonthSales, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        BigDecimal avgDailySales = BigDecimal.ZERO;
        if (!soldDaysThisMonth.isEmpty()) {
            avgDailySales = monthSales.divide(
                    BigDecimal.valueOf(soldDaysThisMonth.size()),
                    2,
                    RoundingMode.HALF_UP
            );
        }

        long lowStockCount = productMap.values().stream()
                .filter(p -> p.getStockQty() != null && p.getStockQty() <= LOW_STOCK_LIMIT)
                .count();

        Map<String, Object> out = new HashMap<>();
        out.put("todaySales", todaySales);
        out.put("monthSales", monthSales);
        out.put("lastMonthSales", lastMonthSales);
        out.put("monthGrowthPercent", monthGrowthPercent);
        out.put("avgDailySales", avgDailySales);
        out.put("totalProducts", productMap.size());
        out.put("lowStockProducts", lowStockCount);
        return out;
    }

    // =========================
    // LOW STOCK
    // =========================
    public List<Product> lowStockProducts(Long userId) {
        requireOwner(userId); // ✅ ตอนนี้ OWNER/ADMIN เข้าได้
        return productRepository.findAll().stream()
                .filter(p -> p.getStockQty() != null && p.getStockQty() <= LOW_STOCK_LIMIT)
                .toList();
    }

    // =========================
    // TOP SELLING (เดือนนี้) by qty
    // =========================
    public List<Map<String, Object>> topSellingProducts(Long userId) {
        requireOwner(userId); // ✅ ตอนนี้ OWNER/ADMIN เข้าได้

        Map<Long, Product> productMap = loadProductMap();
        Set<Long> existingProductIds = productMap.keySet();

        LocalDate now = LocalDate.now(APP_ZONE);
        Map<Long, Integer> qtyMap = new HashMap<>();

        for (Sale sale : saleRepository.findAll()) {
            LocalDate saleDate = toLocalDate(sale.getSoldAt());
            if (saleDate.getYear() != now.getYear() || saleDate.getMonth() != now.getMonth()) continue;
            if (sale.getItems() == null) continue;

            for (SaleItem item : sale.getItems()) {
                if (item == null || item.getProductId() == null) continue;

                Long pid = item.getProductId();
                if (!existingProductIds.contains(pid)) continue;

                int q = (item.getQty() == null) ? 0 : item.getQty();
                qtyMap.merge(pid, q, Integer::sum);
            }
        }

        if (qtyMap.isEmpty()) return List.of();

        List<Map<String, Object>> result = new ArrayList<>();
        qtyMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> {
                    Long pid = e.getKey();
                    Product p = productMap.get(pid);
                    if (p == null) return;

                    Map<String, Object> m = new HashMap<>();
                    m.put("productId", pid);
                    m.put("name", p.getName());
                    m.put("qty", e.getValue());
                    result.add(m);
                });

        return result;
    }

    // =========================
    // AUDIT LOGS
    // =========================
    public List<Map<String, Object>> recentAuditLogs(Long userId) {
        requireOwner(userId); // ✅ ตอนนี้ OWNER/ADMIN เข้าได้

        List<AuditLog> logs = auditLogRepository.findTop50ByOrderByCreatedAtDesc();

        Map<Long, String> userMap = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));

        return logs.stream().map(l -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", l.getId());
            m.put("username", userMap.getOrDefault(l.getUserId(), "unknown"));
            m.put("role", l.getRole());
            m.put("action", l.getAction());
            m.put("target", l.getTarget());
            m.put("targetId", l.getTargetId());
            m.put("createdAt", l.getCreatedAt());
            return m;
        }).toList();
    }

    // =========================
    // RECENT SALES
    // =========================
    public List<Sale> recentSales(Long userId) {
        requireOwner(userId); // ✅ ตอนนี้ OWNER/ADMIN เข้าได้

        return saleRepository.findAll().stream()
                .sorted(Comparator.comparing(Sale::getSoldAt).reversed())
                .limit(10)
                .toList();
    }

    // =========================
    // DAILY SALES
    // =========================
    public List<Map<String, Object>> salesDaily(Long userId, int days) {
        requireOwner(userId); // ✅ ตอนนี้ OWNER/ADMIN เข้าได้

        if (days < 1) days = 1;
        if (days > 365) days = 365;

        Map<Long, Product> productMap = loadProductMap();
        Set<Long> existingProductIds = productMap.keySet();

        LocalDate today = LocalDate.now(APP_ZONE);
        LocalDate start = today.minusDays(days - 1);

        Map<LocalDate, BigDecimal> map = new HashMap<>();

        for (Sale s : saleRepository.findAll()) {
            LocalDate d = toLocalDate(s.getSoldAt());
            if (d.isBefore(start) || d.isAfter(today)) continue;

            BigDecimal total = saleTotalOnlyExistingProducts(s, existingProductIds);
            if (total.compareTo(BigDecimal.ZERO) <= 0) continue;

            map.merge(d, total, BigDecimal::add);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            result.add(Map.of(
                    "date", d.toString(),
                    "total", map.getOrDefault(d, BigDecimal.ZERO)
            ));
        }
        return result;
    }

    // =========================
    // DAILY SALES STACKED (4 params)
    // =========================
    public Map<String, Object> salesDailyStacked(Long userId, int days, int top, Long categoryId) {
        requireOwner(userId); // ✅ ตอนนี้ OWNER/ADMIN เข้าได้

        if (days < 1) days = 1;
        if (days > 365) days = 365;
        if (top < 1) top = 1;
        if (top > 20) top = 20;

        Map<Long, Product> productMap = loadProductMap();
        Set<Long> allowedProductIds = filterProductIdsByCategory(productMap, categoryId);

        LocalDate today = LocalDate.now(APP_ZONE);
        LocalDate start = today.minusDays(days - 1);

        List<String> dates = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            dates.add(start.plusDays(i).toString());
        }

        Map<Long, Integer> qtyMap = new HashMap<>();

        List<Sale> sales = saleRepository.findAll();
        for (Sale s : sales) {
            LocalDate d = toLocalDate(s.getSoldAt());
            if (d.isBefore(start) || d.isAfter(today)) continue;
            if (s.getItems() == null) continue;

            for (SaleItem it : s.getItems()) {
                if (it == null || it.getProductId() == null) continue;
                Long pid = it.getProductId();
                if (!allowedProductIds.contains(pid)) continue;

                int q = (it.getQty() == null) ? 0 : it.getQty();
                qtyMap.merge(pid, q, Integer::sum);
            }
        }

        if (qtyMap.isEmpty()) {
            return Map.of("dates", dates, "series", List.of());
        }

        List<Long> topProductIds = qtyMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(top)
                .map(Map.Entry::getKey)
                .toList();

        Map<Long, BigDecimal[]> moneyByProduct = new LinkedHashMap<>();
        for (Long pid : topProductIds) {
            BigDecimal[] arr = new BigDecimal[days];
            Arrays.fill(arr, BigDecimal.ZERO);
            moneyByProduct.put(pid, arr);
        }

        for (Sale s : sales) {
            LocalDate d = toLocalDate(s.getSoldAt());
            if (d.isBefore(start) || d.isAfter(today)) continue;
            if (s.getItems() == null) continue;

            int dayIndex = (int) (d.toEpochDay() - start.toEpochDay());
            if (dayIndex < 0 || dayIndex >= days) continue;

            for (SaleItem it : s.getItems()) {
                if (it == null || it.getProductId() == null) continue;
                Long pid = it.getProductId();

                if (!moneyByProduct.containsKey(pid)) continue;
                if (!allowedProductIds.contains(pid)) continue;

                BigDecimal sub = nz(it.getSubTotal());
                moneyByProduct.get(pid)[dayIndex] = moneyByProduct.get(pid)[dayIndex].add(sub);
            }
        }

        List<Map<String, Object>> series = new ArrayList<>();
        for (Long pid : topProductIds) {
            Product p = productMap.get(pid);
            if (p == null) continue;

            Map<String, Object> row = new HashMap<>();
            row.put("productId", pid);
            row.put("name", p.getName());
            row.put("totals", Arrays.asList(moneyByProduct.get(pid)));
            series.add(row);
        }

        return Map.of("dates", dates, "series", series);
    }

    // backward compatible
    public Map<String, Object> salesDailyStacked(Long userId, int days, int top) {
        return salesDailyStacked(userId, days, top, null);
    }

    // =========================
    // MONTHLY SALES
    // =========================
    public List<Map<String, Object>> salesMonthly(Long userId) {
        requireOwner(userId); // ✅ ตอนนี้ OWNER/ADMIN เข้าได้

        Map<Long, Product> productMap = loadProductMap();
        Set<Long> existingProductIds = productMap.keySet();

        Map<String, BigDecimal> map = new TreeMap<>();

        for (Sale s : saleRepository.findAll()) {
            BigDecimal total = saleTotalOnlyExistingProducts(s, existingProductIds);
            if (total.compareTo(BigDecimal.ZERO) <= 0) continue;

            LocalDate d = toLocalDate(s.getSoldAt());
            String key = d.getYear() + "-" + String.format("%02d", d.getMonthValue());
            map.merge(key, total, BigDecimal::add);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : map.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("month", e.getKey());
            row.put("total", e.getValue());
            result.add(row);
        }
        return result;
    }

    // =========================
    // helpers
    // =========================
    private User requireOwner(Long userId) {
        if (userId == null) throw new RuntimeException("Unauthorized");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("ไม่พบผู้ใช้"));

        String role = (user.getRole() == null) ? "" : user.getRole().trim().toUpperCase();

        // ✅ FIX: อนุญาต OWNER หรือ ADMIN
        if (!("OWNER".equals(role) || "ADMIN".equals(role))) {
            throw new RuntimeException("เฉพาะ OWNER หรือ ADMIN เท่านั้น");
        }

        return user;
    }

    private LocalDate toLocalDate(Instant instant) {
        if (instant == null) return LocalDate.of(1970, 1, 1);
        return instant.atZone(APP_ZONE).toLocalDate();
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private Map<Long, Product> loadProductMap() {
        return productRepository.findAll().stream()
                .filter(p -> p.getId() != null)
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
    }

    private Set<Long> filterProductIdsByCategory(Map<Long, Product> productMap, Long categoryId) {
        if (categoryId == null) {
            return new HashSet<>(productMap.keySet());
        }
        return productMap.values().stream()
                .filter(p -> p.getCategory() != null
                        && p.getCategory().getId() != null
                        && Objects.equals(p.getCategory().getId(), categoryId))
                .map(Product::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private BigDecimal saleTotalOnlyExistingProducts(Sale sale, Set<Long> existingProductIds) {
        if (sale == null || sale.getItems() == null || sale.getItems().isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (SaleItem it : sale.getItems()) {
            if (it == null || it.getProductId() == null) continue;
            if (!existingProductIds.contains(it.getProductId())) continue;
            sum = sum.add(nz(it.getSubTotal()));
        }
        return sum;
    }
}