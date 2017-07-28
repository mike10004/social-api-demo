package com.github.mike10004.socialapidemo;

@SuppressWarnings("unused")
public class ActivityException extends ExerciseException {
    public ActivityException(String message) {
        super(message);
    }

    public ActivityException(String message, Throwable cause) {
        super(message, cause);
    }

    public ActivityException(Throwable cause) {
        super(cause);
    }
}
