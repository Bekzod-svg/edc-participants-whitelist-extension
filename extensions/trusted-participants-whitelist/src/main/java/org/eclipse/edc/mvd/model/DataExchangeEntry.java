package org.eclipse.edc.mvd.model;

import java.time.LocalDateTime;
import java.util.List;


public class DataExchangeEntry {
    private Participant provider;
    private Participant consumer;
    private List<String> assets;
    private DataExchangeState state;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;

    public DataExchangeEntry(Participant provider, Participant consumer, List<String> assets) {
        this.provider = provider;
        this.consumer = consumer;
        this.assets = assets;
        this.state = DataExchangeState.NOT_READY; // Start at NOT_READY
        this.createdAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
    }

    // Getters and setters

    public Participant getProvider() {
        return provider;
    }

    public void setProvider(Participant provider) {
        this.provider = provider;
        updateLastUpdatedAt();
    }

    public Participant getConsumer() {
        return consumer;
    }

    public void setConsumer(Participant consumer) {
        this.consumer = consumer;
        updateLastUpdatedAt();
    }

    public List<String> getAssets() {
        return assets;
    }

    public void setAssets(List<String> assets) {
        this.assets = assets;
        updateLastUpdatedAt();
    }

    public DataExchangeState getState() {
        return state;
    }

    public void setState(DataExchangeState state) {
        this.state = state;
        updateLastUpdatedAt();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void updateLastUpdatedAt() {
        this.lastUpdatedAt = LocalDateTime.now();
    }
}