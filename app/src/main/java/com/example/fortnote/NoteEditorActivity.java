package com.example.fortnote;

import static com.example.fortnote.R.*;
import static com.example.fortnote.R.id.undo_button;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
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

import java.util.List;

public class NoteEditorActivity extends AppCompatActivity {

    private EditText etNoteTitle;
    private EditText etNoteContent;
    private NoteManager noteManager;
    private String noteId;
    private boolean isEditMode = false;
    private boolean isLocked = false;

    private final Stack<String> undoStack = new Stack<>();
    private final Stack<String> redoStack = new Stack<>();
    private boolean isTextChangingProgrammatically = false;

    private long lastEditTime = 0;
    private static final long UNDO_DELAY = 800;

    // Keep a local copy for scrambling when user locks (session-only)
    private String originalContent = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_note);

        noteManager = new NoteManager(this);

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

        if (getIntent().hasExtra("note_id")) {
            isEditMode = true;
            noteId = getIntent().getStringExtra("note_id");
            String title = getIntent().getStringExtra("note_title");
            String content = getIntent().getStringExtra("note_content");

            etNoteTitle.setText(title);
            etNoteContent.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
        }

        // On open, enforce locked state from storage (prevents edit after reopen)
        if (isEditMode) {
            List<Note> all = noteManager.getAllNotes();
            for (Note n : all) {
                if (n.getId().equals(noteId)) {
                    isLocked = n.isLocked();
                    if (isLocked) {
                        // Show scrambled placeholder and disable editing
                        etNoteContent.setText(toScrambledTextOfLength(n.getContent() != null ? n.getContent().length() : 16));
                        etNoteTitle.setEnabled(false);
                        etNoteContent.setEnabled(false);
                        lockButton.setBackgroundResource(android.R.drawable.ic_lock_lock);
                    } else {
                        lockButton.setBackgroundResource(android.R.drawable.ic_lock_idle_lock);
                    }
                    break;
                }
            }
        }

        backButton.setOnClickListener(v -> finish());

        // Lock/Unlock Button (Option A: affects ALL notes)
        lockButton.setOnClickListener(v -> {
            isLocked = !isLocked;

            if (isLocked) {
                try {
                    originalContent = etNoteContent.getText().toString();

                    // TODO: replace with real password capture UX if desired
                    String password = "password";
                    noteManager.encryptAllNotes(password);

                    etNoteContent.setText(toScrambledText(originalContent));
                    etNoteTitle.setEnabled(false);
                    etNoteContent.setEnabled(false);

                    lockButton.setBackgroundResource(android.R.drawable.ic_lock_lock);
                    Toast.makeText(this, "Note locked", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Encryption failed", Toast.LENGTH_SHORT).show();
                    isLocked = false;
                }
            } else {
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
                builder.setTitle("Enter password to unlock");

                final EditText input = new EditText(this);
                input.setHint("Password");
                builder.setView(input);

                builder.setPositiveButton("OK", (dialog, which) -> {
                    String password = input.getText().toString();
                    try {
                        // Use decrypted list directly and persist
                        List<Note> decryptedNotes = noteManager.getAllNotesDecrypted(password);

                        for (Note note : decryptedNotes) {
                            if (note.getId().equals(noteId)) {
                                // Restore the exact HTML content
                                etNoteContent.setText(Html.fromHtml(note.getContent(), Html.FROM_HTML_MODE_LEGACY));
                                break;
                            }
                        }

                        etNoteTitle.setEnabled(true);
                        etNoteContent.setEnabled(true);

                        lockButton.setBackgroundResource(android.R.drawable.ic_lock_idle_lock);
                        Toast.makeText(this, "Note unlocked", Toast.LENGTH_SHORT).show();
                        isLocked = false;
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show();
                        isLocked = true;
                    }
                });

                builder.setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.cancel();
                    isLocked = true;
                });

                builder.show();
            }
        });

        lockButton.setBackgroundResource(android.R.drawable.ic_lock_idle_lock);

        boldButton.setOnClickListener(v -> applyStyle(Typeface.BOLD));
        italicButton.setOnClickListener(v -> applyStyle(Typeface.ITALIC));
        underlineButton.setOnClickListener(v -> applyUnderline());
        fabSave.setOnClickListener(v -> saveNote());
        undoButton.setOnClickListener(v -> undo());
        redoButton.setOnClickListener(v -> redo());

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
                    long now = System.currentTimeMillis();
                    if (now - lastEditTime > UNDO_DELAY) {
                        undoStack.push(previousText);
                        redoStack.clear();
                    }
                    lastEditTime = now;
                }
            }
        });
    }

    // Scramble based on actual text (used immediately on locking)
    private String toScrambledText(String text) {
        String symbols = "ÆØΔ¥$#@%&*?¶Ω≈≠±";
        StringBuilder scrambled = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                scrambled.append(c);
            } else {
                scrambled.append(symbols.charAt((int)(Math.random() * symbols.length())));
            }
        }
        return scrambled.toString();
    }

    // Scramble to a fixed length (used when reopening locked notes)
    private String toScrambledTextOfLength(int length) {
        if (length <= 0) length = 16;
        String symbols = "ÆØΔ¥$#@%&*?¶Ω≈≠±";
        StringBuilder scrambled = new StringBuilder();
        for (int i = 0; i < length; i++) {
            scrambled.append(symbols.charAt((int)(Math.random() * symbols.length())));
        }
        return scrambled.toString();
    }

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
        spannable.setSpan(new StyleSpan(style), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
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
        spannable.setSpan(new UnderlineSpan(), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
        etNoteContent.setText(spannable);
        etNoteContent.setSelection(start, end);
    }

    private void saveNote() {
        if (isLocked) {
            Toast.makeText(this, "Note is locked. Unlock to save changes.", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etNoteTitle.getText().toString().trim();
        String content = Html.toHtml(etNoteContent.getText(), Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);

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


