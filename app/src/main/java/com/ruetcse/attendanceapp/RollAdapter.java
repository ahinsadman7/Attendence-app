package com.ruetcse.attendanceapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.List;

public class RollAdapter extends RecyclerView.Adapter<RollAdapter.RollViewHolder> {

    public interface OnAttendanceChangeListener {
        void onChange();
    }

    private final List<StudentRoll> rollList;
    private final OnAttendanceChangeListener listener;

    public RollAdapter(List<StudentRoll> rollList, OnAttendanceChangeListener listener) {
        this.rollList = rollList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RollViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_roll, parent, false);
        return new RollViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RollViewHolder holder, int position) {
        StudentRoll student = rollList.get(position);
        holder.tvRollNumber.setText(student.getRollNumber());

        holder.switchAttendance.setOnCheckedChangeListener(null);
        holder.switchAttendance.setChecked(student.isPresent());
        updateSwitchText(holder.switchAttendance, student.isPresent());

        holder.switchAttendance.setOnCheckedChangeListener((buttonView, isChecked) -> {
            student.setPresent(isChecked);
            updateSwitchText(holder.switchAttendance, isChecked);
            if (listener != null) listener.onChange();
        });
    }

    private void updateSwitchText(SwitchMaterial switchMaterial, boolean isPresent) {
        if (isPresent) {
            switchMaterial.setText("Present");
            switchMaterial.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            switchMaterial.setText("Absent");
            switchMaterial.setTextColor(Color.parseColor("#F44336"));
        }
    }

    @Override
    public int getItemCount() {
        return rollList.size();
    }

    public static class RollViewHolder extends RecyclerView.ViewHolder {
        TextView tvRollNumber;
        SwitchMaterial switchAttendance;

        public RollViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRollNumber = itemView.findViewById(R.id.tvRollNumber);
            switchAttendance = itemView.findViewById(R.id.switchAttendance);
        }
    }
}
