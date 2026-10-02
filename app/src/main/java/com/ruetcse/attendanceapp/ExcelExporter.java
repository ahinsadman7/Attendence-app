package com.ruetcse.attendanceapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExcelExporter {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void exportClassAttendance(Context context, ClassItem classItem) {
        String classKey = getClassKey(classItem.getDepartment(), classItem.getCourseCode(), classItem.getSeriesNo());

        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Fetching attendance data from Firebase...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("attendance")
                .child(classKey)
                .child("sessions");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    progressDialog.dismiss();
                    Toast.makeText(context, "No attendance data found for this class yet.", Toast.LENGTH_LONG).show();
                    return;
                }

                progressDialog.setMessage("Generating Excel sheet...");

                // Parse sessions
                List<ClassSessionData> sessionList = new ArrayList<>();
                TreeSet<String> allRolls = new TreeSet<>();

                for (DataSnapshot sessionSnapshot : snapshot.getChildren()) {
                    try {
                        int classNum = 0;
                        try {
                            classNum = Integer.parseInt(sessionSnapshot.getKey());
                        } catch (NumberFormatException ignored) {
                            Long num = sessionSnapshot.child("classNumber").getValue(Long.class);
                            if (num != null) classNum = num.intValue();
                        }

                        String date = sessionSnapshot.child("date").getValue(String.class);
                        if (date == null) date = "";

                        Map<String, String> records = new HashMap<>();
                        DataSnapshot recordsSnapshot = sessionSnapshot.child("records");
                        for (DataSnapshot rec : recordsSnapshot.getChildren()) {
                            String roll = rec.getKey();
                            String status = rec.getValue(String.class);
                            if (roll != null) {
                                records.put(roll, status != null ? status : "A");
                                allRolls.add(roll);
                            }
                        }

                        sessionList.add(new ClassSessionData(classNum, date, records));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (sessionList.isEmpty()) {
                    progressDialog.dismiss();
                    Toast.makeText(context, "No valid sessions found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Sort sessions by class number ascending
                Collections.sort(sessionList, (a, b) -> Integer.compare(a.classNumber, b.classNumber));

                // Generate Excel file in background thread
                executor.execute(() -> {
                    File file = createExcelWorkbook(context, classItem, sessionList, new ArrayList<>(allRolls));
                    mainHandler.post(() -> {
                        progressDialog.dismiss();
                        if (file != null && file.exists()) {
                            shareExcelFile(context, file, classItem);
                        } else {
                            Toast.makeText(context, "Failed to create Excel file.", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(context, "Firebase error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private static File createExcelWorkbook(Context context, ClassItem classItem,
                                            List<ClassSessionData> sessions, List<String> rolls) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet("Attendance");

            // Fonts
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);

            Font regularFont = workbook.createFont();
            regularFont.setFontHeightInPoints((short) 11);

            Font presentFont = workbook.createFont();
            presentFont.setBold(true);
            presentFont.setColor(IndexedColors.DARK_GREEN.getIndex());

            Font absentFont = workbook.createFont();
            absentFont.setBold(true);
            absentFont.setColor(IndexedColors.RED.getIndex());

            // Cell Styles
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle subtitleStyle = workbook.createCellStyle();
            subtitleStyle.setFont(regularFont);
            subtitleStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(headerStyle);

            CellStyle cellStyleDefault = workbook.createCellStyle();
            cellStyleDefault.setFont(regularFont);
            cellStyleDefault.setAlignment(HorizontalAlignment.CENTER);
            setBorders(cellStyleDefault);

            CellStyle rollStyle = workbook.createCellStyle();
            rollStyle.setFont(headerFont);
            rollStyle.setAlignment(HorizontalAlignment.CENTER);
            setBorders(rollStyle);

            CellStyle presentStyle = workbook.createCellStyle();
            presentStyle.setFont(presentFont);
            presentStyle.setAlignment(HorizontalAlignment.CENTER);
            presentStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            presentStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(presentStyle);

            CellStyle absentStyle = workbook.createCellStyle();
            absentStyle.setFont(absentFont);
            absentStyle.setAlignment(HorizontalAlignment.CENTER);
            absentStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            absentStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(absentStyle);

            int totalCols = 1 + sessions.size() + 4; // Roll + N classes + (Total, Present, Absent, %)

            // Row 0: Title
            Row titleRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(classItem.getCourseCode() + " - " + classItem.getDepartment() + " (Series " + classItem.getSeriesNo() + ")");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, Math.max(totalCols - 1, 1)));

            // Row 1: Subtitle
            Row subTitleRow = sheet.createRow(1);
            org.apache.poi.ss.usermodel.Cell subCell = subTitleRow.createCell(0);
            String exportTime = new SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.getDefault()).format(new Date());
            subCell.setCellValue("Exported on: " + exportTime + " | Total Sessions: " + sessions.size());
            subCell.setCellStyle(subtitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, Math.max(totalCols - 1, 1)));

            // Row 3: Header
            Row headerRow = sheet.createRow(3);
            int colIdx = 0;

            // Col 0: Roll Number
            org.apache.poi.ss.usermodel.Cell cRoll = headerRow.createCell(colIdx++);
            cRoll.setCellValue("Roll Number");
            cRoll.setCellStyle(headerStyle);

            // Columns for each Class session
            for (ClassSessionData session : sessions) {
                org.apache.poi.ss.usermodel.Cell c = headerRow.createCell(colIdx++);
                String label = "Class " + session.classNumber;
                if (!session.date.isEmpty()) {
                    label += "\n(" + session.date + ")";
                }
                c.setCellValue(label);
                c.setCellStyle(headerStyle);
            }

            // Summary columns
            org.apache.poi.ss.usermodel.Cell cTot = headerRow.createCell(colIdx++);
            cTot.setCellValue("Total Classes");
            cTot.setCellStyle(headerStyle);

            org.apache.poi.ss.usermodel.Cell cPre = headerRow.createCell(colIdx++);
            cPre.setCellValue("Total Present");
            cPre.setCellStyle(headerStyle);

            org.apache.poi.ss.usermodel.Cell cAbs = headerRow.createCell(colIdx++);
            cAbs.setCellValue("Total Absent");
            cAbs.setCellStyle(headerStyle);

            org.apache.poi.ss.usermodel.Cell cPct = headerRow.createCell(colIdx++);
            cPct.setCellValue("Attendance %");
            cPct.setCellStyle(headerStyle);

            // Data Rows (Row 4 onwards)
            int rowIdx = 4;
            for (String roll : rolls) {
                Row row = sheet.createRow(rowIdx++);
                colIdx = 0;

                // Roll Number
                org.apache.poi.ss.usermodel.Cell rollCell = row.createCell(colIdx++);
                rollCell.setCellValue(roll);
                rollCell.setCellStyle(rollStyle);

                int presentCount = 0;
                int absentCount = 0;

                for (ClassSessionData session : sessions) {
                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(colIdx++);
                    String status = session.records.get(roll);
                    if (status == null) status = "-";

                    cell.setCellValue(status);
                    if ("P".equalsIgnoreCase(status)) {
                        cell.setCellStyle(presentStyle);
                        presentCount++;
                    } else if ("A".equalsIgnoreCase(status)) {
                        cell.setCellStyle(absentStyle);
                        absentCount++;
                    } else {
                        cell.setCellStyle(cellStyleDefault);
                    }
                }

                int attendedTotal = presentCount + absentCount;
                int totalClassesHeld = sessions.size();

                // Total classes
                org.apache.poi.ss.usermodel.Cell totalCell = row.createCell(colIdx++);
                totalCell.setCellValue(totalClassesHeld);
                totalCell.setCellStyle(cellStyleDefault);

                // Present
                org.apache.poi.ss.usermodel.Cell preCell = row.createCell(colIdx++);
                preCell.setCellValue(presentCount);
                preCell.setCellStyle(cellStyleDefault);

                // Absent
                org.apache.poi.ss.usermodel.Cell absCell = row.createCell(colIdx++);
                absCell.setCellValue(absentCount);
                absCell.setCellStyle(cellStyleDefault);

                // %
                org.apache.poi.ss.usermodel.Cell pctCell = row.createCell(colIdx++);
                double pct = totalClassesHeld > 0 ? (presentCount * 100.0) / totalClassesHeld : 0.0;
                pctCell.setCellValue(String.format(Locale.US, "%.1f%%", pct));
                pctCell.setCellStyle(cellStyleDefault);
            }

            // Auto-size columns
            for (int i = 0; i < totalCols; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.max(currentWidth + 1000, 3200));
            }

            // Save file
            File dir = context.getExternalCacheDir();
            if (dir == null) dir = context.getCacheDir();

            String sanitizedCourse = classItem.getCourseCode().replaceAll("\\s+", "_");
            String fileName = "Attendance_" + classItem.getDepartment() + "_" + sanitizedCourse + "_Series" + classItem.getSeriesNo() + ".xlsx";
            File outputFile = new File(dir, fileName);

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }

            return outputFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                workbook.close();
            } catch (IOException ignored) {}
        }
    }

    private static void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private static void shareExcelFile(Context context, File file, ClassItem classItem) {
        try {
            Uri fileUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    file
            );

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.putExtra(Intent.EXTRA_SUBJECT, "Attendance Sheet - " + classItem.getCourseCode());
            intent.putExtra(Intent.EXTRA_TEXT, "Attendance sheet for " + classItem.getCourseCode() + " (" + classItem.getDepartment() + ", Series " + classItem.getSeriesNo() + ").");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(intent, "Share Attendance Sheet (.xlsx)"));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error sharing file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static String getClassKey(String department, String courseCode, String seriesNo) {
        String raw = (department + "_" + courseCode + "_" + seriesNo).trim();
        return raw.replaceAll("[.#$\\[\\]/\\s]+", "_");
    }

    private static class ClassSessionData {
        int classNumber;
        String date;
        Map<String, String> records;

        ClassSessionData(int classNumber, String date, Map<String, String> records) {
            this.classNumber = classNumber;
            this.date = date;
            this.records = records;
        }
    }
}
