package org.eclipse.edc.mvd.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.mvd.model.*;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataExchangeQueueManagerTest {

    private AutoCloseable closeable;
    private DataExchangeQueueManager queueManager;
    @Mock
    private HttpClient httpClient;
    @Mock
    private Monitor monitor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        queueManager = new DataExchangeQueueManager(objectMapper, httpClient, monitor);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void testAddProviderNotification_NewEntry() {
        Participant provider = new Participant("did:example:provider", "Provider", "http://provider.com");
        List<String> assets = List.of("asset1", "asset2");

        String entryId = queueManager.addProviderNotification(provider, assets);

        List<DataExchangeEntry> entries = queueManager.getEntries();
        assertEquals(1, entries.size());

        DataExchangeEntry entry = entries.get(0);
        assertEquals(entryId, entry.getId());
        assertEquals(provider, entry.getProvider());
        assertNull(entry.getConsumer());
        assertEquals(assets, entry.getAssets());
        assertEquals(DataExchangeState.NOT_READY, entry.getState());
    }

    @Test
    void testAddConsumerNotification_ExistingEntry() {
        Participant provider = new Participant("did:example:provider", "Provider", "http://provider.com");
        Participant consumer = new Participant("did:example:consumer", "Consumer", "http://consumer.com");
        List<String> assets = List.of("asset1", "asset2");

        //add provider notification
        queueManager.addProviderNotification(provider, assets);
        //add consumer notification
        String entryId = queueManager.addConsumerNotification(consumer, assets);

        List<DataExchangeEntry> entries = queueManager.getEntries();
        assertEquals(1, entries.size());

        DataExchangeEntry entry = entries.get(0);
        assertEquals(entryId, entry.getId());
        assertEquals(provider, entry.getProvider());
        assertEquals(consumer, entry.getConsumer());
        assertEquals(assets, entry.getAssets());
        assertEquals(DataExchangeState.READY, entry.getState());
    }

    @Test
    void testProcessEntries_Timeout() throws Exception {
        Participant provider = new Participant("did:example:provider", "Provider", "http://provider.com");
        List<String> assets = List.of("asset1", "asset2");

        queueManager.addProviderNotification(provider, assets);

        // Simulate time passing using reflection
        DataExchangeEntry entry = queueManager.getEntries().get(0);

        Field lastUpdatedAtField = DataExchangeEntry.class.getDeclaredField("lastUpdatedAt");
        lastUpdatedAtField.setAccessible(true);
        lastUpdatedAtField.set(entry, LocalDateTime.now().minusSeconds(10));

        queueManager.processEntries();

        assertEquals(DataExchangeState.FAILED, entry.getState());
        verify(monitor).info(contains("Entry has FAILED due to timeout"));
    }

    @Test
    void testUpdateEntryStateManually_Success() {
        Participant provider = new Participant("did:example:provider", "Provider", "http://provider.com");
        Participant consumer = new Participant("did:example:consumer", "Consumer", "http://consumer.com");
        List<String> assets = List.of("asset1", "asset2");

        // Both notifications received
        queueManager.addProviderNotification(provider, assets);
        String entryId = queueManager.addConsumerNotification(consumer, assets);

        boolean updated = queueManager.updateEntryStateManually(entryId, DataExchangeState.IN_PROGRESS);

        assertTrue(updated);

        DataExchangeEntry entry = queueManager.getEntries().get(0);
        assertEquals(DataExchangeState.IN_PROGRESS, entry.getState());
    }

    @Test
    void testSendCompletionNotification() {
        Participant provider = new Participant("did:example:provider", "Provider", "http://provider.com");
        Participant consumer = new Participant("did:example:consumer", "Consumer", "http://consumer.com");
        List<String> assets = List.of("asset1", "asset2");

        // Both notifications received
        queueManager.addProviderNotification(provider, assets);
        queueManager.addConsumerNotification(consumer, assets);

        DataExchangeEntry entry = queueManager.getEntries().get(0);
        entry.setState(DataExchangeState.COMPLETED);

        queueManager.sendCompletionNotification(entry);

        //verify that HTTP requests were made to the correct URLs
        verify(monitor).info(contains("Completion Notification send to provider"));
        verify(monitor).info(contains("Completion Notification send to consumer"));
    }
}