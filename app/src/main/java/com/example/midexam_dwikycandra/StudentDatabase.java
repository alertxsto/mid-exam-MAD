package com.example.midexam_dwikycandra;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

final class StudentDatabase extends SQLiteOpenHelper {
    private static final String TABLE = "students";
    private static final String[] COLUMNS = {"_id", "name", "student_id", "enrolled"};

    StudentDatabase(Context context) {
        super(context, "students.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE students ("
                + "_id INTEGER PRIMARY KEY, "
                + "name TEXT NOT NULL CHECK(length(trim(name)) > 0), "
                + "student_id TEXT NOT NULL UNIQUE CHECK(length(trim(student_id)) > 0), "
                + "enrolled INTEGER NOT NULL CHECK(enrolled IN (0, 1)))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 is the first schema.
    }

    List<Student> getAll() {
        List<Student> students = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(TABLE, COLUMNS, null, null,
                null, null, "_id DESC")) {
            while (cursor.moveToNext()) {
                students.add(new Student(cursor.getLong(0), cursor.getString(1),
                        cursor.getString(2), cursor.getInt(3) == 1));
            }
        }
        return students;
    }

    long insert(String name, String studentId, boolean enrolled) {
        return getWritableDatabase().insertOrThrow(TABLE, null, values(name, studentId, enrolled));
    }

    int update(long rowId, String name, String studentId, boolean enrolled) {
        return getWritableDatabase().update(TABLE, values(name, studentId, enrolled),
                "_id = ?", new String[]{Long.toString(rowId)});
    }

    int delete(long rowId) {
        return getWritableDatabase().delete(TABLE, "_id = ?", new String[]{Long.toString(rowId)});
    }

    private ContentValues values(String name, String studentId, boolean enrolled) {
        ContentValues values = new ContentValues(3);
        values.put("name", name);
        values.put("student_id", studentId);
        values.put("enrolled", enrolled ? 1 : 0);
        return values;
    }
}
