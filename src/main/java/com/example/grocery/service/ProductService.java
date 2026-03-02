package com.example.grocery.service;

import com.example.grocery.dto.common.PageResponse;
import com.example.grocery.dto.product.ProductResponse;
import com.example.grocery.entity.Category;
import com.example.grocery.entity.Product;
import com.example.grocery.repository.CategoryRepository;
import com.example.grocery.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final AuditLogService auditLogService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // -------------------------
    // LIST
    // -------------------------
    public PageResponse<ProductResponse> listByCategory(
            Long categoryId, int page, int size, String q
    ) {
        if (size <= 0) size = 6;
        if (page < 0) page = 0;

        if (!categoryRepository.existsById(categoryId)) {
            throw new RuntimeException("ไม่พบหมวดหมู่สินค้า");
        }

        Pageable pageable =
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        String keyword = (q == null) ? "" : q.trim();
        Page<Product> p;

        if (keyword.isBlank()) {
            p = productRepository.findByCategoryId(categoryId, pageable);
        } else {
            p = productRepository.findByCategoryIdAndNameContainingIgnoreCase(
                    categoryId, keyword, pageable
            );
        }

        List<ProductResponse> items =
                p.getContent().stream().map(this::toDto).toList();

        return PageResponse.<ProductResponse>builder()
                .items(items)
                .page(p.getNumber())
                .size(p.getSize())
                .totalItems(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .hasNext(p.hasNext())
                .hasPrev(p.hasPrevious())
                .build();
    }

    // -------------------------
    // CREATE
    // -------------------------
    public ProductResponse create(
            Long categoryId,
            String name,
            BigDecimal buyPrice,
            BigDecimal sellPrice,
            Integer stockQty,
            Integer monthlySales,
            MultipartFile image,
            Long authUserId
    ) throws IOException {

        Category cat = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("ไม่พบหมวดหมู่สินค้า"));

        String cleanName = normalizeName(name);
        if (cleanName.isBlank()) throw new RuntimeException("กรุณากรอกชื่อสินค้า");

        // ✅ กันชื่อซ้ำในหมวดเดียวกัน
        if (productRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, cleanName)) {
            throw new RuntimeException("ชื่อสินค้านี้มีอยู่แล้วในหมวดนี้");
        }

        if (buyPrice == null || buyPrice.compareTo(BigDecimal.ZERO) < 0)
            throw new RuntimeException("ราคาซื้อต้องไม่ติดลบ");
        if (sellPrice == null || sellPrice.compareTo(BigDecimal.ZERO) < 0)
            throw new RuntimeException("ราคาขายต้องไม่ติดลบ");
        if (stockQty == null || stockQty < 0)
            throw new RuntimeException("สินค้าคงเหลือต้องไม่ติดลบ");
        if (monthlySales == null || monthlySales < 0) monthlySales = 0;

        if (image == null || image.isEmpty())
            throw new RuntimeException("กรุณาเลือกรูปสินค้า");

        String filename = saveImageToProductDir(image);

        Product saved = productRepository.save(Product.builder()
                .category(cat)
                .name(cleanName)
                .buyPrice(buyPrice)
                .sellPrice(sellPrice)
                .stockQty(stockQty)
                .monthlySales(monthlySales)
                .imageFilename(filename)
                .createdAt(Instant.now())
                .build());

        auditLogService.log(authUserId, "CREATE", "PRODUCT", saved.getId());

        return toDto(saved);
    }

    // -------------------------
    // UPDATE
    // -------------------------
    public ProductResponse update(
            Long productId,
            String name,
            BigDecimal buyPrice,
            BigDecimal sellPrice,
            Integer stockQty,
            Integer monthlySales,
            MultipartFile image,
            Long authUserId
    ) throws IOException {

        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("ไม่พบสินค้า"));

        // ✅ ถ้ามีการส่ง name มา และเปลี่ยนจริง -> กันชื่อซ้ำในหมวดเดียวกัน
        if (name != null) {
            String cleanName = normalizeName(name);
            if (!cleanName.isBlank() && !cleanName.equalsIgnoreCase(p.getName())) {
                Long categoryId = p.getCategory().getId();
                if (productRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(categoryId, cleanName, productId)) {
                    throw new RuntimeException("ชื่อสินค้านี้มีอยู่แล้วในหมวดนี้");
                }
                p.setName(cleanName);
            }
        }

        if (buyPrice != null) {
            if (buyPrice.compareTo(BigDecimal.ZERO) < 0)
                throw new RuntimeException("ราคาซื้อต้องไม่ติดลบ");
            p.setBuyPrice(buyPrice);
        }
        if (sellPrice != null) {
            if (sellPrice.compareTo(BigDecimal.ZERO) < 0)
                throw new RuntimeException("ราคาขายต้องไม่ติดลบ");
            p.setSellPrice(sellPrice);
        }
        if (stockQty != null) {
            if (stockQty < 0)
                throw new RuntimeException("สินค้าคงเหลือต้องไม่ติดลบ");
            p.setStockQty(stockQty);
        }
        if (monthlySales != null) {
            if (monthlySales < 0)
                throw new RuntimeException("ยอดขายเดือนนี้ต้องไม่ติดลบ");
            p.setMonthlySales(monthlySales);
        }

        if (image != null && !image.isEmpty()) {
            String newFilename = saveImageToProductDir(image);
            deleteProductImageIfExists(p.getImageFilename());
            p.setImageFilename(newFilename);
        }

        Product saved = productRepository.save(p);

        auditLogService.log(authUserId, "UPDATE", "PRODUCT", saved.getId());

        return toDto(saved);
    }

    // -------------------------
    // DELETE
    // -------------------------
    public void delete(Long productId, Long authUserId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("ไม่พบสินค้า"));

        deleteProductImageIfExists(p.getImageFilename());
        productRepository.delete(p);

        auditLogService.log(authUserId, "DELETE", "PRODUCT", productId);
    }

    // -------------------------
    // HELPERS
    // -------------------------
    private String normalizeName(String s) {
        if (s == null) return "";
        // trim + รวมช่องว่างซ้ำให้เหลือช่องเดียว
        return s.trim().replaceAll("\\s+", " ");
    }

    private String saveImageToProductDir(MultipartFile image) throws IOException {
        String original = image.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(original);
        if (ext == null) ext = "png";
        ext = ext.toLowerCase();

        if (!List.of("png", "jpg", "jpeg", "webp").contains(ext)) {
            throw new RuntimeException("รองรับเฉพาะไฟล์รูป png/jpg/jpeg/webp");
        }

        String filename = UUID.randomUUID() + "." + ext;

        Path base = Paths.get(uploadDir, "products");
        Files.createDirectories(base);

        Path dest = base.resolve(filename).normalize();
        Files.copy(image.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        return filename;
    }

    private void deleteProductImageIfExists(String filename) {
        try {
            if (filename == null || filename.isBlank()) return;
            Path p = Paths.get(uploadDir, "products")
                    .resolve(filename).normalize();
            Files.deleteIfExists(p);
        } catch (Exception ignored) {}
    }

    private ProductResponse toDto(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .categoryId(p.getCategory().getId())
                .name(p.getName())
                .buyPrice(p.getBuyPrice())
                .sellPrice(p.getSellPrice())
                .stockQty(p.getStockQty())
                .monthlySales(p.getMonthlySales())
                .imageUrl("/uploads/products/" + p.getImageFilename())
                .build();
    }
}