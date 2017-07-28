package com.github.mike10004.socialapidemo;

public class ExerciseException extends RuntimeException {

    public ExerciseException(String message) {
        super(message);
    }

    public ExerciseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExerciseException(Throwable cause) {
        super(cause);
    }
}
