package com.ruetcse.attendanceapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttendanceActivity extends AppCompatActivity {

    private final ArrayList<StudentRoll> studentList = new ArrayList<>();
    private TextView tvSummary;
    private RollAdapter adapter;
    private EditText etClassNumber;
    private EditText etAttendanceDate;
    private ProgressBar progressSave;
    private Button btnSave;

    private String dept;
    private String course;
    private String series;
    private String classKey;
    private DatabaseReference sessionsRef;

    private static final Map<String, String> DEPT_CODES = new HashMap<>();
    static {
        DEPT_CODES.put("EEE", "01");
        DEPT_CODES.put("ME", "02");
        DEPT_CODES.put("CSE", "03");
        DEPT_CODES.put("CE", "04");
        DEPT_CODES.put("ETE", "05");
        DEPT_CODES.put("IPE", "06");
        DEPT_CODES.put("CME", "07");
        DEPT_CODES.put("URP", "08");
        DEPT_CODES.put("Arch", "09");
        DEPT_CODES.put("BECM", "10");
        DEPT_CODES.put("MSE", "11");
        DEPT_CODES.put("ECE", "12");
        DEPT_CODES.put("MTE", "13");
        DEPT_CODES.put("ChE", "14");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        TextView tvTitle = findViewById(R.id.tvHeaderTitle);
        TextView tvSubtitle = findViewById(R.id.tvHeaderSubtitle);
        tvSummary = findViewById(R.id.tvAttendanceSummary);
        RecyclerView recyclerView = findViewById(R.id.recyclerViewRolls);

        etClassNumber = findViewById(R.id.etClassNumber);
        etAttendanceDate = findViewById(R.id.etAttendanceDate);
        progressSave = findViewById(R.id.progressSave);

        Button btnAllPresent = findViewById(R.id.btnAllPresent);
        Button btnAllAbsent = findViewById(R.id.btnAllAbsent);
        btnSave = findViewById(R.id.btnSaveAttendance);

        dept = getIntent().getStringExtra("DEPARTMENT");
        course = getIntent().getStringExtra("COURSE_CODE");
        series = getIntent().getStringExtra("SERIES_NO");
        int firstRoll = getIntent().getIntExtra("FIRST_ROLL", 0);
        int lastRoll = getIntent().getIntExtra("LAST_ROLL", 0);
        String permAbsentStr = getIntent().getStringExtra("PERMANENT_ABSENT");
        String extraRollsStr = getIntent().getStringExtra("EXTRA_ROLLS");

        tvTitle.setText(course + " (" + dept + ")");
        tvSubtitle.setText("Series: " + series);

        // Date selection setup
        setupDateSelector();

        // Generate rolls
        generateRollNumbers(series, dept, firstRoll, lastRoll, permAbsentStr, extraRollsStr);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RollAdapter(studentList, this::updateSummary);
        recyclerView.setAdapter(adapter);

        updateSummary();

        // Firebase reference & auto class number detection
        classKey = ExcelExporter.getClassKey(dept, course, series);
        sessionsRef = FirebaseDatabase.getInstance()
                .getReference("attendance")
                .child(classKey)
                .child("sessions");

        loadNextClassNumber();

        btnAllPresent.setOnClickListener(v -> setAllAttendance(true));
        btnAllAbsent.setOnClickListener(v -> setAllAttendance(false));
        btnSave.setOnClickListener(v -> saveAttendanceToFirebase());
    }

    private void setupDateSelector() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        etAttendanceDate.setText(sdf.format(calendar.getTime()));

        etAttendanceDate.setOnClickListener(v -> {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePicker = new DatePickerDialog(this, (view, y, m, d) -> {
                calendar.set(y, m, d);
                etAttendanceDate.setText(sdf.format(calendar.getTime()));
            }, year, month, day);
            datePicker.show();
        });
    }

    private void loadNextClassNumber() {
        sessionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int maxClassNum = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        int num = Integer.parseInt(child.getKey());
                        if (num > maxClassNum) maxClassNum = num;
                    } catch (NumberFormatException ignored) {
                        Long val = child.child("classNumber").getValue(Long.class);
                        if (val != null && val > maxClassNum) {
                            maxClassNum = val.intValue();
                        }
                    }
                }
                etClassNumber.setText(String.valueOf(maxClassNum + 1));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Non-fatal, leave default "1"
            }
        });
    }

    private void generateRollNumbers(String series, String dept, int first, int last, String permAbsentStr,
            String extraRollsStr) {
        String deptCode = DEPT_CODES.getOrDefault(dept, "00");
        ArrayList<String> permAbsents = parseRollList(permAbsentStr);
        ArrayList<String> extraRolls = parseRollList(extraRollsStr);

        for (int i = first; i <= last; i++) {
            String rollFormatted = String.format("%03d", i);
            String fullRoll = series + deptCode + rollFormatted;

            if (!permAbsents.contains(fullRoll) && !permAbsents.contains(String.valueOf(i))) {
                studentList.add(new StudentRoll(fullRoll));
            }
        }

        for (String extra : extraRolls) {
            if (!extra.isEmpty()) {
                studentList.add(new StudentRoll(extra));
            }
        }
    }

    private void setAllAttendance(boolean isPresent) {
        for (StudentRoll student : studentList) {
            student.setPresent(isPresent);
        }
        adapter.notifyDataSetChanged();
        updateSummary();
    }

    private ArrayList<String> parseRollList(String rawInput) {
        ArrayList<String> list = new ArrayList<>();
        if (rawInput != null && !rawInput.trim().isEmpty()) {
            String[] tokens = rawInput.split(",");
            for (String token : tokens) {
                list.add(token.trim());
            }
        }
        return list;
    }

    private void updateSummary() {
        int presentCount = 0;
        for (StudentRoll roll : studentList) {
            if (roll.isPresent())
                presentCount++;
        }
        int total = studentList.size();
        int absentCount = total - presentCount;

        tvSummary.setText("Total: " + total + " | Present: " + presentCount + " | Absent: " + absentCount);
    }

    private void saveAttendanceToFirebase() {
        String classNumStr = etClassNumber.getText().toString().trim();
        if (classNumStr.isEmpty()) {
            etClassNumber.setError("Enter class number");
            return;
        }

        int classNumber;
        try {
            classNumber = Integer.parseInt(classNumStr);
            if (classNumber <= 0) {
                etClassNumber.setError("Must be positive number");
                return;
            }
        } catch (NumberFormatException e) {
            etClassNumber.setError("Invalid number");
            return;
        }

        String dateStr = etAttendanceDate.getText().toString().trim();
        if (dateStr.isEmpty()) {
            dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
        }

        int presentCount = 0;
        Map<String, String> records = new HashMap<>();
        List<String> absentRolls = new ArrayList<>();

        for (StudentRoll student : studentList) {
            if (student.isPresent()) {
                records.put(student.getRollNumber(), "P");
                presentCount++;
            } else {
                records.put(student.getRollNumber(), "A");
                absentRolls.add(student.getRollNumber());
            }
        }

        int total = studentList.size();
        int absentCount = total - presentCount;

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("classNumber", classNumber);
        sessionData.put("date", dateStr);
        sessionData.put("timestamp", System.currentTimeMillis());
        sessionData.put("totalStudents", total);
        sessionData.put("presentCount", presentCount);
        sessionData.put("absentCount", absentCount);
        sessionData.put("records", records);
        sessionData.put("absentRolls", absentRolls);

        // Show progress indicator
        progressSave.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        sessionsRef.child(String.valueOf(classNumber)).setValue(sessionData)
                .addOnSuccessListener(aVoid -> {
                    progressSave.setVisibility(View.GONE);
                    btnSave.setEnabled(true);

                    String absentSummary = absentRolls.isEmpty() ? "None" : String.join(", ", absentRolls);
                    Toast.makeText(this, "Class " + classNumber + " saved to Firebase!\nAbsent: " + absentSummary, Toast.LENGTH_LONG).show();

                    // Increment for next class session
                    etClassNumber.setText(String.valueOf(classNumber + 1));
                })
                .addOnFailureListener(e -> {
                    progressSave.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Failed to save to Firebase: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
