package com.ruetcse.attendanceapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ClassViewHolder> {

    private final ArrayList<ClassItem> classList;
    private final Context context;

    public ClassAdapter(Context context, ArrayList<ClassItem> classList) {
        this.context = context;
        this.classList = classList;
    }

    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_class_button, parent, false);
        return new ClassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        ClassItem currentItem = classList.get(position);
        holder.tvCourseCode.setText(currentItem.getCourseCode());
        holder.tvDepartment.setText("Dept: " + currentItem.getDepartment());
        holder.tvSeriesNo.setText("Series: " + currentItem.getSeriesNo());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AttendanceActivity.class);
            intent.putExtra("DEPARTMENT", currentItem.getDepartment());
            intent.putExtra("COURSE_CODE", currentItem.getCourseCode());
            intent.putExtra("SERIES_NO", currentItem.getSeriesNo());
            intent.putExtra("FIRST_ROLL", currentItem.getFirstRoll());
            intent.putExtra("LAST_ROLL", currentItem.getLastRoll());
            intent.putExtra("PERMANENT_ABSENT", currentItem.getPermanentAbsentRolls());
            intent.putExtra("EXTRA_ROLLS", currentItem.getExtraRolls());
            context.startActivity(intent);
        });

        holder.btnExportExcel.setOnClickListener(v -> {
            ExcelExporter.exportClassAttendance(context, currentItem);
        });
    }

    @Override
    public int getItemCount() {
        return classList.size();
    }

    public static class ClassViewHolder extends RecyclerView.ViewHolder {
        TextView tvCourseCode, tvDepartment, tvSeriesNo;
        View btnExportExcel;

        public ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseCode = itemView.findViewById(R.id.tvCourseCode);
            tvDepartment = itemView.findViewById(R.id.tvDepartment);
            tvSeriesNo = itemView.findViewById(R.id.tvSeriesNo);
            btnExportExcel = itemView.findViewById(R.id.btnExportExcel);
        }
    }
}
