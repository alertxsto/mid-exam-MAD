package com.example.midexam_dwikycandra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.DatabaseErrorHandler;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class StudentDatabaseTest {
    private Context context;
    private StudentDatabase database;
    private File databaseFile;

    @Before
    public void setUp() {
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        databaseFile = new File(app.getCacheDir(), "students-" + UUID.randomUUID() + ".db");
        context = new ContextWrapper(app) {
            @Override
            public File getDatabasePath(String name) {
                return databaseFile;
            }

            @Override
            public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory) {
                return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), factory);
            }

            @Override
            public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory,
                                                       DatabaseErrorHandler errorHandler) {
                return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).getPath(),
                        factory, errorHandler);
            }
        };
        database = new StudentDatabase(context);
    }

    @After
    public void tearDown() {
        database.close();
        SQLiteDatabase.deleteDatabase(databaseFile);
    }

    @Test
    public void enrollmentAndLeadingZeroIdsSurviveReopening() {
        database.insert("Dwiky Candra", "002", true);
        database.insert("Nadia Putri", "004", false);
        database.close();
        database = new StudentDatabase(context);

        List<Student> students = database.getAll();
        assertEquals(2, students.size());
        Student dwiky = findStudent(students, "002");
        Student nadia = findStudent(students, "004");
        assertEquals("Dwiky Candra", dwiky.name);
        assertTrue(dwiky.enrolled);
        assertEquals("Nadia Putri", nadia.name);
        assertFalse(nadia.enrolled);
    }

    @Test
    public void editingStudentIdKeepsIdentityAndDoesNotOverwriteAnotherStudent() {
        long rowId = database.insert("Dwiky Candra", "002", true);
        database.insert("Nadia Putri", "004", true);

        assertEquals(1, database.update(rowId, "Dwiky Updated", "006", false));
        List<Student> students = database.getAll();
        assertEquals(2, students.size());
        Student updated = findStudent(students, "006");
        assertEquals(rowId, updated.rowId);
        assertEquals("Dwiky Updated", updated.name);
        assertFalse(updated.enrolled);
        assertEquals("Nadia Putri", findStudent(students, "004").name);
        assertTrue(findStudent(students, "004").enrolled);
    }

    @Test
    public void duplicateInsertAndEditLeaveExistingRecordsUnchanged() {
        database.insert("Dwiky Candra", "002", true);
        long second = database.insert("Nadia Putri", "004", false);

        assertThrows(SQLiteConstraintException.class,
                () -> database.insert("Duplicate", "002", false));
        assertThrows(SQLiteConstraintException.class,
                () -> database.update(second, "Overwritten", "002", true));

        List<Student> students = database.getAll();
        assertEquals(2, students.size());
        assertEquals("Dwiky Candra", findStudent(students, "002").name);
        assertEquals("Nadia Putri", findStudent(students, "004").name);
        assertFalse(findStudent(students, "004").enrolled);
    }

    @Test
    public void deletingOneStudentPreservesOtherStudents() {
        long first = database.insert("Dwiky Candra", "002", true);
        database.insert("Nadia Putri", "004", false);

        assertEquals(1, database.delete(first));
        assertEquals(0, database.delete(first));
        List<Student> students = database.getAll();
        assertEquals(1, students.size());
        assertEquals("004", students.get(0).studentId);
        assertEquals("Nadia Putri", students.get(0).name);
    }

    private Student findStudent(List<Student> students, String studentId) {
        for (Student student : students) {
            if (studentId.equals(student.studentId)) {
                return student;
            }
        }
        throw new AssertionError("Student not found: " + studentId);
    }
}
