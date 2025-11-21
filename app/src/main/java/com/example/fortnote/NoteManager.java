package com.example.fortnote;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Html;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NoteManager {

    private static final String PREFS_NAME = "FortnotePrefs";
    private static final String NOTES_KEY = "notes";
    private SharedPreferences prefs;

    public NoteManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }


    public void saveNote(String title, String content) {
        List<Note> notes = getAllNotes();
        String id = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        Note newNote = new Note(id, title, content, now, now, false);

        int ptLen = Html.fromHtml(content).toString().length();
        newNote.setPlaintextLength(ptLen);

        notes.add(0, newNote);
        saveAllNotes(notes);
    }

 
    public void updateNote(String id, String title, String content) {
        List<Note> notes = getAllNotes();

        for (Note note : notes) {
            if (note.getId().equals(id)) {
                note.setTitle(title);
                note.setContent(content);
                note.setTimestamp(System.currentTimeMillis());

                int ptLen = Html.fromHtml(content).toString().length();
                note.setPlaintextLength(ptLen);

                break;
            }
        }

        saveAllNotes(notes);
    }


    public void deleteNote(String id) {
        List<Note> notes = getAllNotes();
        notes.removeIf(note -> note.getId().equals(id));
        saveAllNotes(notes);
    }

 
    public List<Note> getAllNotes() {
        List<Note> notes = new ArrayList<>();
        String notesJson = prefs.getString(NOTES_KEY, "[]");

        try {
            JSONArray jsonArray = new JSONArray(notesJson);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);

                String id = obj.getString("id");
                String title = obj.getString("title");
                String content = obj.getString("content");

                long timestamp = obj.getLong("timestamp");
                long creationTimestamp = obj.optLong("creationTimestamp", timestamp);

                boolean locked = obj.optBoolean("locked", false);

                int plaintextLength = obj.optInt("plaintextLength", 0);

                Note note = new Note(id, title, content, timestamp, creationTimestamp, locked);
                note.setPlaintextLength(plaintextLength);

                notes.add(note);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return notes;
    }

  
    private void saveAllNotes(List<Note> notes) {

        JSONArray jsonArray = new JSONArray();

        for (Note note : notes) {
            try {
                JSONObject obj = new JSONObject();

                obj.put("id", note.getId());
                obj.put("title", note.getTitle());
                obj.put("content", note.getContent());
                obj.put("timestamp", note.getTimestamp());
                obj.put("creationTimestamp", note.getCreationTimestamp());
                obj.put("locked", note.isLocked());

                obj.put("plaintextLength", note.getPlaintextLength());

                jsonArray.put(obj);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        prefs.edit().putString(NOTES_KEY, jsonArray.toString()).apply();
    }


    public boolean encryptNote(String id, String password) {
        List<Note> notes = getAllNotes();

        for (Note note : notes) {
            if (note.getId().equals(id)) {

                if (note.isLocked()) return true; 

                try {
                    String encrypted = EncryptionManager.encrypt(note.getContent(), password);
                    note.setContent(encrypted);
                    note.setLocked(true);

                    saveAllNotes(notes);
                    return true;

                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }

        return false;
    }


    public String decryptNote(String id, String password) {
        List<Note> notes = getAllNotes();

        for (Note note : notes) {
            if (note.getId().equals(id)) {

                if (!note.isLocked()) {
                    return note.getContent(); 
                }

                try {
                    String decrypted = EncryptionManager.decrypt(note.getContent(), password);
                    note.setContent(decrypted);
                    note.setLocked(false);

                    int ptLen = Html.fromHtml(decrypted).toString().length();
                    note.setPlaintextLength(ptLen);

                    saveAllNotes(notes);

                    return decrypted;

                } catch (Exception e) {
                    return null; 
                }
            }
        }

        return null;
    }
}
