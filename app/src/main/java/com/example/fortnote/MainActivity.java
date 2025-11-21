package com.example.fortnote;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends AppCompatActivity implements NoteAdapter.OnNoteClickListener {

    private RecyclerView recyclerView;
    private NoteAdapter noteAdapter;
    private NoteManager noteManager;
    private TextView tvEmptyState;


    private static final int SORT_DATE_CREATED=0;
    private static final int SORT_LAST_EDITED=1;
    private int currentSort=SORT_LAST_EDITED;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        noteManager = new NoteManager(this);

        recyclerView = findViewById(R.id.recyclerViewNotes);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Spinner spinnerSort = findViewById(R.id.spinnerSort);
        String[] sortOptions={"Date Created","Last Edited"};
        ArrayAdapter<String> adapter= new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,sortOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(adapter);

        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id){
                currentSort=position;
                loadNotes();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent){}
        });
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
            startActivity(intent);
        });

        loadNotes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }

    private void loadNotes() {
        List<Note> notes = noteManager.getAllNotes();

        if (currentSort == SORT_DATE_CREATED) {
            notes.sort((n1, n2) -> Long.compare(n2.getCreationTimestamp(), n1.getCreationTimestamp()));
        } else if (currentSort == SORT_LAST_EDITED) {
            notes.sort((n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));
        }

        if (notes.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }

        if (noteAdapter == null) {
            noteAdapter = new NoteAdapter(notes, this);
            recyclerView.setAdapter(noteAdapter);
        } else {
            noteAdapter.updateNotes(notes);
        }
        noteAdapter.setSortByCreationDate(currentSort==SORT_DATE_CREATED);
    }

    @Override
    public void onNoteClick(Note note) {
        Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
        intent.putExtra("note_id", note.getId());
        intent.putExtra("note_title", note.getTitle());
        intent.putExtra("note_content", note.getContent());
        startActivity(intent);
    }

    @Override
    public void onNoteLongClick(Note note) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    noteManager.deleteNote(note.getId());
                    loadNotes();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
