package com.example.grocery.controller;

import com.example.grocery.entity.AuditLog;
import com.example.grocery.repository.AuditLogRepository;
import com.example.grocery.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/audit-logs")
@CrossOrigin(origins = "*")
public class AuditLogExportController {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @GetMapping("/export")
    public void exportExcel(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        Long userId = (Long) request.getAttribute("authUserId");

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("ไม่พบผู้ใช้"));

        if (!user.getRole().equalsIgnoreCase("OWNER")
                && !user.getRole().equalsIgnoreCase("ADMIN")) {
            throw new RuntimeException("ไม่มีสิทธิ์ Export Audit Log");
        }

        List<AuditLog> logs =
                auditLogRepository.findTop50ByOrderByCreatedAtDesc();

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Audit Logs");

        // ===== HEADER =====
        Row header = sheet.createRow(0);
        String[] cols = {
                "เวลา",
                "User ID",
                "Role",
                "Action",
                "Target",
                "Target ID"
        };

        for (int i = 0; i < cols.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(headerStyle(wb));
        }

        // ===== DATA =====
        int rowIdx = 1;
        for (AuditLog l : logs) {
            Row r = sheet.createRow(rowIdx++);

            r.createCell(0).setCellValue(
                    l.getCreatedAt()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                            .toString()
            );
            r.createCell(1).setCellValue(l.getUserId());
            r.createCell(2).setCellValue(l.getRole());
            r.createCell(3).setCellValue(l.getAction());
            r.createCell(4).setCellValue(l.getTarget());
            Cell targetIdCell = r.createCell(5);
            if (l.getTargetId() != null) {
                targetIdCell.setCellValue(l.getTargetId());
            } else {
                targetIdCell.setCellValue("");
            }
        }

        for (int i = 0; i < cols.length; i++) {
            sheet.autoSizeColumn(i);
        }

        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=audit_logs.xlsx"
        );

        wb.write(response.getOutputStream());
        wb.close();
    }

    private CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        style.setFont(f);
        return style;
    }
}
