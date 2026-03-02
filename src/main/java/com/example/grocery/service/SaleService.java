package com.example.grocery.service;

import com.example.grocery.dto.SaleItemRequest;
import com.example.grocery.dto.SaleRequest;
import com.example.grocery.entity.Product;
import com.example.grocery.entity.Sale;
import com.example.grocery.entity.SaleItem;
import com.example.grocery.repository.ProductRepository;
import com.example.grocery.repository.SaleRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final AuditLogService auditLogService; // ✅ เพิ่ม

    /**
     * ✅ DEMO MODE:
     * - ถ้า true จะอนุญาตให้ส่ง soldAt ย้อนหลังได้ (เพื่อ seed ข้อมูลโชว์กราฟหลายเดือน)
     * - ถ้า false จะ ignore soldAt และใช้ Instant.now() เสมอ (ปลอดภัยกว่า)
     *
     * เปิดได้ใน application.properties:
     *   app.demo.allowBackdateSales=true
     */
    @Value("${app.demo.allowBackdateSales:false}")
    private boolean allowBackdateSales;
    @PostConstruct
    public void init() {
        System.out.println("DEMO allowBackdateSales = " + allowBackdateSales);
    }

    @Transactional
    public Sale sell(Long userId, SaleRequest req) {

        if (req == null || req.getItems() == null || req.getItems().isEmpty()) {
            throw new RuntimeException("ไม่มีสินค้าในรายการขาย");
        }

        // ✅ เลือกเวลา soldAt
        Instant effectiveSoldAt = Instant.now();
        if (allowBackdateSales && req.getSoldAt() != null) {
            effectiveSoldAt = req.getSoldAt();
        }

        List<SaleItem> saleItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (SaleItemRequest r : req.getItems()) {

            if (r == null || r.getProductId() == null) {
                throw new RuntimeException("ข้อมูลสินค้าไม่ครบ (productId)");
            }
            if (r.getQty() == null || r.getQty() <= 0) {
                throw new RuntimeException("จำนวนสินค้าต้องมากกว่า 0");
            }

            Product p = productRepository.findById(r.getProductId())
                    .orElseThrow(() -> new RuntimeException("ไม่พบสินค้า"));

            if (p.getStockQty() == null) {
                throw new RuntimeException("สต๊อกสินค้าไม่ถูกต้อง: " + p.getName());
            }

            if (p.getStockQty() < r.getQty()) {
                throw new RuntimeException("สต๊อกไม่พอ: " + p.getName());
            }

            // 🔻 ตัดสต๊อก
            p.setStockQty(p.getStockQty() - r.getQty());

            // 🔺 เพิ่มยอดขาย (หมายเหตุ: monthlySales เป็น field เดิมของคุณ)
            // ถ้าคุณอยากให้ monthlySales อิงจากการขายจริงแบบเดือนต่อเดือน "จริง ๆ"
            // แนะนำให้เลิกเก็บ monthlySales ใน Product แล้วคำนวณจาก SaleItem + soldAt แทน
            if (p.getMonthlySales() == null) p.setMonthlySales(0);
            p.setMonthlySales(p.getMonthlySales() + r.getQty());

            productRepository.save(p);

            // 💰 คำนวณเงิน
            if (p.getSellPrice() == null) {
                throw new RuntimeException("ราคาขายไม่ถูกต้อง: " + p.getName());
            }

            BigDecimal lineTotal =
                    p.getSellPrice().multiply(BigDecimal.valueOf(r.getQty()));

            SaleItem si = SaleItem.builder()
                    .productId(p.getId())
                    .productName(p.getName())
                    .qty(r.getQty())
                    .price(p.getSellPrice())
                    .subTotal(lineTotal)
                    .build();

            total = total.add(lineTotal);
            saleItems.add(si);
        }

        Sale sale = Sale.builder()
                .soldBy(userId)
                .soldAt(effectiveSoldAt) // ✅ ใช้เวลาที่เลือก
                .totalPrice(total)
                .build();

        saleRepository.save(sale);

        saleItems.forEach(i -> i.setSale(sale));
        sale.setItems(saleItems);

        // ✅ AUDIT LOG (SELL)
        auditLogService.log(
                userId,
                "SELL",
                "SALE",
                sale.getId()
        );

        return sale;
    }

}
