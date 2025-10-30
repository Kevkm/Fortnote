package com.example.fortnote;

import static com.example.fortnote.R.*;
import static com.example.fortnote.R.id.undo_button;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.util.Stack;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class NoteEditorActivity extends AppCompatActivity {

    private EditText etNoteTitle;
    private EditText etNoteContent;
    private NoteManager noteManager;
    private String noteId;
    private boolean isEditMode = false;
    private boolean isLocked = false;

    // Undo/Redo stacks
    private final Stack<String> undoStack = new Stack<>();
    private final Stack<String> redoStack = new Stack<>();
    private boolean isTextChangingProgrammatically = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_note);

        noteManager = new NoteManager(this);

        // Initialize views
        etNoteTitle = findViewById(id.etNoteTitle);
        etNoteContent = findViewById(id.etNoteContent);
        Button backButton = findViewById(id.back_button);
        Button lockButton = findViewById(id.lock_button);
        Button boldButton = findViewById(id.bold_button);
        Button italicButton = findViewById(id.italic_button);
        Button underlineButton = findViewById(id.under_button);
        ImageButton undoButton = findViewById(id.undo_button);
        ImageButton redoButton = findViewById(id.redo_button);
        FloatingActionButton fabSave = findViewById(id.fabSave);

        // Check if we're editing an existing note
        if (getIntent().hasExtra("note_id")) {
            isEditMode = true;
            noteId = getIntent().getStringExtra("note_id");
            String title = getIntent().getStringExtra("note_title");
            String content = getIntent().getStringExtra("note_content");

            etNoteTitle.setText(title);
            etNoteContent.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
        }

        // Back button
        backButton.setOnClickListener(v -> finish());

        // Lock button
        lockButton.setOnClickListener(v -> {
            isLocked = !isLocked;
            etNoteTitle.setEnabled(!isLocked);
            etNoteContent.setEnabled(!isLocked);

            if (isLocked) {
                lockButton.setBackgroundResource(android.R.drawable.ic_lock_lock);
                Toast.makeText(this, "Note locked", Toast.LENGTH_SHORT).show();
            } else {
                lockButton.setBackgroundResource(android.R.drawable.ic_lock_idle_lock);
                Toast.makeText(this, "Note unlocked", Toast.LENGTH_SHORT).show();
            }
        });

        // Initially set lock icon
        lockButton.setBackgroundResource(android.R.drawable.ic_lock_idle_lock);

        // Bold button
        boldButton.setOnClickListener(v -> applyStyle(Typeface.BOLD));

        // Italic button
        italicButton.setOnClickListener(v -> applyStyle(Typeface.ITALIC));

        // Underline button
        underlineButton.setOnClickListener(v -> applyUnderline());

        // Save button
        fabSave.setOnClickListener(v -> saveNote());

        // Undo / Redo listeners
        undoButton.setOnClickListener(v -> undo());
        redoButton.setOnClickListener(v -> redo());

        // Text watcher to track changes for undo/redo
        etNoteContent.addTextChangedListener(new TextWatcher() {
            private String previousText = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!isTextChangingProgrammatically) {
                    previousText = s.toString();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!isTextChangingProgrammatically) {
                    undoStack.push(previousText);
                    redoStack.clear();
                }
            }
        });
    }

    /** Undo last change */
    private void undo() {
        if (!undoStack.isEmpty()) {
            String lastState = undoStack.pop();
            redoStack.push(etNoteContent.getText().toString());
            isTextChangingProgrammatically = true;
            etNoteContent.setText(lastState);
            etNoteContent.setSelection(lastState.length());
            isTextChangingProgrammatically = false;
        } else {
            Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT).show();
        }
    }

    /** Redo last undone change */
    private void redo() {
        if (!redoStack.isEmpty()) {
            String nextState = redoStack.pop();
            undoStack.push(etNoteContent.getText().toString());
            isTextChangingProgrammatically = true;
            etNoteContent.setText(nextState);
            etNoteContent.setSelection(nextState.length());
            isTextChangingProgrammatically = false;
        } else {
            Toast.makeText(this, "Nothing to redo", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyStyle(int style) {
        int start = etNoteContent.getSelectionStart();
        int end = etNoteContent.getSelectionEnd();

        if (start >= end) {
            Toast.makeText(this, "Please select text first", Toast.LENGTH_SHORT).show();
            return;
        }

        SpannableStringBuilder spannable = new SpannableStringBuilder(etNoteContent.getText());
        spannable.setSpan(new StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        etNoteContent.setText(spannable);
        etNoteContent.setSelection(start, end);
    }

    private void applyUnderline() {
        int start = etNoteContent.getSelectionStart();
        int end = etNoteContent.getSelectionEnd();

        if (start >= end) {
            Toast.makeText(this, "Please select text first", Toast.LENGTH_SHORT).show();
            return;
        }

        SpannableStringBuilder spannable = new SpannableStringBuilder(etNoteContent.getText());
        spannable.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        etNoteContent.setText(spannable);
        etNoteContent.setSelection(start, end);
    }

    private void saveNote() {
        if (isLocked) {
            Toast.makeText(this, "Note is locked. Unlock to save changes.", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etNoteTitle.getText().toString().trim();
        String content = Html.toHtml(etNoteContent.getText(),Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "Note is empty", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (isEditMode) {
            noteManager.updateNote(noteId, title, content);
            Toast.makeText(this, "Note updated!", Toast.LENGTH_SHORT).show();
        } else {
            noteManager.saveNote(title, content);
            Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show();
        }

        finish();
    }
}

