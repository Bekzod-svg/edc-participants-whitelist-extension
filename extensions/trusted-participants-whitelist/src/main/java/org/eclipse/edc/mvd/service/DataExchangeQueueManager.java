package org.eclipse.edc.mvd.service;

import org.eclipse.edc.mvd.model.DataExchangeEntry;
import org.eclipse.edc.mvd.model.DataExchangeState;
import org.eclipse.edc.mvd.model.Participant;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DataExchangeQueueManager {
    private List<DataExchangeEntry> queue = new ArrayList<>();
    private static final Duration TIMEOUT_DURATION = Duration.ofSeconds(5);

    public void addProviderNotification(Participant provider, List<String> assets) {
        DataExchangeEntry entry = findOrCreateEntry(provider, null, assets);
        entry.setProvider(provider);
        updateEntryState(entry);
    }

    public void addConsumerNotification(Participant consumer, List<String> assets) {
        DataExchangeEntry entry = findOrCreateEntry(null, consumer, assets);
        entry.setConsumer(consumer);
        updateEntryState(entry);
    }

    private DataExchangeEntry findOrCreateEntry(Participant provider, Participant consumer, List<String> assets) {
        // Search for an existing entry with matching provider or consumer and assets
        for (DataExchangeEntry entry : queue) {
            boolean providerMatches = (provider == null || entry.getProvider() == null || entry.getProvider().equals(provider));
            boolean consumerMatches = (consumer == null || entry.getConsumer() == null || entry.getConsumer().equals(consumer));
            if (providerMatches && consumerMatches) {
                return entry;
            }
        }
        // Create a new entry if not found
        DataExchangeEntry newEntry = new DataExchangeEntry(provider, consumer, assets);
        queue.add(newEntry);
        return newEntry;
    }

    private void updateEntryState(DataExchangeEntry entry) {
//        System.out.println("Updating entry state for entry: " + entry);
//        System.out.println("Provider: " + entry.getProvider());
//        System.out.println("Consumer: " + entry.getConsumer());
//        System.out.println("Current State: " + entry.getState());
        if (entry.getProvider() != null && entry.getConsumer() != null) {
            entry.setState(DataExchangeState.READY);
            System.out.println("Entry is READY: " + entry);
        }else{
            System.out.println("Entry is NOT READY: " + entry);
        }
    }

    public void processEntries() {
        Iterator<DataExchangeEntry> iterator = queue.iterator();
        while (iterator.hasNext()) {
            DataExchangeEntry entry = iterator.next();

            switch (entry.getState()) {
                case NOT_READY:
                    if (hasTimedOut(entry)) {
                        entry.setState(DataExchangeState.FAILED);
                        System.out.println("Entry has FAILED due to timeout: " + entry);
                        // Optionally notify parties about the failure
                    }
                    break;

                case READY:
                    if (hasTimedOut(entry)) {
                        entry.setState(DataExchangeState.FAILED);
                        System.out.println("Entry has FAILED due to timeout: " + entry);
                        // Optionally notify parties about the failure
                    } else {
                        startDataExchange(entry);
                    }
                    break;

                case IN_PROGRESS:
                    System.out.println("Entry is IN_PROGRESS: " + entry);
                    // Implement any required checks during data exchange
                    break;

                case COMPLETED:
                    System.out.println("Entry is COMPLETED: " + entry);
                case FAILED:
                    System.out.println("Entry FAILED: " + entry);
                    // Remove completed or failed entries from the queue
                    iterator.remove();
                    break;

                default:
                    break;
            }
        }
    }

    private boolean hasTimedOut(DataExchangeEntry entry) {
        Duration duration = Duration.between(entry.getLastUpdatedAt(), LocalDateTime.now());
        return duration.compareTo(TIMEOUT_DURATION) > 0;
    }

    private void startDataExchange(DataExchangeEntry entry) {
        entry.setState(DataExchangeState.IN_PROGRESS);
        System.out.println("Starting data exchange for entry: " + entry);

        boolean success = initiateDataExchange(entry);
        if (success) {
            entry.setState(DataExchangeState.COMPLETED);
            System.out.println("Data exchange COMPLETED for entry: " + entry);
        } else {
            entry.setState(DataExchangeState.FAILED);
            System.out.println("Data exchange FAILED for entry: " + entry);
        }
    }

    private boolean initiateDataExchange(DataExchangeEntry entry) {
        // Implement the logic to start the data exchange between provider and consumer
        // This is a placeholder implementation
        try {
            // Simulate data exchange logic here
            System.out.println("Initiating data exchange between " + entry.getProvider().getName()
                    + " and " + entry.getConsumer().getName() + " for assets " + entry.getAssets());
            // Simulate success
            return true;
        } catch (Exception e) {
            // Log exception details
            System.err.println("Data exchange failed: " + e.getMessage());
            return false;
        }
    }
}