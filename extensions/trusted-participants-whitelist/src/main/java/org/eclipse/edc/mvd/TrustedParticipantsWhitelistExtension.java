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

import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.mvd.model.InMemoryMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.io.IOException;


/**
 * Extension to maintain trusted participants.
 */
public class TrustedParticipantsWhitelistExtension implements ServiceExtension {

  @Inject
  WebService webService;

  @Override
  public String name() {
    return "Maintain trusted participants.";
  }

  @Override
  public void initialize(ServiceExtensionContext context) {
    Monitor originalMonitor = context.getMonitor();
    HttpClient httpClient = HttpClient.newHttpClient();
    context.registerService(HttpClient.class, httpClient);
    ObjectMapper objectMapper = new ObjectMapper();
    InMemoryMonitor inMemoryMonitor = new InMemoryMonitor(originalMonitor);
    webService.registerResource(new TrustedParticipantsWhitelistApiController(inMemoryMonitor, objectMapper, httpClient));
  }
}
