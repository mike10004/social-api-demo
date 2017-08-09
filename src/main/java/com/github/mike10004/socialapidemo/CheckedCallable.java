package com.github.mike10004.socialapidemo;

interface CheckedCallable<T, X extends Exception> {
    T call() throws X;
}
