package com.example.fortnote;

public class Note {

    private final long creationTimestamp;
    private String id;
    private String title;
    private String content;
    private long timestamp;
    private boolean locked;

    private int plaintextLength = 0;

    public Note(String id, String title, String content, long timestamp,
                long creationTimestamp, boolean locked) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
        this.locked = locked;
        this.creationTimestamp = creationTimestamp;
    }

    public Note(String id, String title, String content, long timestamp,
                boolean locked, long creationTimestamp) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
        this.locked = locked;
        this.creationTimestamp = creationTimestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getCreationTimestamp() { return creationTimestamp; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public int getPlaintextLength() { return plaintextLength; }
    public void setPlaintextLength(int plaintextLength) {
        this.plaintextLength = plaintextLength;
    }
}
