package com.furkan.scheduler.ingest;

public class ScheduleFetchException extends RuntimeException {
    public ScheduleFetchException(String message) { super(message); }
    public ScheduleFetchException(String message, Throwable cause) { super(message, cause); }
}