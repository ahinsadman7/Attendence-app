package com.ruetcse.attendanceapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ArrayList<ClassItem> classList;
    private ClassAdapter adapter;

    private final String[] departments = {
            "CSE", "EEE", "ECE", "ETE", "CE", "Arch",
            "URP", "BECM", "ME", "IPE", "CME", "MTE", "MSE", "ChE"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        classList = new ArrayList<>();

        RecyclerView recyclerView = findViewById(R.id.recyclerViewClasses);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ClassAdapter(this, classList);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fabAddClass = findViewById(R.id.fabAddClass);
        fabAddClass.setOnClickListener(v -> showAddClassDialog());
    }

    private void showAddClassDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_class, null);

        Spinner spinnerDept = dialogView.findViewById(R.id.spinnerDepartment);
        EditText etSeriesNo = dialogView.findViewById(R.id.etSeriesNo);
        EditText etCourseCode = dialogView.findViewById(R.id.etCourseCode);
        EditText etFirstRoll = dialogView.findViewById(R.id.etFirstRoll);
        EditText etLastRoll = dialogView.findViewById(R.id.etLastRoll);
        EditText etPermanentAbsent = dialogView.findViewById(R.id.etPermanentAbsent);
        EditText etExtraRolls = dialogView.findViewById(R.id.etExtraRolls);

        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                departments
        );
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDept.setAdapter(deptAdapter);

        builder.setView(dialogView)
                .setTitle("Add New Class")
                .setPositiveButton("Add", (dialog, which) -> {
                    String dept = spinnerDept.getSelectedItem().toString();
                    String series = etSeriesNo.getText().toString().trim();
                    String course = etCourseCode.getText().toString().trim();

                    String firstStr = etFirstRoll.getText().toString().trim();
                    String lastStr = etLastRoll.getText().toString().trim();

                    int first = firstStr.isEmpty() ? 0 : Integer.parseInt(firstStr);
                    int last = lastStr.isEmpty() ? 0 : Integer.parseInt(lastStr);

                    String permAbsent = etPermanentAbsent.getText().toString().trim();
                    String extra = etExtraRolls.getText().toString().trim();

                    ClassItem newClass = new ClassItem(dept, series, course, first, last, permAbsent, extra);
                    classList.add(newClass);
                    adapter.notifyItemInserted(classList.size() - 1);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
