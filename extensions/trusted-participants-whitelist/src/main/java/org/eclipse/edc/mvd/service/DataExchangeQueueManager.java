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

    public List<DataExchangeEntry> getEntries() {
        return new ArrayList<>(queue);
    }

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
        if (entry.getProvider() != null && entry.getConsumer() != null) {
            entry.setState(DataExchangeState.READY);
            System.out.println("Second notification received from " + entry.getConsumer().getName() +
                    ", conditions ready for data exchange.");
        } else {
            System.out.println("First notification received from " +
                    (entry.getProvider() != null ? entry.getProvider().getName() : entry.getConsumer().getName()) +
                    ", waiting for second notification...");
            entry.setState(DataExchangeState.NOT_READY);
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
                    }
                    break;

                case READY:
                    // No automatic transition
                    break;

                case IN_PROGRESS:
                    System.out.println("Data exchange IN_PROGRESS for entry: " + entry);
                    break;

                case COMPLETED:
                    System.out.println("Data exchange COMPLETED for entry: " + entry);
                case FAILED:
                    System.out.println("Entry FAILED: " + entry);
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

    // method to manually update the state via API
    public boolean updateEntryStateManually(String entryId, DataExchangeState newState) {
        for (DataExchangeEntry entry : queue) {
            if (entry.getId().equals(entryId) && entry.getState() == DataExchangeState.READY) {
                entry.setState(newState);
                System.out.println("State manually updated to " + newState + " for entry: " + entry);
                return true;
            }
        }
        System.out.println("Entry not found or not in READY state.");
        return false;
    }


}