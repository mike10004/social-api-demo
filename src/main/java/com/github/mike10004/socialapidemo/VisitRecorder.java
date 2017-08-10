package com.github.mike10004.socialapidemo;

import java.util.HashSet;
import java.util.Set;

public interface VisitRecorder {

    /**
     * Records a visit and reveals whether this is a new visit.
     * @param identifier identifier
     * @return true iff this is a new visit, meaning the identifier was not already visited
     */
    boolean recordVisit(String identifier) throws VisitRecorderException;

    @SuppressWarnings("unused")
    class VisitRecorderException extends CrawlerException {

        public VisitRecorderException(String message) {
            super(message);
        }

        public VisitRecorderException(String message, Throwable cause) {
            super(message, cause);
        }

        public VisitRecorderException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Creates and returns a visit recorder that records visits in memory.
     * @return a new recorder instance
     */
    static MemoryVisitRecorder inMemory() {
        return new MemoryVisitRecorder() {

        };
    }

    class MemoryVisitRecorder implements VisitRecorder {

        private final Set<String> visited = new HashSet<>();

        @Override
        public boolean recordVisit(String identifier) throws VisitRecorderException {
            return visited.add(identifier);
        }

        public void reset() {
            visited.clear();
        }
    }
}
