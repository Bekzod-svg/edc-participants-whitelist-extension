package org.eclipse.edc.mvd.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.mvd.model.DataExchangeEntry;
import org.eclipse.edc.mvd.model.DataExchangeState;
import org.eclipse.edc.mvd.model.Participant;

import org.eclipse.edc.spi.monitor.Monitor;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class DataExchangeQueueManager {
    private List<DataExchangeEntry> queue = new ArrayList<>();
    private static final Duration TIMEOUT_DURATION = Duration.ofSeconds(5);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Monitor monitor;

    public DataExchangeQueueManager(ObjectMapper objectMapper, HttpClient httpClient, Monitor monitor){
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.monitor = monitor;
    }

    public List<DataExchangeEntry> getEntries() {
        return new ArrayList<>(queue);
    }

    public String addProviderNotification(Participant provider, List<String> assets) {
        DataExchangeEntry entry = findOrCreateEntry(provider, null, assets);
        entry.setProvider(provider);
        updateEntryState(entry);
        return entry.getId();
    }

    public String addConsumerNotification(Participant consumer, List<String> assets) {
        DataExchangeEntry entry = findOrCreateEntry(null, consumer, assets);
        entry.setConsumer(consumer);
        updateEntryState(entry);
        return entry.getId();
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
                    sendCompletionNotification(entry);
                    iterator.remove();
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
            if (entry.getId().equals(entryId) && entry.getState() == DataExchangeState.READY || entry.getState() == DataExchangeState.IN_PROGRESS) {
                entry.setState(newState);
                System.out.println("State manually updated to " + newState + " for entry: " + entry);
                if(newState == DataExchangeState.COMPLETED){
                    sendCompletionNotification(entry);
                }
                return true;
            }
        }
        System.out.println("Entry not found or not in READY state.");
        return false;
    }

    public void sendCompletionNotification(DataExchangeEntry entry){
        Participant provider = entry.getProvider();
        Participant consumer = entry.getConsumer();
        String notificationMessage = "Data exchange has been completed for assets: " + entry.getAssets();

        try {
            if(provider != null && provider.getUrl() != null){
                String providerNotificationUrl = provider.getUrl() + "/notify-completion";
                Map<String, String> notification = new HashMap<>();
                notification.put("message", notificationMessage);
                notification.put("role", "provider");
                String requestBody = objectMapper.writeValueAsString(notification);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(providerNotificationUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                monitor.info("Completion Notification send to provider: " + provider.getName() + "; Response: " + response.body());
            }

            if(consumer != null && consumer.getUrl() != null){
                String consumerNotificationUrl = consumer.getUrl() + "/notify-completion";
                Map<String, String> notification = new HashMap<>();
                notification.put("message", notificationMessage);
                notification.put("role", "consumer");
                String requestBody = objectMapper.writeValueAsString(notification);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(consumerNotificationUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                monitor.info("Completion Notification send to consumer: " + consumer.getName() + "; Response: " + response.body());
            }
        }catch (Exception e){
            monitor.warning("Failed to send completion notifications: " + e.getMessage());
        }
    }


}