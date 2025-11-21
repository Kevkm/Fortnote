package com.example.fortnote;

import static com.example.fortnote.R.*;

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
import com.google.android.material.button.MaterialButton;
import android.widget.LinearLayout;

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

    private String originalHtml = "";

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
        MaterialButton undoButton = findViewById(id.undo_button);
        MaterialButton redoButton = findViewById(id.redo_button);
        FloatingActionButton fabSave = findViewById(id.fabSave);

        if (getIntent().hasExtra("note_id")) {
            isEditMode = true;
            noteId = getIntent().getStringExtra("note_id");
            etNoteTitle.setText(getIntent().getStringExtra("note_title"));
            etNoteContent.setText(Html.fromHtml(getIntent().getStringExtra("note_content")));
        }

        if (isEditMode) {
            List<Note> all = noteManager.getAllNotes();
            for (Note n : all) {
                if (n.getId().equals(noteId)) {
                    isLocked = n.isLocked();
                    if (isLocked) {
                        int len = n.getPlaintextLength() > 0 ? n.getPlaintextLength() : 16;
                        etNoteContent.setText(scrambleFromLengthPreserveSpaces(len));
                        etNoteContent.setEnabled(false);
                        etNoteTitle.setEnabled(false);
                        lockButton.setBackgroundResource(android.R.drawable.ic_lock_lock);
                    } else {
                        lockButton.setBackgroundResource(android.R.drawable.ic_lock_idle_lock);
                    }
                }
            }
        }

        backButton.setOnClickListener(v -> finish());

        lockButton.setOnClickListener(v -> {
            if (!isLocked) lockNote(lockButton);
            else unlockNote(lockButton);
        });

        boldButton.setOnClickListener(v -> applyStyle(Typeface.BOLD));
        italicButton.setOnClickListener(v -> applyStyle(Typeface.ITALIC));
        underlineButton.setOnClickListener(v -> applyUnderline());

        fabSave.setOnClickListener(v -> saveNote());
        undoButton.setOnClickListener(v -> undo());
        redoButton.setOnClickListener(v -> redo());

        etNoteContent.addTextChangedListener(new TextWatcher() {
            private String before = "";
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { before = s.toString(); }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isTextChangingProgrammatically) {
                    long now = System.currentTimeMillis();
                    if (now - lastEditTime > UNDO_DELAY) {
                        undoStack.push(before);
                        redoStack.clear();
                    }
                    lastEditTime = now;
                }
            }
        });
    }

    
    private void lockNote(Button lockButton) {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        EditText p1Field = new EditText(this);
        p1Field.setHint("Enter Password");
        p1Field.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(p1Field);

        EditText p2Field = new EditText(this);
        p2Field.setHint("Confirm Password");
        p2Field.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(p2Field);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Lock Note")
                .setView(layout)
                .setPositiveButton("OK", (d, w) -> {

                    String p1 = p1Field.getText().toString();
                    String p2 = p2Field.getText().toString();

                    if (p1.isEmpty() || !p1.equals(p2)) {
                        Toast.makeText(this,"Passwords do not match",Toast.LENGTH_SHORT).show();
                        return;
                    }

                    originalHtml = Html.toHtml(etNoteContent.getText());

                    int len = Html.fromHtml(originalHtml).toString().length();

                    noteManager.updateNote(noteId,
                            etNoteTitle.getText().toString(),
                            originalHtml); 
                    List<Note> all = noteManager.getAllNotes();
                    for (Note n : all) {
                        if (n.getId().equals(noteId)) {
                            n.setPlaintextLength(len);
                            break;
                        }
                    }

                    boolean ok = noteManager.encryptNote(noteId, p1);
                    if (!ok) {
                        Toast.makeText(this,"Encryption failed",Toast.LENGTH_SHORT).show();
                        return;
                    }

                    etNoteContent.setText(scrambleFromLengthPreserveSpaces(len));
                    etNoteContent.setEnabled(false);
                    etNoteTitle.setEnabled(false);

                    lockButton.setBackgroundResource(android.R.drawable.ic_lock_lock);
                    isLocked = true;
                    Toast.makeText(this,"Note locked",Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    
    private void unlockNote(Button lockButton) {

        EditText passField = new EditText(this);
        passField.setHint("Password");
        passField.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Unlock Note")
                .setView(passField)
                .setPositiveButton("OK", (d, w) -> {

                    String pass = passField.getText().toString();
                    String decrypted = noteManager.decryptNote(noteId, pass);

                    if (decrypted == null) {
                        Toast.makeText(this,"Wrong password",Toast.LENGTH_SHORT).show();
                        return;
                    }

                    etNoteContent.setText(Html.fromHtml(decrypted));
                    etNoteContent.setEnabled(true);
                    etNoteTitle.setEnabled(true);

                    lockButton.setBackgroundResource(android.R.drawable.ic_lock_idle_lock);
                    isLocked = false;

                    Toast.makeText(this,"Note unlocked",Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

   
    private String scrambleFromLengthPreserveSpaces(int length) {
        String symbols = "ÆØΔ¥$#@%&*?¶Ω≈≠±";
        StringBuilder sb = new StringBuilder();

        
        for (int i = 0; i < length; i++) {
            sb.append(symbols.charAt((int)(Math.random() * symbols.length())));
        }

        return sb.toString();
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            String last = undoStack.pop();
            redoStack.push(etNoteContent.getText().toString());
            isTextChangingProgrammatically = true;
            etNoteContent.setText(last);
            etNoteContent.setSelection(last.length());
            isTextChangingProgrammatically = false;
        }
    }

    private void redo() {
        if (!redoStack.isEmpty()) {
            String next = redoStack.pop();
            undoStack.push(etNoteContent.getText().toString());
            isTextChangingProgrammatically = true;
            etNoteContent.setText(next);
            etNoteContent.setSelection(next.length());
            isTextChangingProgrammatically = false;
        }
    }

    private void applyStyle(int style) {
        int start = etNoteContent.getSelectionStart();
        int end = etNoteContent.getSelectionEnd();
        if (start < end) {
            SpannableStringBuilder span = new SpannableStringBuilder(etNoteContent.getText());
            span.setSpan(new StyleSpan(style), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
            etNoteContent.setText(span);
            etNoteContent.setSelection(start, end);
        }
    }

    private void applyUnderline() {
        int start = etNoteContent.getSelectionStart();
        int end = etNoteContent.getSelectionEnd();
        if (start < end) {
            SpannableStringBuilder span = new SpannableStringBuilder(etNoteContent.getText());
            span.setSpan(new UnderlineSpan(), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
            etNoteContent.setText(span);
            etNoteContent.setSelection(start, end);
        }
    }

    private void saveNote() {
        if (isLocked) {
            Toast.makeText(this,"Unlock first",Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etNoteTitle.getText().toString().trim();
        String content = Html.toHtml(etNoteContent.getText());

        if (isEditMode) {
            noteManager.updateNote(noteId, title, content);
            Toast.makeText(this,"Note updated!",Toast.LENGTH_SHORT).show();
        } else {
            noteManager.saveNote(title, content);
            Toast.makeText(this,"Note saved!",Toast.LENGTH_SHORT).show();
        }

        finish();
    }
}
