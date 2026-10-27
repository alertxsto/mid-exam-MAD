package com.example.midexam_dwikycandra;

final class Student {
    final long rowId;
    final String name;
    final String studentId;
    final boolean enrolled;

    Student(long rowId, String name, String studentId, boolean enrolled) {
        this.rowId = rowId;
        this.name = name;
        this.studentId = studentId;
        this.enrolled = enrolled;
    }
}
