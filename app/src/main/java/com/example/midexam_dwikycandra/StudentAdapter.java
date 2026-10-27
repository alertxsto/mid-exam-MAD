package com.example.midexam_dwikycandra;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

final class StudentAdapter extends BaseAdapter {
    interface Actions {
        void onView(Student student);

        void onEdit(Student student);

        void onDelete(Student student);
    }

    private final LayoutInflater inflater;
    private final Actions actions;
    private final List<Student> students = new ArrayList<>();

    StudentAdapter(Context context, Actions actions) {
        this.inflater = LayoutInflater.from(context);
        this.actions = actions;
    }

    void setStudents(List<Student> students) {
        this.students.clear();
        this.students.addAll(students);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return students.size();
    }

    @Override
    public Student getItem(int position) {
        return students.get(position);
    }

    @Override
    public long getItemId(int position) {
        return students.get(position).rowId;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.student_row, parent, false);
            holder = new Holder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        Student student = getItem(position);
        holder.name.setText(student.name);
        holder.id.setText(student.studentId);
        holder.enrollment.setText(student.enrolled
                ? R.string.enrolled_yes
                : R.string.enrolled_no);
        holder.enrollment.setTextColor(themeColor(convertView, student.enrolled
                ? androidx.appcompat.R.attr.colorPrimary
                : com.google.android.material.R.attr.colorOnSurfaceVariant));
        holder.view.setOnClickListener(v -> actions.onView(student));
        holder.edit.setOnClickListener(v -> actions.onEdit(student));
        holder.delete.setOnClickListener(v -> actions.onDelete(student));
        return convertView;
    }

    private static int themeColor(View view, int attr) {
        android.util.TypedValue value = new android.util.TypedValue();
        if (view.getContext().getTheme().resolveAttribute(attr, value, true)) {
            return value.data;
        }
        return 0;
    }

    private static final class Holder {
        final TextView name;
        final TextView id;
        final TextView enrollment;
        final View view;
        final View edit;
        final View delete;

        Holder(View row) {
            name = row.findViewById(R.id.record_name);
            id = row.findViewById(R.id.record_id);
            enrollment = row.findViewById(R.id.record_enrollment);
            view = row.findViewById(R.id.view_student);
            edit = row.findViewById(R.id.edit_student);
            delete = row.findViewById(R.id.delete_student);
        }
    }
}