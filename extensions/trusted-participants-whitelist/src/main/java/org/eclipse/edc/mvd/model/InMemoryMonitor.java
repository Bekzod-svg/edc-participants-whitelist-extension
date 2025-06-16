package org.eclipse.edc.mvd.model;

import org.eclipse.edc.spi.monitor.Monitor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class InMemoryMonitor implements Monitor {

    private final Monitor delegate;
    private final List<LogEntry> logEntries = new ArrayList<>();
    private static final int MAX_LOG_ENTRIES = 1000;

    public InMemoryMonitor(Monitor delegate) {
        this.delegate = delegate;
    }

    @Override
    public void severe(String message, Throwable... errors) {
        delegate.severe(message, errors);
        log("SEVERE", message, errors);
    }

    @Override
    public void severe(Supplier<String> supplier, Throwable... errors) {
        delegate.severe(supplier, errors);
        String message = supplier.get();
        log("SEVERE", message, errors);
    }

    @Override
    public void severe(java.util.Map<String, Object> data) {
        delegate.severe(data);
        log("SEVERE", data.toString());
    }

    @Override
    public void warning(String message, Throwable... errors) {
        delegate.warning(message, errors);
        log("WARNING", message, errors);
    }

    @Override
    public void warning(Supplier<String> supplier, Throwable... errors) {
        delegate.warning(supplier, errors);
        String message = supplier.get();
        log("WARNING", message, errors);
    }

    @Override
    public void info(String message, Throwable... errors) {
        delegate.info(message, errors);
        log("INFO", message, errors);
    }

    @Override
    public void info(Supplier<String> supplier, Throwable... errors) {
        delegate.info(supplier, errors);
        String message = supplier.get();
        log("INFO", message, errors);
    }

    @Override
    public void debug(String message, Throwable... errors) {
        delegate.debug(message, errors);
        log("DEBUG", message, errors);
    }

    @Override
    public void debug(Supplier<String> supplier, Throwable... errors) {
        delegate.debug(supplier, errors);
        String message = supplier.get();
        log("DEBUG", message, errors);
    }

    private void log(String level, String message, Throwable... errors) {
        StringBuilder fullMessage = new StringBuilder(message);
        if (errors != null && errors.length > 0) {
            for (Throwable error : errors) {
                fullMessage.append("\nException: ").append(error.toString());
            }
        }
        LogEntry entry = new LogEntry(LocalDateTime.now(), level, fullMessage.toString());
        synchronized (logEntries) {
            logEntries.add(entry);
            if (logEntries.size() > MAX_LOG_ENTRIES) {
                logEntries.remove(0); // Remove the oldest entry
            }
        }
    }

    // Overloaded log method for Map data
    private void log(String level, String message) {
        LogEntry entry = new LogEntry(LocalDateTime.now(), level, message);
        synchronized (logEntries) {
            logEntries.add(entry);
            if (logEntries.size() > MAX_LOG_ENTRIES) {
                logEntries.remove(0);
            }
        }
    }

    public List<LogEntry> getLogEntries() {
        synchronized (logEntries) {
            return new ArrayList<>(logEntries);
        }
    }

    public static class LogEntry {
        private final LocalDateTime timestamp;
        private final String level;
        private final String message;

        public LogEntry(LocalDateTime timestamp, String level, String message) {
            this.timestamp = timestamp;
            this.level = level;
            this.message = message;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public String getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }
    }
}