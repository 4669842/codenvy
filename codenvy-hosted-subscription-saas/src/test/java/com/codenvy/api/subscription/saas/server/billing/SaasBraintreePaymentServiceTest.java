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
package com.codenvy.api.subscription.saas.server.billing;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.Plan;
import com.braintreegateway.PlanGateway;
import com.braintreegateway.Result;
import com.braintreegateway.Transaction;
import com.braintreegateway.TransactionGateway;
import com.braintreegateway.TransactionRequest;
import com.braintreegateway.exceptions.BraintreeException;
import com.codenvy.api.subscription.saas.server.AccountLocker;
import com.codenvy.api.creditcard.shared.dto.CreditCard;
import com.codenvy.api.subscription.saas.server.service.util.SubscriptionMailSender;

import com.codenvy.api.creditcard.server.CreditCardDao;
import com.codenvy.api.subscription.server.dao.Subscription;
import com.codenvy.api.subscription.shared.dto.BillingCycleType;
import com.codenvy.api.subscription.shared.dto.SubscriptionState;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.commons.schedule.ScheduleDelay;
import org.eclipse.che.dto.server.DtoFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Tests for {@link SaasBraintreePaymentService}
 *
 * @author Alexander Garagatyi
 */
@Listeners(MockitoTestNGListener.class)
public class SaasBraintreePaymentServiceTest {
    private static final String SUBSCRIPTION_ID = "subscriptionId";
    private static final String PLAN_ID         = "planId";
    private static final String PAYMENT_TOKEN   = "ptoken";
    private static final Double PRICE           = 10D;

    @Mock
    private CreditCardDao                   creditCardDao;
    @Mock
    private BraintreeGateway                gateway;
    @Mock
    private PlanGateway                     planGateway;
    @Mock
    private Plan                            plan;
    @Mock
    private TransactionGateway              transactionGateway;
    @Mock
    private Result                          result;
    @Mock
    private EventService                    eventService;
    @Mock
    private Transaction                     transaction;
    @Mock
    private AccountLocker                   accountLocker;
    @Mock
    private com.braintreegateway.CreditCard creditCard;
    @Mock
    SubscriptionMailSender mailSender;

    @InjectMocks
    private SaasBraintreePaymentService service;

    @BeforeMethod
    public void setUp() throws Exception {
        prepareSuccessfulCharge();

        when(creditCardDao.getCards(anyString()))
                .thenReturn(Arrays.asList(DtoFactory.getInstance().createDto(CreditCard.class).withToken(PAYMENT_TOKEN)));
    }

    @Test
    public void shouldBeAbleToChargeSubscription() throws Exception {
        service.charge(createSubscription());

        verify(transactionGateway).sale(any(TransactionRequest.class));
    }

    @Test(expectedExceptions = ForbiddenException.class, expectedExceptionsMessageRegExp = "No subscription information provided")
    public void shouldThrowForbiddenExceptionIfSubscriptionToChargeIsNull() throws Exception {
        service.charge((Subscription)null);
    }

    @Test(expectedExceptions = ForbiddenException.class, expectedExceptionsMessageRegExp = "Subscription id required")
    public void shouldThrowForbiddenExceptionIfIdIsNull() throws Exception {
        service.charge(createSubscription().withId(null));
    }

    @Test(expectedExceptions = ForbiddenException.class, expectedExceptionsMessageRegExp = "Account hasn't credit card")
    public void shouldThrowForbiddenExceptionIfTokenIsNull() throws Exception {
        when(creditCardDao.getCards(anyString())).thenReturn(Collections.<CreditCard>emptyList());

        service.charge(createSubscription());
    }

