package com.example.midexam_dwikycandra;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class StudentActivityTest {
    private StudentDatabase database;
    private final List<Long> createdRows = new ArrayList<>();

    @Before
    public void setUp() {
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = new StudentDatabase(app);
    }

    @After
    public void tearDown() {
        for (long rowId : createdRows) {
            database.delete(rowId);
        }
        database.close();
    }

    @Test
    public void recreationKeepsDraftAndUpdatesTheSelectedRowInsteadOfInserting() {
        String studentId = "ui-" + UUID.randomUUID();
        long rowId = database.insert("Original student", studentId, true);
        createdRows.add(rowId);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onData(studentRow(rowId)).onChildView(withId(R.id.edit_student)).perform(click());
            onView(withId(R.id.student_name)).perform(replaceText("Updated student"), closeSoftKeyboard());
            onView(withId(R.id.student_id)).perform(replaceText(studentId + "-edited"), closeSoftKeyboard());
            onView(withId(R.id.has_enrolled)).perform(click());

            scenario.recreate();

            scenario.onActivity(activity -> {
                assertEquals("Updated student", ((EditText) activity.findViewById(R.id.student_name))
                        .getText().toString());
                assertEquals(studentId + "-edited", ((EditText) activity.findViewById(R.id.student_id))
                        .getText().toString());
                assertFalse(((CheckBox) activity.findViewById(R.id.has_enrolled)).isChecked());
                assertEquals(View.VISIBLE, activity.findViewById(R.id.cancel_edit).getVisibility());
            });
            onView(withId(R.id.save_student)).perform(click());
        }

        Student saved = studentByRow(rowId);
        assertEquals("Updated student", saved.name);
        assertEquals(studentId + "-edited", saved.studentId);
        assertFalse(saved.enrolled);
        for (Student student : database.getAll()) {
            assertFalse("An extra row was inserted instead of updating", studentId.equals(student.studentId));
        }
    }

    @Test
    public void cancellingDeletePreservesTheSelectedRecord() {
        long rowId = database.insert("Keep this student", "ui-" + UUID.randomUUID(), true);
        createdRows.add(rowId);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onData(studentRow(rowId)).onChildView(withId(R.id.delete_student)).perform(click());
            onView(withId(android.R.id.button2)).perform(click());
        }

        Student saved = studentByRow(rowId);
        assertEquals("Keep this student", saved.name);
        assertTrue(saved.enrolled);
    }

    private Student studentByRow(long rowId) {
        for (Student student : database.getAll()) {
            if (student.rowId == rowId) {
                return student;
            }
        }
        throw new AssertionError("Student was removed: " + rowId);
    }

    private Matcher<Object> studentRow(long rowId) {
        return new BoundedMatcher<Object, Student>(Student.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("student row " + rowId);
            }

            @Override
            protected boolean matchesSafely(Student student) {
                return student.rowId == rowId;
            }
        };
    }
}
