package com.github.mike10004.socialapidemo;

public interface ErrorReactor {

    <X extends Exception> void react(X exception) throws X;

    static ErrorReactor rethrower() {
        return new ErrorReactor() {
            @Override
            public <X extends Exception> void react(X exception) throws X {
                throw exception;
            }
        };
    }

}
