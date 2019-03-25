/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.saga.omega.transaction;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.apache.servicecomb.saga.common.EventType;
import org.apache.servicecomb.saga.omega.context.CallbackContext;
import org.apache.servicecomb.saga.omega.idempotency.IdempotencyManager;
import org.junit.Before;
import org.junit.Test;

public class CompensationMessageHandlerTest {

  private final List<TxEvent> events = new ArrayList<>();
  private final SagaMessageSender sender = new SagaMessageSender() {
    @Override
    public void onConnected() {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void close() {

    }

    @Override
    public String target() {
      return "UNKNOWN"; }

    @Override
    public AlphaResponse send(TxEvent event) {
      events.add(event);
      return new AlphaResponse(false);
    }
  };

  private final String globalTxId = uniquify("globalTxId");
  private final String localTxId = uniquify("localTxId");
  private final String parentTxId = uniquify("parentTxId");

  private final String compensationMethod = getClass().getCanonicalName();
  private final String payload = uniquify("blah");

  private final CallbackContext context = mock(CallbackContext.class);

  private final IdempotencyManager idempotencyManager=mock(IdempotencyManager.class);

  private final CompensationMessageHandler handler = new CompensationMessageHandler(sender, context,idempotencyManager);

  @Before
  public void setUp() {
    events.clear();
  }

  @Test
  public void sendsCompensatedEventOnCompensationCompleted() {
    handler.onReceive(globalTxId, localTxId, parentTxId, compensationMethod, payload);

    assertThat(events.size(), is(1));

    TxEvent event = events.get(0);
    assertThat(event.globalTxId(), is(globalTxId));
    assertThat(event.localTxId(), is(localTxId));
    assertThat(event.parentTxId(), is(parentTxId));
    assertThat(event.type(), is(EventType.TxCompensatedEvent));
    assertThat(event.compensationMethod(), is(getClass().getCanonicalName()));
    assertThat(event.payloads().length, is(0));

    verify(context).apply(globalTxId, localTxId, compensationMethod, payload);
  }
}
