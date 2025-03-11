/*
 *  Copyright (c) 2024 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial implementation
 *
 */

package org.eclipse.edc.mvd;

import org.eclipse.edc.mvd.model.*;
import org.eclipse.edc.mvd.service.DataExchangeQueueManager;
import org.eclipse.edc.mvd.util.HashUtil;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.ws.rs.core.Response;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TrustedParticipantsWhitelistApiControllerTest {

    private AutoCloseable closeable;
    @Mock
    private Monitor monitor;
    @Mock
    private TrustedParticipantsWhitelist trustedList;
    @Mock
    private HttpClient httpClient;
    @Mock
    private DataExchangeQueueManager queueManager;
    private TrustedParticipantsWhitelistApiController controller;

    @BeforeEach
    void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        // Use reflection to inject the mocked singleton instance
        Field instance = TrustedParticipantsWhitelist.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, trustedList);
        // Now when the controller calls getInstance, it will receive the mock

        // Instantiate the controller with mocked dependencies
        controller = new TrustedParticipantsWhitelistApiController(monitor);

        // Inject mocks into the controller
        Field httpClientField = TrustedParticipantsWhitelistApiController.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(controller, httpClient);

        Field queueManagerField = TrustedParticipantsWhitelistApiController.class.getDeclaredField("queueManager");
        queueManagerField.setAccessible(true);
        queueManagerField.set(controller, queueManager);
    }

    @AfterEach
    void releaseMocks() throws Exception {
        closeable.close();
    }

    @Test
    void testCheckHealth() {
        String healthStatus = controller.checkHealth();
        assertEquals("{\"response\":\"Web server running on Connector and ready for requests\"}", healthStatus);
        verify(monitor).info("Received a health request");
    }

    @Test
    void testAddTrustedParticipant() {
        Participant participant = new Participant("did:example:123456789abcdefghi", "testParticipant", "http://example.com");
        when(trustedList.addTrustedParticipant(participant)).thenReturn(true);
        String response = controller.addTrustedParticipant(participant);
        verify(trustedList).addTrustedParticipant(participant);
        verify(monitor).info("Adding trusted participant: " + participant.getName());
        assertEquals("{\"response\":\"Participant added successfully\"}", response);
    }

    @Test
    void testAddExistingTrustedParticipant() {
        Participant participant = new Participant("did:example:123456789abcdefghi", "testParticipant", "http://example.com");
        when(trustedList.addTrustedParticipant(participant)).thenReturn(false);
        String response = controller.addTrustedParticipant(participant);
        verify(trustedList).addTrustedParticipant(participant);
        verify(monitor).info("Adding trusted participant: " + participant.getName());
        assertEquals("{\"response\":\"Participant already exists\"}", response);
    }

    @Test
    void testGetTrustedParticipants() throws NoSuchAlgorithmException {
        List<Participant> expectedParticipants = List.of(
                new Participant("did:example:123456789abcdefghi", "testParticipant1", "http://example.com"),
                new Participant("did:example:123456789jklmnopqr", "testParticipant2", "http://example.com")
        );
        when(trustedList.getTrustedParticipants()).thenReturn(expectedParticipants);
        TrustedParticipantsResponse response = controller.getTrustedParticipants();
        String expectedHash = HashUtil.computeHash(expectedParticipants);
        verify(trustedList).getTrustedParticipants();
        verify(monitor).info("Retrieving trusted participants");
        assertEquals(expectedParticipants, response.participants());
        assertEquals(expectedHash, response.hash());
    }

    @Test
    void testRemoveTrustedParticipant() {
        Participant participant = new Participant("did:example:123456789abcdefghi", "testParticipant", "http://example.com");
        when(trustedList.removeTrustedParticipant(participant)).thenReturn(true);
        String response = controller.removeTrustedParticipant(participant);
        verify(trustedList).removeTrustedParticipant(participant);
        verify(monitor).info("Removing trusted participant: " + participant.getName());
        assertEquals("{\"response\":\"Participant removed successfully\"}", response);
    }


    @Test
    void testReceiveNegotiation() throws Exception {
        List<Participant> trustedParticipants = List.of(
                new Participant("did:example:1", "DataTrustee1", "http://datatrustee1.com"),
                new Participant("did:example:2", "DataTrustee2", "http://datatrustee2.com")
        );
        String hash = HashUtil.computeHash(trustedParticipants);

        NegotiationRequest negotiationRequest = new NegotiationRequest(
                new Participant("did:example:source", "Provider", "http://provider.com"),
                new Participant("did:example:sink", "Consumer", "http://consumer.com"),
                trustedParticipants,
                List.of("asset1", "asset2"),
                hash
        );

        when(trustedList.getTrustedParticipants()).thenReturn(trustedParticipants);

        String response = controller.receiveNegotiation(negotiationRequest);

        verify(monitor).info("Received negotiation request");

        // Additional assertions can be added based on the expected behavior
        assertNotNull(response);
    }

    @Test
    void testReceiveNotification() {
        DataTrusteeRequest request = new DataTrusteeRequest(
                new Participant("did:example:source", "Provider", "http://provider.com"),
                new Participant("did:example:sink", "Consumer", "http://consumer.com"),
                List.of("asset1", "asset2"),
                "provider"
        );

        when(queueManager.addProviderNotification(any(), any())).thenReturn("entryId");
        Response response = controller.receiveNotification(request);

        verify(monitor).info("Received notification: " + request);

        assertEquals(200, response.getStatus());
        Map<String, String> responseBody = (Map<String, String>) response.getEntity();
        assertEquals("Notification received", responseBody.get("message"));
        assertEquals("entryId", responseBody.get("entryId"));
    }

    @Test
    void testReceiveNotificationCompletion() {
        Map<String, String> notification = Map.of(
                "message", "Data exchange completed",
                "role", "provider"
        );

        Response response = controller.receiveNotificationCompletion(notification);

        verify(monitor).info("Received completion notification for role: " + "provider" + ". Message: " + "Data exchange completed");
        assertEquals(200, response.getStatus());
        assertEquals("{\"message\":\"Completion notification received.\"}", response.getEntity());
    }

    @Test
    void testUpdateDataExchangeEntryState() {
        when(queueManager.updateEntryStateManually("entryId", DataExchangeState.COMPLETED)).thenReturn(true);
        Response response = controller.updateDataExchangeEntryState("entryId", "COMPLETED");
        verify(monitor).info("Received request to update state of entry entryId to COMPLETED");
        assertEquals(200, response.getStatus());
        assertEquals("{\"message\":\"State updated successfully.\"}", response.getEntity());
    }

    @Test
    void testGetDataExchangeEntries() {
        List<DataExchangeEntry> entries = List.of(
                new DataExchangeEntry(new Participant("did:example:1", "Provider", "http://provider.com"),
                        new Participant("did:example:2", "Consumer", "http://consumer.com"),
                        List.of("asset1", "asset2"))
        );
        when(queueManager.getEntries()).thenReturn(entries);

        Response response = controller.getDataExchangeEntries();
        verify(monitor).info("Retrieving current DataExchangeEntries");
        assertEquals(200, response.getStatus());
        assertNotNull(response.getEntity());
    }
}