    @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "Internal server error occurs. Please, contact " +
                                                                                        "support")
    public void shouldThrowServerExceptionIfPriceIsMissing() throws Exception {
        Field prices = SaasBraintreePaymentService.class.getDeclaredField("prices");
        prices.setAccessible(true);
        prices.set(service, Collections.emptyMap());

        service.charge(createSubscription());
    }

    @Test
    public void shouldDoNotChargeSubscriptionIfHimPriceEqualsTo0() throws Exception {
        Field prices = SaasBraintreePaymentService.class.getDeclaredField("prices");
        prices.setAccessible(true);
        prices.set(service, Collections.singletonMap(PLAN_ID, 0D));

        service.charge(createSubscription());

        verifyZeroInteractions(gateway);
    }

    @Test(expectedExceptions = ForbiddenException.class, expectedExceptionsMessageRegExp = "error message")
    public void shouldThrowConflictExceptionIfChargeWasUnsuccessful() throws Exception {
        when(result.isSuccess()).thenReturn(false);
        when(result.getMessage()).thenReturn("error message");

        service.charge(createSubscription());

        verify(transactionGateway).sale(any(TransactionRequest.class));
    }

    @Test(expectedExceptions = ServerException.class,
            expectedExceptionsMessageRegExp = "Internal server error occurs. Please, contact support")
    public void shouldThrowServerExceptionIfOtherExceptionOccurs() throws Exception {
        when(transactionGateway.sale(any(TransactionRequest.class))).thenThrow(new BraintreeException("message"));

        service.charge(createSubscription());

        verify(transactionGateway).sale(any(TransactionRequest.class));
    }

    @Test(timeOut = 1000)
    public void shouldHavePostConstructMethodWhichFillsPrices() throws Exception {
        when(gateway.plan()).thenReturn(planGateway);
        when(planGateway.all()).thenReturn(Arrays.asList(plan));
        when(plan.getId()).thenReturn("planId");
        when(plan.getPrice()).thenReturn(new BigDecimal(1));

        Method getPrices = SaasBraintreePaymentService.class.getDeclaredMethod("updatePrices");
        assertTrue(getPrices.isAnnotationPresent(ScheduleDelay.class));
        getPrices.setAccessible(true);
        getPrices.invoke(service);

        Field pricesField = SaasBraintreePaymentService.class.getDeclaredField("prices");
        pricesField.setAccessible(true);
        Map<String, BigInteger> planPricesMap;
        while (true) {
            planPricesMap = (Map<String, BigInteger>)pricesField.get(service);
            if (!planPricesMap.isEmpty()) {
                break;
            }

        }
        assertEquals(planPricesMap, Collections.singletonMap("planId", 1D));
    }

    private Subscription createSubscription() {
        final HashMap<String, String> properties = new HashMap<>(4);
        properties.put("key1", "value1");
        properties.put("key2", "value2");
        return new Subscription().withId(SUBSCRIPTION_ID)
                                 .withAccountId("test_account_id")
                                 .withPlanId(PLAN_ID)
                                 .withServiceId("test_service_id")
                                 .withProperties(properties)
                                 .withBillingCycleType(BillingCycleType.AutoRenew)
                                 .withBillingCycle(1)
                                 .withDescription("description")
                                 .withBillingContractTerm(1)
                                 .withStartDate(new Date())
                                 .withEndDate(new Date())
                                 .withBillingStartDate(new Date())
                                 .withBillingEndDate(new Date())
                                 .withNextBillingDate(new Date())
                                 .withState(SubscriptionState.ACTIVE)
                                 .withUsePaymentSystem(true);
    }

    private void prepareSuccessfulCharge() throws NoSuchFieldException, IllegalAccessException {
        Field prices = SaasBraintreePaymentService.class.getDeclaredField("prices");
        prices.setAccessible(true);
        prices.set(service, Collections.singletonMap(PLAN_ID, PRICE));
        when(gateway.transaction()).thenReturn(transactionGateway);
        when(transactionGateway.sale(any(TransactionRequest.class))).thenReturn(result);
        when(result.getTarget()).thenReturn(transaction);
        when(transaction.getCreditCard()).thenReturn(creditCard);
        when(result.isSuccess()).thenReturn(true);
    }
}