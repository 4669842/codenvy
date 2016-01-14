/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.workspace.activity;

import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import com.codenvy.workspace.event.WsActivityEvent;

import org.mockito.ArgumentMatcher;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Listeners(value = {MockitoTestNGListener.class})
public class WsActivityEventSenderTest {
    private EventService eventService;

    private WsActivityEventSender wsActivityEventSender;

    private EventSubscriber<WsActivityEvent> eventSubscriber;

    @BeforeMethod
    public void setUp() throws Exception {
        eventService = new EventService();
        wsActivityEventSender = new WsActivityEventSender(eventService);

        eventSubscriber = spy(new EventSubscriber<WsActivityEvent>() {
            @Override
            public void onEvent(WsActivityEvent event) {
            }
        });

        eventService.subscribe(eventSubscriber);
    }

    @Test
    public void shouldSendEventIfThisIsFirstAccessToWs() throws InterruptedException {

        wsActivityEventSender.onActivity("id", true);

        Thread.sleep(500);

        verify(eventSubscriber, times(1)).onEvent(argThat(new ArgumentMatcher<WsActivityEvent>() {
            @Override
            public boolean matches(Object argument) {
                WsActivityEvent event = (WsActivityEvent)argument;
                return event.isTemporary() && "id".equals(event.getWorkspaceId());
            }
        }));
    }

    @Test(enabled = false)
    public void shouldSendEventIfLastAccessToWsWasMoreThan60SecondsAgo() throws Exception {

        setFinalStatic(WsActivityEventSender.class.getField("ACTIVITY_PERIOD"), Integer.valueOf(1000));

        wsActivityEventSender.onActivity("id", true);

        Thread.sleep(2000);

        wsActivityEventSender.onActivity("id", true);

        Thread.sleep(500);

        verify(eventSubscriber, times(2)).onEvent(argThat(new ArgumentMatcher<WsActivityEvent>() {
            @Override
            public boolean matches(Object argument) {
                WsActivityEvent event = (WsActivityEvent)argument;
                return event.isTemporary() && "id".equals(event.getWorkspaceId());
            }
        }));
    }

    @Test
    public void shouldNotSendEventIfLastAccessToWsWasLessThan60SecondsAgo() {
        wsActivityEventSender.onActivity("id", true);
        wsActivityEventSender.onActivity("id", true);

        verify(eventSubscriber, times(1)).onEvent(argThat(new ArgumentMatcher<WsActivityEvent>() {
            @Override
            public boolean matches(Object argument) {
                WsActivityEvent event = (WsActivityEvent)argument;
                return event.isTemporary() && "id".equals(event.getWorkspaceId());
            }
        }));
    }

    static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);

        // remove final modifier from field
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, newValue);
    }
}
