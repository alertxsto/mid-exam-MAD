package com.example.midexam_dwikycandra;

import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

public class MainActivity extends AppCompatActivity implements StudentAdapter.Actions {
    private static final String STATE_EDITING_ROW = "editing_row";
    private static final long NO_ROW = -1L;

    private StudentDatabase database;
    private StudentAdapter adapter;

    private TextInputLayout nameLayout;
    private TextInputLayout idLayout;
    private EditText nameInput;
    private EditText idInput;
    private CheckBox enrolledInput;
    private Button saveButton;
    private Button cancelButton;
    private TextView recordsTitle;
    private TextView emptyState;

    private long editingRowId = NO_ROW;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (savedInstanceState != null) {
            editingRowId = savedInstanceState.getLong(STATE_EDITING_ROW, NO_ROW);
        }

        database = new StudentDatabase(this);
        bindViews();
        renderForm();
        loadStudents();
    }

    private void bindViews() {
        nameLayout = findViewById(R.id.name_input_layout);
        idLayout = findViewById(R.id.id_input_layout);
        nameInput = findViewById(R.id.student_name);
        idInput = findViewById(R.id.student_id);
        enrolledInput = findViewById(R.id.has_enrolled);
        saveButton = findViewById(R.id.save_student);
        cancelButton = findViewById(R.id.cancel_edit);
        recordsTitle = findViewById(R.id.records_title);
        emptyState = findViewById(R.id.empty_state);

        ListView list = findViewById(R.id.student_list);
        adapter = new StudentAdapter(this, this);
        list.setAdapter(adapter);

        saveButton.setOnClickListener(v -> saveStudent());
        cancelButton.setOnClickListener(v -> {
            resetForm();
            hideKeyboard();
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_EDITING_ROW, editingRowId);
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    private void loadStudents() {
        List<Student> students = database.getAll();
        adapter.setStudents(students);
        recordsTitle.setText(getString(R.string.record_count, students.size()));
        emptyState.setVisibility(students.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void saveStudent() {
        nameLayout.setError(null);
        idLayout.setError(null);

        String name = nameInput.getText().toString().trim();
        String studentId = idInput.getText().toString().trim();
        boolean enrolled = enrolledInput.isChecked();

        boolean valid = true;
        if (name.isEmpty()) {
            nameLayout.setError(getString(R.string.name_required));
            valid = false;
        }
        if (studentId.isEmpty()) {
            idLayout.setError(getString(R.string.id_required));
            valid = false;
        }
        if (!valid) {
            return;
        }

        try {
            if (editingRowId == NO_ROW) {
                database.insert(name, studentId, enrolled);
            } else {
                database.update(editingRowId, name, studentId, enrolled);
            }
        } catch (SQLiteConstraintException duplicate) {
            idLayout.setError(getString(R.string.id_taken));
            return;
        }

        resetForm();
        loadStudents();
        hideKeyboard();
    }

    private void resetForm() {
        editingRowId = NO_ROW;
        nameInput.setText("");
        idInput.setText("");
        enrolledInput.setChecked(false);
        nameLayout.setError(null);
        idLayout.setError(null);
        renderForm();
    }

    private void renderForm() {
        boolean editing = editingRowId != NO_ROW;
        saveButton.setText(editing ? R.string.save_changes : R.string.add_student);
        cancelButton.setVisibility(editing ? View.VISIBLE : View.GONE);
    }

    private void hideKeyboard() {
        InputMethodManager keyboard = getSystemService(InputMethodManager.class);
        if (keyboard != null) {
            keyboard.hideSoftInputFromWindow(nameInput.getWindowToken(), 0);
        }
    }

    @Override
    public void onView(Student student) {
        String enrollment = getString(student.enrolled
                ? R.string.enrolled_yes
                : R.string.enrolled_no);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.student_details)
                .setMessage(getString(R.string.details_body, student.name, student.studentId, enrollment))
                .setPositiveButton(R.string.close, null)
                .show();
    }

    @Override
    public void onEdit(Student student) {
        editingRowId = student.rowId;
        nameInput.setText(student.name);
        idInput.setText(student.studentId);
        enrolledInput.setChecked(student.enrolled);
        nameLayout.setError(null);
        idLayout.setError(null);
        renderForm();
        nameInput.requestFocus();
    }

    @Override
    public void onDelete(Student student) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_title)
                .setMessage(getString(R.string.delete_message, student.name))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    database.delete(student.rowId);
                    if (editingRowId == student.rowId) {
                        resetForm();
                    }
                    loadStudents();
                })
                .show();
    }
}