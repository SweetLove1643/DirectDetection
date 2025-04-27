package com.sweetlove.directdetection.Controller;

public class Notification {
    private String content;
    private String time;

    public Notification(String content, String time) {
        this.content = content;
        this.time = time;
    }

    public String getContent() {
        return content;
    }

    public String getTime() {
        return time;
    }
}