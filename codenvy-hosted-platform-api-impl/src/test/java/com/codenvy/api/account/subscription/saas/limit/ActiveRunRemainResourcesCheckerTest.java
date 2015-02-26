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

import com.codenvy.api.account.billing.BillingService;
import com.codenvy.api.account.billing.MonthlyBillingPeriod;
import com.codenvy.api.account.billing.ResourcesFilter;
import com.codenvy.api.account.impl.shared.dto.AccountResources;
import com.codenvy.api.account.server.dao.Account;
import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.account.server.dao.Subscription;
import com.codenvy.api.account.subscription.ServiceId;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.runner.RunQueue;
import com.codenvy.api.runner.RunQueueTask;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for ActiveRunRemainResourcesChecker.
 *
 * @author Max Shaposhnik (mshaposhnik@codenvy.com) on 1/19/15.
 */
@Listeners(MockitoTestNGListener.class)
public class ActiveRunRemainResourcesCheckerTest {

    private ActiveRunRemainResourcesChecker checker;

    private static final String ACC_ID     = "accountId";
    private static final long   PROCESS_ID = 1L;

    @Mock
    ActiveRunHolder activeRunHolder;
    @Mock
    AccountDao      accountDao;
    @Mock
    BillingService  service;
    @Mock
    RunQueue        runQueue;
    @Mock
    RunQueueTask    runQueueTask;


    @BeforeMethod
    public void setUp() throws Exception {
        this.checker = new ActiveRunRemainResourcesChecker(activeRunHolder, accountDao, service, runQueue, new MonthlyBillingPeriod());
        Map<String, Set<Long>> activeRuns = new HashMap<>();
        Set<Long> pIds = new HashSet<>();
        pIds.add(PROCESS_ID);
        activeRuns.put(ACC_ID, pIds);
        when(activeRunHolder.getActiveRuns()).thenReturn(activeRuns);
    }

    @Test
    public void shouldNotCheckOnPaidAccounts() throws ServerException, NotFoundException {
        //given
        Subscription subscription = Mockito.mock(Subscription.class);
        when(subscription.getPlanId()).thenReturn("Super-Pupper-Plan");
        when(accountDao.getActiveSubscription(eq(ACC_ID), eq(ServiceId.SAAS))).thenReturn(subscription);

        //when
        checker.run();

        //then
        verifyZeroInteractions(service);
        verifyZeroInteractions(runQueue);
    }

    @Test
    public void shouldStopRunIfLimitExeeded() throws Exception {
        when(accountDao.getById(anyString())).thenReturn(new Account().withId(ACC_ID));
        when(service.getEstimatedUsageByAccount((ResourcesFilter)anyObject())).thenReturn(Arrays.asList(
                DtoFactory.getInstance().createDto(AccountResources.class)));
        when(runQueue.getTask(anyLong())).thenReturn(runQueueTask);

        checker.run();
        verify(runQueueTask, times(1)).stop();
    }

    @Test
    public void shouldNotStopRunIfLimitNotExeeded() throws Exception {
        when(accountDao.getById(anyString())).thenReturn(new Account().withId(ACC_ID));
        when(service.getEstimatedUsageByAccount((ResourcesFilter)anyObject())).thenReturn(Collections.<AccountResources>emptyList());
        when(runQueue.getTask(anyLong())).thenReturn(runQueueTask);

        verifyZeroInteractions(runQueue);
    }

}
