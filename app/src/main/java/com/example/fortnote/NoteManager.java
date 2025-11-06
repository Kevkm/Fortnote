package com.example.fortnote;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import android.util.Base64;

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
        long timestamp = System.currentTimeMillis();
        Note newNote = new Note(id, title, content, timestamp, false);
        notes.add(0, newNote);
        saveAllNotes(notes);
    }

    public void updateNote(String id, String title, String content) {
        List<Note> notes = getAllNotes();
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).getId().equals(id)) {
                notes.get(i).setTitle(title);
                notes.get(i).setContent(content);
                notes.get(i).setTimestamp(System.currentTimeMillis());
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
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String id = jsonObject.getString("id");
                String title = jsonObject.getString("title");
                String content = jsonObject.getString("content");
                long timestamp = jsonObject.getLong("timestamp");
                boolean locked = jsonObject.has("locked") && jsonObject.getBoolean("locked");
                Note note = new Note(id, title, content, timestamp, locked);
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
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", note.getId());
                jsonObject.put("title", note.getTitle());
                jsonObject.put("content", note.getContent());
                jsonObject.put("timestamp", note.getTimestamp());
                jsonObject.put("locked", note.isLocked()); // persist lock state
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        prefs.edit().putString(NOTES_KEY, jsonArray.toString()).apply();
    }

    // Encrypt ALL notes that aren't already encrypted; mark as locked
    public void encryptAllNotes(String password) {
        List<Note> notes = getAllNotes();
        for (Note note : notes) {
            try {
                if (!note.isLocked() || !isProbablyEncrypted(note.getContent())) {
                    String encrypted = EncryptionManager.encrypt(note.getContent(), password);
                    note.setContent(encrypted);
                    note.setLocked(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        saveAllNotes(notes);
    }

    // Decrypt ALL notes; mark as unlocked; persist decrypted content
    public List<Note> getAllNotesDecrypted(String password) {
        List<Note> notes = getAllNotes();
        for (Note note : notes) {
            try {
                // If marked locked OR the content still looks like ciphertext, try to decrypt
                if (note.isLocked() || isProbablyEncrypted(note.getContent())) {
                    String decrypted = EncryptionManager.decrypt(note.getContent(), password);
                    note.setContent(decrypted);
                    note.setLocked(false);
                }
            } catch (Exception e) {
                note.setContent("**Decryption failed**");
                note.setLocked(true); // still locked if it failed
            }
        }
        saveAllNotes(notes); // persist decrypted results
        return notes;
    }

    // Heuristic: Base64 (no '<', '>', spaces), reasonably long
    private boolean isProbablyEncrypted(String content) {
        if (content == null) return false;
        String s = content.trim();
        if (s.isEmpty()) return false;
        if (s.contains("<") || s.contains(">") || s.contains(" ")) return false;
        if (!s.matches("^[A-Za-z0-9+/=]+$")) return false;
        try {
            byte[] decoded = Base64.decode(s, Base64.NO_WRAP);
            return decoded != null && decoded.length > 32; // > salt+iv length
        } catch (Exception e) {
            return false;
        }
    }
}
