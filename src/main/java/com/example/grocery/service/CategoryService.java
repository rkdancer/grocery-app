package com.example.grocery.service;

import com.example.grocery.dto.category.CategoryResponse;
import com.example.grocery.dto.common.PageResponse;
import com.example.grocery.entity.Category;
import com.example.grocery.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AuditLogService auditLogService; // ✅ เพิ่ม

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // =========================
    // CREATE
    // =========================
    public CategoryResponse create(
            Long authUserId,
            String name,
            MultipartFile image
    ) throws IOException {

        String cleanName = name == null ? "" : name.trim();

        if (cleanName.isBlank())
            throw new RuntimeException("กรุณากรอกชื่อประเภทสินค้า");

        if (categoryRepository.existsByNameIgnoreCase(cleanName))
            throw new RuntimeException("ชื่อประเภทสินค้านี้มีอยู่แล้ว");

        if (image == null || image.isEmpty())
            throw new RuntimeException("กรุณาเลือกรูปภาพประกอบ");

        String filename = saveImageToCategoryDir(image);

        Category saved = categoryRepository.save(
                Category.builder()
                        .name(cleanName)
                        .imageFilename(filename)
                        .createdAt(Instant.now())
                        .build()
        );

        // ✅ AUDIT LOG
        auditLogService.log(
                authUserId,
                "CREATE",
                "CATEGORY",
                saved.getId()
        );

        return toDto(saved);
    }

    // =========================
    // LIST (PAGINATION + SEARCH)
    // =========================
    public PageResponse<CategoryResponse> list(int page, int size, String q) {

        if (size <= 0) size = 6;
        if (page < 0) page = 0;

        String keyword = (q == null) ? "" : q.trim();

        Pageable pageable =
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Category> p = keyword.isBlank()
                ? categoryRepository.findAll(pageable)
                : categoryRepository.findByNameContainingIgnoreCase(keyword, pageable);

        List<CategoryResponse> items =
                p.getContent().stream().map(this::toDto).toList();

        return PageResponse.<CategoryResponse>builder()
                .items(items)
                .page(p.getNumber())
                .size(p.getSize())
                .totalItems(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .hasNext(p.hasNext())
                .hasPrev(p.hasPrevious())
                .build();
    }

    // =========================
    // UPDATE
    // =========================
    public CategoryResponse update(
            Long authUserId,
            Long id,
            String name,
            MultipartFile image
    ) throws IOException {

        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ไม่พบประเภทสินค้า"));

        if (name != null && !name.trim().isBlank()) {
            String cleanName = name.trim();

            if (!cleanName.equalsIgnoreCase(c.getName())
                    && categoryRepository.existsByNameIgnoreCase(cleanName)) {
                throw new RuntimeException("ชื่อประเภทสินค้านี้มีอยู่แล้ว");
            }

            c.setName(cleanName);
        }

        if (image != null && !image.isEmpty()) {
            String newFilename = saveImageToCategoryDir(image);
            deleteCategoryImageIfExists(c.getImageFilename());
            c.setImageFilename(newFilename);
        }

        Category saved = categoryRepository.save(c);

        // ✅ AUDIT LOG
        auditLogService.log(
                authUserId,
                "UPDATE",
                "CATEGORY",
                saved.getId()
        );

        return toDto(saved);
    }

    // =========================
    // DELETE
    // =========================
    public void delete(Long authUserId, Long id) {

        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ไม่พบประเภทสินค้า"));

        deleteCategoryImageIfExists(c.getImageFilename());
        categoryRepository.delete(c);

        // ✅ AUDIT LOG
        auditLogService.log(
                authUserId,
                "DELETE",
                "CATEGORY",
                id
        );
    }

    // =========================
    // HELPERS
    // =========================
    private String saveImageToCategoryDir(MultipartFile image) throws IOException {

        String original = image.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(original);
        if (ext == null) ext = "png";
        ext = ext.toLowerCase();

        if (!List.of("png", "jpg", "jpeg", "webp").contains(ext)) {
            throw new RuntimeException("รองรับเฉพาะไฟล์รูป png/jpg/jpeg/webp");
        }

        String filename = UUID.randomUUID() + "." + ext;

        Path base = Paths.get(uploadDir, "categories");
        Files.createDirectories(base);

        Path dest = base.resolve(filename).normalize();
        Files.copy(image.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        return filename;
    }

    private void deleteCategoryImageIfExists(String filename) {
        try {
            if (filename == null || filename.isBlank()) return;
            Path p = Paths.get(uploadDir, "categories")
                    .resolve(filename)
                    .normalize();
            Files.deleteIfExists(p);
        } catch (Exception ignored) {
        }
    }

    private CategoryResponse toDto(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .imageUrl("/uploads/categories/" + c.getImageFilename())
                .build();
    }
}
