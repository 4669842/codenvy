/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
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
package com.codenvy.api.account.subscription.saas.limit;

import com.codenvy.api.account.AccountLocker;
import com.codenvy.api.account.billing.BillingService;
import com.codenvy.api.account.billing.MonthlyBillingPeriod;
import com.codenvy.api.account.billing.ResourcesFilter;
import com.codenvy.api.account.impl.shared.dto.AccountResources;
import com.codenvy.api.account.server.dao.Account;
import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.account.server.dao.Subscription;
import com.codenvy.api.account.subscription.ServiceId;
import com.codenvy.api.core.notification.EventService;
import com.codenvy.api.runner.internal.RunnerEvent;
import com.codenvy.api.workspace.server.dao.Workspace;
import com.codenvy.api.workspace.server.dao.WorkspaceDao;
import com.codenvy.dto.server.DtoFactory;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


@Listeners(MockitoTestNGListener.class)
public class CheckRemainResourcesOnStopSubscriberTest {
    private static final long   PROCESS_ID = 1L;
    private static final Double FREE_LIMIT = 100D;
    private static final String WS_ID      = "workspaceId";
    private static final String ACC_ID     = "accountId";

    @Mock
    EventService    eventService;
    @Mock
    WorkspaceDao    workspaceDao;
    @Mock
    AccountDao      accountDao;
    @Mock
    BillingService  service;
    @Mock
    ActiveRunHolder activeRunHolder;
    @Mock
    AccountLocker   accountLocker;

    CheckRemainResourcesOnStopSubscriber subscriber;

    @BeforeMethod
    public void setUp() throws Exception {
        subscriber = new CheckRemainResourcesOnStopSubscriber(eventService, workspaceDao, accountDao, service,
                                                              activeRunHolder, new MonthlyBillingPeriod(), accountLocker);

        when(workspaceDao.getById(anyString())).thenReturn(new Workspace().withAccountId(ACC_ID)
                                                                          .withId(WS_ID));

        when(accountDao.getById(anyString())).thenReturn(new Account().withId(ACC_ID));
    }


    @Test
    public void shouldAddEventOnRunStarted() throws Exception {
        subscriber.onEvent(RunnerEvent.startedEvent(PROCESS_ID, WS_ID, "/project"));

        verify(activeRunHolder, times(1)).addRun(any(RunnerEvent.class));
    }

    @Test
    public void shouldAddEventOnRunStopped() throws Exception {
        Subscription subscription = Mockito.mock(Subscription.class);
        when(subscription.getPlanId()).thenReturn("Super-Pupper-Plan");
        when(accountDao.getActiveSubscription(eq(ACC_ID), eq(ServiceId.SAAS))).thenReturn(subscription);
        when(accountDao.getById(anyString())).thenReturn(new Account().withId(ACC_ID).withAttributes(new HashMap<String, String>()));

        subscriber.onEvent(RunnerEvent.stoppedEvent(PROCESS_ID, WS_ID, "/project"));

        verify(activeRunHolder, times(1)).removeRun(any(RunnerEvent.class));
    }

    @Test
    public void shouldNotUpdateAccountAndWorkspacesIfResourcesAreLeft() throws Exception {
        when(service.getEstimatedUsage((ResourcesFilter)anyObject())).thenReturn(Collections.<AccountResources>emptyList());
        when(workspaceDao.getByAccount(anyString())).thenReturn(Arrays.asList(new Workspace().withAccountId(ACC_ID)
                                                                                             .withId(WS_ID)));

        subscriber.onEvent(RunnerEvent.stoppedEvent(PROCESS_ID, WS_ID, "/project"));

        verifyZeroInteractions(accountLocker);
    }

    @Test
    public void shouldUpdateAccountAndWorkspacesIfNoResourcesLeft() throws Exception {
        when(service.getEstimatedUsage((ResourcesFilter)anyObject()))
                .thenReturn(Arrays.asList(DtoFactory.getInstance().createDto(AccountResources.class)));
        when(workspaceDao.getByAccount(anyString())).thenReturn(Arrays.asList(new Workspace().withAccountId(ACC_ID)
                                                                                             .withId(WS_ID)));

        subscriber.onEvent(RunnerEvent.stoppedEvent(PROCESS_ID, WS_ID, "/project"));

        verify(accountLocker).lockAccountResources(eq(ACC_ID));
    }


}
