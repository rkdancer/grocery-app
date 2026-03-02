package com.example.grocery.service;

import com.example.grocery.dto.SaleItemRequest;
import com.example.grocery.dto.SaleRequest;
import com.example.grocery.entity.Product;
import com.example.grocery.entity.User;
import com.example.grocery.repository.ProductRepository;
import com.example.grocery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DemoSeedService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SaleService saleService;

    /**
     * ต้องเปิดโหมด demo ก่อนถึงจะ seed ได้
     * application.properties:
     *   app.demo.allowBackdateSales=true
     */
    @Value("${app.demo.allowBackdateSales:false}")
    private boolean allowBackdateSales;

    private static final Random RND = new Random();

    public Map<String, Object> seedSales(
            Long authUserId,
            int months,
            int minPerMonth,
            int maxPerMonth,
            int maxItems,
            int maxQty
    ) {
        User me = requireOwnerOrAdmin(authUserId);

        if (!allowBackdateSales) {
            throw new RuntimeException("ปิดโหมด DEMO อยู่ (ตั้ง app.demo.allowBackdateSales=true ก่อน)");
        }

        if (months < 1) months = 1;
        if (months > 24) months = 24; // กันยิงหนักเกิน
        if (minPerMonth < 1) minPerMonth = 1;
        if (maxPerMonth < minPerMonth) maxPerMonth = minPerMonth;
        if (maxItems < 1) maxItems = 1;
        if (maxItems > 8) maxItems = 8;
        if (maxQty < 1) maxQty = 1;
        if (maxQty > 10) maxQty = 10;

        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            throw new RuntimeException("ไม่มีสินค้าในระบบ (ต้องสร้างสินค้าอย่างน้อย 1 รายการก่อน)");
        }

        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);

        int totalSalesCreated = 0;

        // ทำย้อนหลัง: months เดือน (รวมเดือนนี้)
        for (int m = months - 1; m >= 0; m--) {
            LocalDate monthBase = today.withDayOfMonth(1).minusMonths(m);
            YearMonth ym = YearMonth.from(monthBase);

            int countThisMonth = randBetween(minPerMonth, maxPerMonth);

            for (int i = 0; i < countThisMonth; i++) {
                // สุ่มวันในเดือน + เวลา
                int day = randBetween(1, ym.lengthOfMonth());
                int hour = randBetween(9, 20);
                int minute = randBetween(0, 59);

                LocalDateTime ldt = LocalDateTime.of(ym.getYear(), ym.getMonthValue(), day, hour, minute);
                Instant soldAt = ldt.atZone(zone).toInstant();

                // สุ่มจำนวนรายการในบิล
                int itemCount = randBetween(1, Math.min(maxItems, products.size()));

                // สุ่มสินค้าไม่ซ้ำในบิลเดียว
                List<Product> picked = pickDistinct(products, itemCount);

                List<SaleItemRequest> items = new ArrayList<>();
                for (Product p : picked) {
                    int qty = randBetween(1, maxQty);

                    // กันสต็อกติดลบ: ถ้าไม่พอให้ "เติมสต็อก" ก่อน (เพื่อ demo)
                    ensureStock(p, qty);

                    SaleItemRequest it = new SaleItemRequest();
                    it.setProductId(p.getId());
                    it.setQty(qty);
                    items.add(it);
                }

                // เรียก flow ขายจริง (จะตัดสต็อก + สร้าง sale + sale_items + log)
                SaleRequest req = new SaleRequest();
                req.setItems(items);
                req.setSoldAt(soldAt);

                saleService.sell(me.getId(), req);

                totalSalesCreated++;
            }
        }

        return Map.of(
                "success", true,
                "message", "Seed sales สำเร็จ",
                "months", months,
                "totalSalesCreated", totalSalesCreated
        );
    }

    private void ensureStock(Product p, int needQty) {
        Integer stock = p.getStockQty();
        if (stock == null) stock = 0;

        if (stock < needQty) {
            // เติมให้พอ + เผื่ออีกนิดเพื่อให้ขายต่อได้ (demo)
            int add = (needQty - stock) + randBetween(5, 20);
            p.setStockQty(stock + add);
            productRepository.save(p);
        }
    }

    private User requireOwnerOrAdmin(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("ไม่พบผู้ใช้"));

        String role = (u.getRole() == null) ? "" : u.getRole().trim().toUpperCase();
        if (!role.equals("OWNER") && !role.equals("ADMIN")) {
            throw new RuntimeException("เฉพาะ OWNER/ADMIN เท่านั้น");
        }
        return u;
    }

    private static int randBetween(int a, int b) {
        if (a > b) { int t = a; a = b; b = t; }
        return a + RND.nextInt(b - a + 1);
    }

    private static List<Product> pickDistinct(List<Product> products, int n) {
        ArrayList<Product> copy = new ArrayList<>(products);
        Collections.shuffle(copy, RND);
        return copy.subList(0, Math.min(n, copy.size()));
    }
}
