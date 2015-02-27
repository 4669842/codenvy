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
package com.codenvy.api.dao.sql;

import com.codenvy.api.account.billing.BillingPeriod;
import com.codenvy.api.account.billing.BillingService;
import com.codenvy.api.account.billing.InvoiceFilter;
import com.codenvy.api.account.billing.MonthlyBillingPeriod;
import com.codenvy.api.account.billing.PaymentState;
import com.codenvy.api.account.billing.Period;
import com.codenvy.api.account.billing.ResourcesFilter;
import com.codenvy.api.account.impl.shared.dto.AccountResources;
import com.codenvy.api.account.impl.shared.dto.Charge;
import com.codenvy.api.account.impl.shared.dto.Invoice;
import com.codenvy.api.account.impl.shared.dto.Resources;
import com.codenvy.api.account.metrics.MemoryUsedMetric;
import com.codenvy.api.account.metrics.MeterBasedStorage;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

import static com.google.common.collect.Iterables.get;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;


public class SqlBillingServiceTest extends AbstractSQLTest {
    private BillingPeriod billingPeriod = new MonthlyBillingPeriod();

    @DataProvider(name = "storage")
    public Object[][] createDS() throws SQLException {

        Object[][] result = new Object[sources.length][];
        for (int i = 0; i < sources.length; i++) {
            DataSourceConnectionFactory connectionFactory = new DataSourceConnectionFactory(sources[i]);
            result[i] = new Object[]{new SqlMeterBasedStorage(connectionFactory),
                                     new SqlBillingService(connectionFactory, 0.15, 10.0)};
        }
        return result;
    }


    @Test(dataProvider = "storage")
    public void shouldCalculateSimpleInvoice(MeterBasedStorage meterBasedStorage, BillingService billingService)
            throws ParseException, ServerException {
        //given


        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("01-01-2015 10:00:00").getTime(),
                                     sdf.parse("10-01-2015 10:00:00").getTime(),
                                     "usr-345",
                                     "ac-3",
                                     "ws-235423",
                                     "run-234"));

        //when
        billingService.generateInvoices(sdf.parse("01-01-2015 00:00:00").getTime(),
                                        sdf.parse("01-02-2015 00:00:00").getTime());
        List<Invoice> ac3 = billingService.getInvoices("ac-3", -1, 0);
        //then
        assertEquals(ac3.size(), 1);
        Invoice invoice = get(ac3, 0);
        assertEquals(invoice.getTotal(), 30.9);

        assertEquals(invoice.getCharges().size(), 1);
        Charge saasCharge = get(invoice.getCharges(), 0);
        assertEquals(saasCharge.getServiceId(), "Saas");
        assertEquals(saasCharge.getFreeAmount(), 10.0);
        assertEquals(saasCharge.getPaidAmount(), 206.0);
        assertEquals(saasCharge.getPaidPrice(), 0.15);
        assertEquals(saasCharge.getPrePaidAmount(), 0.0);
        assertNotNull(saasCharge.getDetails());
        assertEquals(saasCharge.getDetails().size(), 1);
        assertEquals(saasCharge.getDetails().get("ws-235423"), "216.0");

    }

    @Test(dataProvider = "storage", enabled = false)
    public void shouldBeAbleToFilterInvoiceByDates(MeterBasedStorage meterBasedStorage, BillingService billingService)
            throws ParseException, ServerException {
        //given


        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("01-01-2015 10:00:00").getTime(),
                                     sdf.parse("10-01-2015 10:00:00").getTime(),
                                     "usr-345",
                                     "ac-3",
                                     "ws-235423",
                                     "run-234"));

        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("01-02-2015 10:00:00").getTime(),
                                     sdf.parse("10-02-2015 10:00:00").getTime(),
                                     "usr-345",
                                     "ac-3",
                                     "ws-235423",
                                     "run-2343"));


        //when
        billingService.generateInvoices(sdf.parse("01-01-2015 00:00:00").getTime(),
                                        sdf.parse("01-02-2015 00:00:00").getTime() - 1);
        billingService.generateInvoices(sdf.parse("01-02-2015 00:00:00").getTime(),
                                        sdf.parse("01-03-2015 00:00:00").getTime() - 1);
        List<Invoice> ac3 = billingService.getInvoices(
                InvoiceFilter.builder()
                             .withAccountId("ac-3")
                             .withFromDate(sdf.parse("01-02-2015 00:00:00").getTime())
                             .withTillDate(sdf.parse("01-03-2015 00:00:00").getTime() - 1).build());
        //then
        assertEquals(ac3.size(), 1);
        Invoice invoice = get(ac3, 0);
        assertEquals(invoice.getTotal(), 30.9);
    }


    @Test(dataProvider = "storage")
    public void shouldCalculateFreeHours(MeterBasedStorage meterBasedStorage, BillingService billingService)
            throws ServerException, ParseException {
        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(256,
                                     sdf.parse("01-01-2015 10:00:00").getTime(),
                                     sdf.parse("01-01-2015 12:05:32").getTime(),
                                     "usr-345",
                                     "ac-3",
                                     "ws-235423",
                                     "run-234"));

        //when
        billingService.generateInvoices(sdf.parse("01-01-2015 00:00:00").getTime(),
                                        sdf.parse("01-02-2015 00:00:00").getTime());
        List<Invoice> ac3 = billingService.getInvoices("ac-3", -1, 0);
        //then
        assertEquals(ac3.size(), 1);
        Invoice invoice = get(ac3, 0);
        assertEquals(invoice.getTotal(), 0.0);
        assertEquals(invoice.getCharges().size(), 1);
        Charge saasCharge = get(invoice.getCharges(), 0);
        assertEquals(saasCharge.getServiceId(), "Saas");
        assertEquals(saasCharge.getFreeAmount(), 0.523056);
        assertEquals(saasCharge.getPaidAmount(), 0.0);
        assertEquals(saasCharge.getPaidPrice(), 0.15);
        assertEquals(saasCharge.getPrePaidAmount(), 0.0);
        assertNotNull(saasCharge.getDetails());
        assertEquals(saasCharge.getDetails().size(), 1);
        assertEquals(saasCharge.getDetails().get("ws-235423"), "0.523056");
    }

    @Test(dataProvider = "storage")
    public void shouldCalculateBetweenSeveralWorkspaces(MeterBasedStorage meterBasedStorage,
                                                        BillingService billingService)
            throws ServerException, ParseException {
        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(256,
                                     sdf.parse("01-01-2015 10:00:00").getTime(),
                                     sdf.parse("01-01-2015 12:05:32").getTime(),
                                     "usr-345",
                                     "ac-3",
                                     "ws-235423",
                                     "run-234"));

        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(256,
                                     sdf.parse("02-01-2015 10:00:00").getTime(),
                                     sdf.parse("02-01-2015 12:05:32").getTime(),
                                     "usr-345",
                                     "ac-3",
                                     "ws-2",
                                     "run-234"));

        //when
        billingService.generateInvoices(sdf.parse("01-01-2015 00:00:00").getTime(),
                                        sdf.parse("01-02-2015 00:00:00").getTime());
        List<Invoice> ac3 = billingService.getInvoices("ac-3", -1, 0);
        //then
        assertEquals(ac3.size(), 1);
        Invoice invoice = get(ac3, 0);

        assertEquals(invoice.getTotal(), 0.0);
        assertEquals(invoice.getCharges().size(), 1);
        Charge saasCharge = get(invoice.getCharges(), 0);
        assertEquals(saasCharge.getServiceId(), "Saas");
        assertEquals(saasCharge.getFreeAmount(), 1.046112);
        assertEquals(saasCharge.getPaidAmount(), 0.0);
        assertEquals(saasCharge.getPaidPrice(), 0.15);
        assertEquals(saasCharge.getPrePaidAmount(), 0.0);
        assertNotNull(saasCharge.getDetails());
        assertEquals(saasCharge.getDetails().size(), 2);
        assertEquals(saasCharge.getDetails().get("ws-235423"), "0.523056");
        assertEquals(saasCharge.getDetails().get("ws-2"), "0.523056");
    }


    @Test(dataProvider = "storage")
    public void shouldCalculateWithMultipleAccounts(MeterBasedStorage meterBasedStorage, BillingService billingService)
            throws ParseException, ServerException {
        //given
        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("10-01-2015 10:20:56").getTime(),
                                     sdf.parse("11-01-2015 10:20:56").getTime(),
                                     "usr-123",
                                     "ac-5",
                                     "ws-7",
                                     "run-1254"));

        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("10-01-2015 10:20:56").getTime(),
                                     sdf.parse("10-01-2015 18:20:56").getTime(),
                                     "usr-123",
                                     "ac-1",
                                     "ws-235423",
                                     "run-234"));

        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(256,
                                     sdf.parse("10-01-2015 10:20:56").getTime(),
                                     sdf.parse("10-01-2015 11:18:35").getTime(),
                                     "usr-4358634",
                                     "ac-1",
                                     "ws-4356",
                                     "run-435876"));

        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("01-01-2015 10:00:00").getTime(),
                                     sdf.parse("10-01-2015 10:00:00").getTime(),
                                     "usr-345",
                                     "ac-3",
                                     "ws-235423",
                                     "run-234"));

        //when
        billingService.generateInvoices(sdf.parse("01-01-2015 00:00:00").getTime(),
                                        sdf.parse("01-02-2015 00:00:00").getTime());
        List<Invoice> ac3 = billingService.getInvoices("ac-3", -1, 0);
        List<Invoice> ac5 = billingService.getInvoices("ac-5", -1, 0);
        List<Invoice> ac1 = billingService.getInvoices("ac-1", -1, 0);
        //then
        assertEquals(ac3.size(), 1);
        assertEquals(get(ac3, 0).getTotal(), 30.9);


        assertEquals(ac5.size(), 1);
        assertEquals(get(ac5, 0).getTotal(), 2.1);

        assertEquals(ac1.size(), 1);
        assertEquals(get(ac1, 0).getTotal(), 0.0);

    }

    @Test(dataProvider = "storage")
    public void shouldBeAbleToGetByPaymentState(MeterBasedStorage meterBasedStorage, BillingService billingService)
            throws ParseException, ServerException {
        //given
        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("10-01-2015 10:20:56").getTime(),
                                     sdf.parse("11-01-2015 10:20:56").getTime(),
                                     "usr-123",
                                     "ac-5",
                                     "ws-7",
                                     "run-1254"));

        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("01-01-2015 10:00:00").getTime(),
                                     sdf.parse("10-01-2015 10:00:00").getTime(),
                                     "usr-345",
                                     "ac-3",
                                     "ws-235423",
                                     "run-234"));

        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("01-01-2015 10:00:00").getTime(),
                                     sdf.parse("01-01-2015 11:00:00").getTime(),
                                     "usr-34",
                                     "ac-4",
                                     "ws-4567845",
                                     "run-345634"));


        //when
        billingService.generateInvoices(sdf.parse("01-01-2015 00:00:00").getTime(),
                                        sdf.parse("01-02-2015 00:00:00").getTime());
        //then
        assertEquals(billingService.getInvoices(PaymentState.WAITING_EXECUTOR, -1, 0).size(), 2);
        assertEquals(billingService.getInvoices(PaymentState.NOT_REQUIRED, -1, 0).size(), 1);
        assertEquals(billingService.getInvoices(PaymentState.EXECUTING, -1, 0).size(), 0);
        assertEquals(billingService.getInvoices(PaymentState.PAYMENT_FAIL, -1, 0).size(), 0);
        assertEquals(billingService.getInvoices(PaymentState.CREDIT_CARD_MISSING, -1, 0).size(), 0);
        assertEquals(billingService.getInvoices(PaymentState.PAID_SUCCESSFULLY, -1, 0).size(), 0);

    }

    @Test(dataProvider = "storage")
    public void shouldBeAbleToSetPaymentState(MeterBasedStorage meterBasedStorage, BillingService billingService)
            throws ParseException, ServerException {
        //given
        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("10-01-2015 10:20:56").getTime(),
                                     sdf.parse("11-01-2015 10:20:56").getTime(),
                                     "usr-123",
                                     "ac-5",
                                     "ws-7",
                                     "run-1254"));

        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("01-01-2015 10:00:00").getTime(),
                                     sdf.parse("10-01-2015 10:00:00").getTime(),
                                     "usr-345",
                                     "ac-3",
                                     "ws-235423",
                                     "run-234"));


        //when
        billingService.generateInvoices(sdf.parse("01-01-2015 00:00:00").getTime(),
                                        sdf.parse("01-02-2015 00:00:00").getTime());
        Invoice invoice = get(billingService.getInvoices(PaymentState.WAITING_EXECUTOR, 50, 0), 1);
        billingService.setPaymentState(invoice.getId(), PaymentState.PAYMENT_FAIL, "cc111");

        //then
        assertEquals(invoice.getPaymentDate().longValue(), 0L);
        assertEquals(billingService.getInvoices(PaymentState.WAITING_EXECUTOR, -1, 0).size(), 1);
        assertEquals(billingService.getInvoices(PaymentState.EXECUTING, -1, 0).size(), 0);
        List<Invoice> invoices = billingService.getInvoices(PaymentState.PAYMENT_FAIL, 50, 0);
        assertEquals(invoices.size(), 1);

        Invoice invoice1 = get(invoices, 0);
        Assert.assertTrue(invoice1.getPaymentDate() > 0);
        assertEquals(invoice1.getCreditCardId(), "cc111");
        assertEquals(get(billingService.getInvoices(PaymentState.PAYMENT_FAIL, 50, 0), 0).getId(), invoice.getId());
        assertEquals(billingService.getInvoices(PaymentState.CREDIT_CARD_MISSING, 50, 0).size(), 0);
        assertEquals(billingService.getInvoices(PaymentState.PAID_SUCCESSFULLY, -1, 0).size(), 0);

    }


    @Test(dataProvider = "storage")
    public void shouldBeAbleToSetGetByMailingFailPaymentInvoice(MeterBasedStorage meterBasedStorage,
                                                                BillingService billingService)
            throws ParseException, ServerException {
        //given
        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("10-01-2015 10:20:56").getTime(),
                                     sdf.parse("11-01-2015 10:20:56").getTime(),
                                     "usr-123",
                                     "ac-5",
                                     "ws-7",
                                     "run-1254"));

        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("01-01-2015 10:00:00").getTime(),
                                     sdf.parse("10-01-2015 10:00:00").getTime(),
                                     "usr-345",
                                     "ac-3",
                                     "ws-235423",
                                     "run-234"));


        //when
        billingService.generateInvoices(sdf.parse("01-01-2015 00:00:00").getTime(),
                                        sdf.parse("01-02-2015 00:00:00").getTime());
        Long id = get(billingService.getInvoices(PaymentState.WAITING_EXECUTOR, 50, 0), 1).getId();
        billingService.setPaymentState(id, PaymentState.PAYMENT_FAIL, "cc-234356");

        //then
        List<Invoice> notSendInvoice = billingService.getNotSendInvoices(-1, 0);
        assertEquals(notSendInvoice.size(), 1);
        assertEquals(get(notSendInvoice, 0).getId(), id);

    }

    @Test(dataProvider = "storage")
    public void shouldBeAbleToSetGetByMailingPaidSuccessfulyInvoice(MeterBasedStorage meterBasedStorage,
                                                                    BillingService billingService)
            throws ParseException, ServerException {
        //given
        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("10-01-2015 10:20:56").getTime(),
                                     sdf.parse("11-01-2015 10:20:56").getTime(),
                                     "usr-123",
                                     "ac-5",
                                     "ws-7",
                                     "run-1254"));

        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("01-01-2015 10:00:00").getTime(),
                                     sdf.parse("10-01-2015 10:00:00").getTime(),
                                     "usr-345",
                                     "ac-3",
                                     "ws-235423",
                                     "run-234"));


        //when
        billingService.generateInvoices(sdf.parse("01-01-2015 00:00:00").getTime(),
                                        sdf.parse("01-02-2015 00:00:00").getTime());
        Long id = get(billingService.getInvoices(PaymentState.WAITING_EXECUTOR, 50, 0), 1).getId();
        billingService.setPaymentState(id, PaymentState.PAID_SUCCESSFULLY, "cc-445");

        //then
        List<Invoice> notSendInvoice = billingService.getNotSendInvoices(-1, 0);
        assertEquals(notSendInvoice.size(), 1);
        assertEquals(get(notSendInvoice, 0).getId(), id);

    }

    @Test(dataProvider = "storage")
    public void shouldBeAbleToSetGetByMailingCreditCardMissingInvoice(MeterBasedStorage meterBasedStorage,
                                                                      BillingService billingService)
            throws ParseException, ServerException {
        //given
        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("10-01-2015 10:20:56").getTime(),
                                     sdf.parse("11-01-2015 10:20:56").getTime(),
                                     "usr-123",
                                     "ac-5",
                                     "ws-7",
                                     "run-1254"));

        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("01-01-2015 10:00:00").getTime(),
                                     sdf.parse("10-01-2015 10:00:00").getTime(),
                                     "usr-345",
                                     "ac-3",
                                     "ws-235423",
                                     "run-234"));


        //when
        billingService.generateInvoices(sdf.parse("01-01-2015 00:00:00").getTime(),
                                        sdf.parse("01-02-2015 00:00:00").getTime());
        Long id = get(billingService.getInvoices(PaymentState.WAITING_EXECUTOR, 50, 0), 1).getId();
        billingService.setPaymentState(id, PaymentState.CREDIT_CARD_MISSING, null);

        //then
        List<Invoice> notSendInvoice = billingService.getNotSendInvoices(-1, 0);
        assertEquals(notSendInvoice.size(), 1);
        assertEquals(get(notSendInvoice, 0).getId(), id);

    }


    @Test(dataProvider = "storage")
    public void shouldBeAbleToGetByMailingPaymentNotRequiredInvoice(MeterBasedStorage meterBasedStorage,
                                                                    BillingService billingService)
            throws ParseException, ServerException {
        //given
        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("10-01-2015 10:20:56").getTime(),
                                     sdf.parse("10-01-2015 11:20:56").getTime(),
                                     "usr-123",
                                     "ac-5",
                                     "ws-7",
                                     "run-1254"));

        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("01-01-2015 10:00:00").getTime(),
                                     sdf.parse("10-01-2015 10:00:00").getTime(),
                                     "usr-345",
                                     "ac-3",
                                     "ws-235423",
                                     "run-234"));


        //when
        billingService.generateInvoices(sdf.parse("01-01-2015 00:00:00").getTime(),
                                        sdf.parse("01-02-2015 00:00:00").getTime());
        //then
        List<Invoice> notSendInvoice = billingService.getNotSendInvoices(-1, 0);
        assertEquals(notSendInvoice.size(), 1);
        assertEquals(get(notSendInvoice, 0).getAccountId(), "ac-5");

    }


    @Test(dataProvider = "storage")
    public void shouldBeAbleToSetInvoiceMailState(MeterBasedStorage meterBasedStorage, BillingService billingService)
            throws ParseException, ServerException {
        //given
        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("10-01-2015 10:20:56").getTime(),
                                     sdf.parse("11-01-2015 10:20:56").getTime(),
                                     "usr-123",
                                     "ac-5",
                                     "ws-7",
                                     "run-1254"));

        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("01-01-2015 10:00:00").getTime(),
                                     sdf.parse("10-01-2015 10:00:00").getTime(),
                                     "usr-345",
                                     "ac-3",
                                     "ws-235423",
                                     "run-234"));


        //when
        billingService.generateInvoices(sdf.parse("01-01-2015 00:00:00").getTime(),
                                        sdf.parse("01-02-2015 00:00:00").getTime());
        Long id = get(billingService.getInvoices(PaymentState.WAITING_EXECUTOR, -1, 0), 1).getId();
        billingService.setPaymentState(id, PaymentState.CREDIT_CARD_MISSING, null);
        billingService.markInvoiceAsSent(id);
        //then
        assertEquals(billingService.getNotSendInvoices(-1, 0).size(), 0);
    }


    @Test(dataProvider = "storage", expectedExceptions = NotFoundException.class, expectedExceptionsMessageRegExp =
            "Invoice with id " +
            "498509 is not found")
    public void shouldFailIfInvoiceIsNotFound(MeterBasedStorage meterBasedStorage, BillingService billingService)
            throws ServerException, NotFoundException {
        //given
        //when
        billingService.getInvoice(498509);
    }

    @Test(dataProvider = "storage")
    public void shouldBeAbleToGetInvoicesById(MeterBasedStorage meterBasedStorage, BillingService billingService)
            throws ParseException, ServerException, NotFoundException {
        //given
        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("10-01-2015 10:20:56").getTime(),
                                     sdf.parse("11-01-2015 10:20:56").getTime(),
                                     "usr-123",
                                     "ac-5",
                                     "ws-7",
                                     "run-1254"));

        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("01-01-2015 10:00:00").getTime(),
                                     sdf.parse("10-01-2015 10:00:00").getTime(),
                                     "usr-345",
                                     "ac-3",
                                     "ws-235423",
                                     "run-234"));


        //when
        billingService.generateInvoices(sdf.parse("01-01-2015 00:00:00").getTime(),
                                        sdf.parse("01-02-2015 00:00:00").getTime());
        Invoice expected = get(billingService.getInvoices(PaymentState.WAITING_EXECUTOR, 50, 0), 1);
        Invoice actual = billingService.getInvoice(expected.getId());
        assertEquals(actual, expected);


    }

    @Test(dataProvider = "storage", expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp =
            "Not able to generate invoices. Result overlaps with existed invoices.")
    public void shouldFailToCalculateInvoicesTwiceWithOverlappingPeriod(MeterBasedStorage meterBasedStorage,
                                                                        BillingService billingService)
            throws ParseException, ServerException, NotFoundException {
        //given
        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("10-01-2015 10:20:56").getTime(),
                                     sdf.parse("11-01-2015 10:20:56").getTime(),
                                     "usr-123",
                                     "ac-5",
                                     "ws-7",
                                     "run-1254"));

        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("01-01-2015 10:00:00").getTime(),
                                     sdf.parse("10-01-2015 10:00:00").getTime(),
                                     "usr-345",
                                     "ac-3",
                                     "ws-235423",
                                     "run-234"));


        //when
        billingService.generateInvoices(sdf.parse("01-01-2015 00:00:00").getTime(),
                                        sdf.parse("01-02-2015 00:00:00").getTime());
        billingService.generateInvoices(sdf.parse("09-01-2015 00:00:00").getTime(),
                                        sdf.parse("15-02-2015 00:00:00").getTime());

    }

    @Test(dataProvider = "storage")
    public void shouldBeAbleToAddPrepaidTime(MeterBasedStorage meterBasedStorage,
                                             BillingService billingService)
            throws ParseException, ServerException, NotFoundException {
        //when
        billingService.addPrepaid("ac-1", 34.34,
                                  sdf.parse("01-01-2015 00:00:00").getTime(),
                                  sdf.parse("01-02-2015 00:00:00").getTime());
    }

    @Test(dataProvider = "storage", expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp =
            "Unable to add new prepaid time since it overlapping with existed period")
    public void shouldNoBeAbleToAddPrepaidTimeForIntersectionPeriod(MeterBasedStorage meterBasedStorage,
                                                                    BillingService billingService)
            throws ParseException, ServerException, NotFoundException {
        //when
        billingService.addPrepaid("ac-1", 34.34,
                                  sdf.parse("01-01-2015 00:00:00").getTime(),
                                  sdf.parse("01-02-2015 00:00:00").getTime());
        billingService.addPrepaid("ac-1", 34.34,
                                  sdf.parse("15-01-2015 00:00:00").getTime(),
                                  sdf.parse("15-02-2015 00:00:00").getTime());

    }

    @Test(dataProvider = "storage")
    public void shouldBeAbleToCalculateInvoiceWithoutPrepaid(
            MeterBasedStorage meterBasedStorage,
            BillingService billingService)
            throws ParseException, ServerException, NotFoundException {
        //given
        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("10-01-2015 01:00:00").getTime(),
                                     sdf.parse("21-01-2015 21:00:00").getTime(),
                                     "usr-123",
                                     "ac-5",
                                     "ws-7",
                                     "run-1254"));


        //when
        Period period = billingPeriod.get(sdf.parse("01-01-2015 00:00:00"));
        billingService.generateInvoices(period.getStartDate().getTime(), period.getEndDate().getTime());
        //then
        Invoice actual = get(billingService.getInvoices("ac-5", -1, 0), 0);
        assertNotNull(actual);
        Charge saasCharge = get(actual.getCharges(), 0);
        assertNotNull(saasCharge);
        assertEquals(saasCharge.getFreeAmount(), 10.0);
        assertEquals(saasCharge.getPrePaidAmount(), 0.0);
        assertEquals(saasCharge.getPaidAmount(), 274.0);


    }

    @Test(dataProvider = "storage")
    public void shouldBeAbleToAddPrepaidTimeForInvoicePrepaidAddFromTheMiddleOfTheMonth(
            MeterBasedStorage meterBasedStorage,
            BillingService billingService)
            throws ParseException, ServerException, NotFoundException {
        //given
        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("10-01-2015 01:00:00").getTime(),
                                     sdf.parse("21-01-2015 21:00:00").getTime(),
                                     "usr-123",
                                     "ac-5",
                                     "ws-7",
                                     "run-1254"));
        billingService.addPrepaid("ac-5", 100,
                                  sdf.parse("15-01-2015 00:00:00").getTime(),
                                  sdf.parse("15-05-2015 00:00:00").getTime());


        //when
        Period period = billingPeriod.get(sdf.parse("01-01-2015 00:00:00"));
        billingService.generateInvoices(period.getStartDate().getTime(), period.getEndDate().getTime());
        //then
        Invoice actual = get(billingService.getInvoices("ac-5", -1, 0), 0);
        assertNotNull(actual);
        Charge saasCharge = get(actual.getCharges(), 0);
        assertNotNull(saasCharge);
        assertEquals(saasCharge.getFreeAmount(), 10.0);
        assertEquals(saasCharge.getPrePaidAmount(), 54.83871);
        assertEquals(saasCharge.getPaidAmount(), 219.16129);


    }

    @Test(dataProvider = "storage")
    public void shouldBeAbleToAddPrepaidTimeForInvoicePrepaidAddTillTheMiddleOfTheMonth(
            MeterBasedStorage meterBasedStorage,
            BillingService billingService)
            throws ParseException, ServerException, NotFoundException {
        //given
        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("10-01-2015 01:00:00").getTime(),
                                     sdf.parse("21-01-2015 21:00:00").getTime(),
                                     "usr-123",
                                     "ac-5",
                                     "ws-7",
                                     "run-1254"));
        billingService.addPrepaid("ac-5", 100,
                                  sdf.parse("15-12-2014 00:00:00").getTime(),
                                  sdf.parse("15-01-2015 00:00:00").getTime());


        //when
        Period period = billingPeriod.get(sdf.parse("01-01-2015 00:00:00"));
        billingService.generateInvoices(period.getStartDate().getTime(), period.getEndDate().getTime());
        //then
        Invoice actual = get(billingService.getInvoices("ac-5", -1, 0), 0);
        assertNotNull(actual);
        Charge saasCharge = get(actual.getCharges(), 0);
        assertNotNull(saasCharge);
        assertEquals(saasCharge.getFreeAmount(), 10.0);
        assertEquals(saasCharge.getPrePaidAmount(), 45, 16129);
        assertEquals(saasCharge.getPaidAmount(), 228, 83871);


    }

    @Test(dataProvider = "storage")
    public void shouldBeAbleToAddPrepaidTimeForInvoicePrepaidAddForTheFullMonth(
            MeterBasedStorage meterBasedStorage,
            BillingService billingService)
            throws ParseException, ServerException, NotFoundException {
        //given
        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("10-01-2015 01:00:00").getTime(),
                                     sdf.parse("21-01-2015 21:00:00").getTime(),
                                     "usr-123",
                                     "ac-5",
                                     "ws-7",
                                     "run-1254"));
        billingService.addPrepaid("ac-5", 100,
                                  sdf.parse("15-12-2014 00:00:00").getTime(),
                                  sdf.parse("15-05-2015 00:00:00").getTime());


        //when
        Period period = billingPeriod.get(sdf.parse("01-01-2015 00:00:00"));
        billingService.generateInvoices(period.getStartDate().getTime(), period.getEndDate().getTime());
        //then
        Invoice actual = get(billingService.getInvoices("ac-5", -1, 0), 0);
        assertNotNull(actual);
        Charge saasCharge = get(actual.getCharges(), 0);
        assertNotNull(saasCharge);
        assertEquals(saasCharge.getFreeAmount(), 10.0);
        assertEquals(saasCharge.getPrePaidAmount(), 100.0);
        assertEquals(saasCharge.getPaidAmount(), 174.0);

    }

    @Test(dataProvider = "storage")
    public void shouldBeAbleToAddPrepaidTimeForInvoiceFromTwoClosePeriods(
            MeterBasedStorage meterBasedStorage,
            BillingService billingService)
            throws ParseException, ServerException, NotFoundException {
        //given
        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("10-01-2015 01:00:00").getTime(),
                                     sdf.parse("21-01-2015 21:00:00").getTime(),
                                     "usr-123",
                                     "ac-5",
                                     "ws-7",
                                     "run-1254"));
        billingService.addPrepaid("ac-5", 100,
                                  sdf.parse("15-12-2014 00:00:00").getTime(),
                                  sdf.parse("15-01-2015 00:00:00").getTime() - 1);
        billingService.addPrepaid("ac-5", 100,
                                  sdf.parse("15-01-2015 00:00:00").getTime(),
                                  sdf.parse("15-02-2015 00:00:00").getTime());


        //when
        Period period = billingPeriod.get(sdf.parse("01-01-2015 00:00:00"));
        billingService.generateInvoices(period.getStartDate().getTime(), period.getEndDate().getTime());
        //then
        Invoice actual = get(billingService.getInvoices("ac-5", -1, 0), 0);
        assertNotNull(actual);
        Charge saasCharge = get(actual.getCharges(), 0);
        assertNotNull(saasCharge);
        assertEquals(saasCharge.getFreeAmount(), 10.0);
        assertEquals(saasCharge.getPrePaidAmount(), 100.0);
        assertEquals(saasCharge.getPaidAmount(), 174.0);

    }

    @Test(dataProvider = "storage")
    public void shouldBeAbleToAddPrepaidTimeForInvoiceFromTwoSeparatePeriods(
            MeterBasedStorage meterBasedStorage,
            BillingService billingService)
            throws ParseException, ServerException, NotFoundException {
        //given
        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("10-01-2015 01:00:00").getTime(),
                                     sdf.parse("21-01-2015 21:00:00").getTime(),
                                     "usr-123",
                                     "ac-5",
                                     "ws-7",
                                     "run-1254"));
        billingService.addPrepaid("ac-5", 100,
                                  sdf.parse("15-12-2014 00:00:00").getTime(),
                                  sdf.parse("15-01-2015 00:00:00").getTime() - 1);
        billingService.addPrepaid("ac-5", 100,
                                  sdf.parse("20-01-2015 00:00:00").getTime(),
                                  sdf.parse("15-02-2015 00:00:00").getTime());


        //when
        Period period = billingPeriod.get(sdf.parse("01-01-2015 00:00:00"));
        billingService.generateInvoices(period.getStartDate().getTime(), period.getEndDate().getTime());
        //then
        Invoice actual = get(billingService.getInvoices("ac-5", -1, 0), 0);
        assertNotNull(actual);
        Charge saasCharge = get(actual.getCharges(), 0);
        assertNotNull(saasCharge);
        assertEquals(saasCharge.getFreeAmount(), 10.0);
        assertEquals(saasCharge.getPrePaidAmount(), 83.870968);
        assertEquals(saasCharge.getPaidAmount(), 190.129032);

    }


    @Test(dataProvider = "storage", expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp =
            "Credit card parameter is missing for states  PAYMENT_FAIL or PAID_SUCCESSFULLY")
    public void shouldNotAllowToSetPaymentStateSuccessfulWithoutCC(
            MeterBasedStorage meterBasedStorage,
            BillingService billingService)
            throws ParseException, ServerException, NotFoundException {
        //given
        billingService.setPaymentState(1, PaymentState.PAID_SUCCESSFULLY, null);

    }

    @Test(dataProvider = "storage", expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp =
            "Credit card parameter is missing for states  PAYMENT_FAIL or PAID_SUCCESSFULLY")
    public void shouldNotAllowToSetPaymentStateSuccessfulWithEmptyCC(
            MeterBasedStorage meterBasedStorage,
            BillingService billingService)
            throws ParseException, ServerException, NotFoundException {
        //given
        billingService.setPaymentState(1, PaymentState.PAID_SUCCESSFULLY, "");
    }

    @Test(dataProvider = "storage", expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp =
            "Credit card parameter is missing for states  PAYMENT_FAIL or PAID_SUCCESSFULLY")
    public void shouldNotAllowToSetPaymentStateFailWithoutCC(
            MeterBasedStorage meterBasedStorage,
            BillingService billingService)
            throws ParseException, ServerException, NotFoundException {
        //given
        billingService.setPaymentState(1, PaymentState.PAYMENT_FAIL, null);

    }

    @Test(dataProvider = "storage", expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp =
            "Credit card parameter is missing for states  PAYMENT_FAIL or PAID_SUCCESSFULLY")
    public void shouldNotAllowToSetPaymentStateFailWithEmptyCC(
            MeterBasedStorage meterBasedStorage,
            BillingService billingService)
            throws ParseException, ServerException, NotFoundException {
        //given
        billingService.setPaymentState(1, PaymentState.PAYMENT_FAIL, "");
    }

    @Test(dataProvider = "storage", expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp =
            "Credit card parameter should be null for states different when PAYMENT_FAIL or PAID_SUCCESSFULLY")
    public void shouldNotAllowToSetPaymentStateNotRequiredWithCC(
            MeterBasedStorage meterBasedStorage,
            BillingService billingService)
            throws ParseException, ServerException, NotFoundException {
        //given
        billingService.setPaymentState(1, PaymentState.NOT_REQUIRED, "CC");
    }

    @Test(dataProvider = "storage", expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp =
            "Credit card parameter should be null for states different when PAYMENT_FAIL or PAID_SUCCESSFULLY")
    public void shouldNotAllowToSetPaymentStateCCMissingWithCC(
            MeterBasedStorage meterBasedStorage,
            BillingService billingService)
            throws ParseException, ServerException, NotFoundException {
        //given
        billingService.setPaymentState(1, PaymentState.CREDIT_CARD_MISSING, "CC");
    }


    @Test(dataProvider = "storage")
    public void shouldGetEstimationByAccountWithAllDatesBetweenPeriod(MeterBasedStorage meterBasedStorage,
                                                                      BillingService billingService)
            throws ServerException, ParseException {
        //given
        meterBasedStorage.createMemoryUsedRecord(new MemoryUsedMetric(256,
                                                                      sdf.parse("10-01-2014 11:00:00").getTime(),
                                                                      sdf.parse("10-01-2014 11:01:00").getTime(),
                                                                      "usr-123453",
                                                                      "ac-348798",
                                                                      "ws-235675423",
                                                                      "run-2344567"));

        meterBasedStorage.createMemoryUsedRecord(new MemoryUsedMetric(256,
                                                                      sdf.parse("10-01-2014 10:00:00").getTime(),
                                                                      sdf.parse("10-01-2014 10:05:00").getTime(),
                                                                      "usr-123",
                                                                      "ac-46534",
                                                                      "ws-235423",
                                                                      "run-234"));
        meterBasedStorage.createMemoryUsedRecord(new MemoryUsedMetric(256,
                                                                      sdf.parse("10-01-2014 11:00:00").getTime(),
                                                                      sdf.parse("10-01-2014 11:07:00").getTime(),
                                                                      "usr-123",
                                                                      "ac-46534",
                                                                      "ws-235423",
                                                                      "run-234"));

        meterBasedStorage.createMemoryUsedRecord(new MemoryUsedMetric(1024,
                                                                      sdf.parse("10-01-2014 12:00:00").getTime(),
                                                                      sdf.parse("10-01-2014 12:20:00").getTime(),
                                                                      "usr-123",
                                                                      "ac-46534",
                                                                      "ws-235423",
                                                                      "run-234"));

        //when
        List<AccountResources> usage = billingService
                .getEstimatedUsageByAccount(ResourcesFilter.builder()
                                                           .withFromDate(sdf.parse("10-01-2014 09:00:00").getTime())
                                                           .withTillDate(sdf.parse("10-01-2014 14:00:00").getTime())
                                                           .withAccountId("ac-46534")
                                                           .build());

        //then
        assertEquals(usage.size(), 1);
        AccountResources resources = get(usage, 0);
        assertEquals(resources.getFreeAmount(), 0.383333);

    }


    @Test(dataProvider = "storage")
    public void shouldGetEstimateByAccountWithDatesBetweenPeriod(MeterBasedStorage meterBasedStorage,
                                                                   BillingService billingService)
            throws ServerException, ParseException {
        //given
        //when
        meterBasedStorage.createMemoryUsedRecord(new MemoryUsedMetric(256,
                                                                      sdf.parse("10-01-2014 11:00:00").getTime(),
                                                                      sdf.parse("10-01-2014 11:01:00").getTime(),
                                                                      "usr-123453",
                                                                      "ac-348798",
                                                                      "ws-235675423",
                                                                      "run-2344567"));

        meterBasedStorage.createMemoryUsedRecord(new MemoryUsedMetric(256,
                                                                      sdf.parse("10-01-2013 10:00:00").getTime(),
                                                                      sdf.parse("10-01-2013 10:05:00").getTime(),
                                                                      "usr-123",
                                                                      "ac-46534",
                                                                      "ws-235423",
                                                                      "run-234"));

        meterBasedStorage.createMemoryUsedRecord(new MemoryUsedMetric(256,
                                                                      sdf.parse("10-01-2014 09:55:00").getTime(),
                                                                      sdf.parse("10-01-2014 10:05:00").getTime(),
                                                                      "usr-123",
                                                                      "ac-46534",
                                                                      "ws-235423",
                                                                      "run-234"));
        meterBasedStorage.createMemoryUsedRecord(new MemoryUsedMetric(256,
                                                                      sdf.parse("10-01-2014 11:00:00").getTime(),
                                                                      sdf.parse("10-01-2014 11:07:00").getTime(),
                                                                      "usr-123",
                                                                      "ac-46534",
                                                                      "ws-235423",
                                                                      "run-234"));

        meterBasedStorage.createMemoryUsedRecord(new MemoryUsedMetric(1024,
                                                                      sdf.parse("10-01-2014 12:00:00").getTime(),
                                                                      sdf.parse("10-01-2014 12:20:00").getTime(),
                                                                      "usr-123",
                                                                      "ac-46534",
                                                                      "ws-235423",
                                                                      "run-234"));

        meterBasedStorage.createMemoryUsedRecord(new MemoryUsedMetric(256,
                                                                      sdf.parse("10-01-2015 10:00:00").getTime(),
                                                                      sdf.parse("10-01-2015 10:05:00").getTime(),
                                                                      "usr-123",
                                                                      "ac-46534",
                                                                      "ws-235423",
                                                                      "run-234"));

        //when
        List<AccountResources> usage = billingService
                .getEstimatedUsageByAccount(ResourcesFilter.builder()
                                                           .withFromDate(sdf.parse("10-01-2014 10:00:00").getTime())
                                                           .withTillDate(sdf.parse("10-01-2014 12:10:00").getTime())
                                                           .withAccountId("ac-46534")
                                                           .build());

        //then
        assertEquals(usage.size(), 1);
        AccountResources resources = get(usage, 0);
        assertEquals(resources.getFreeAmount(), 0.216667);
    }
    @Test(dataProvider = "storage")
    public void shouldBeAbleToEstimateUsageWithFreePrepaidAndPaidTime(MeterBasedStorage meterBasedStorage,
                                                                      BillingService billingService) throws ParseException,
                                                                                                            ServerException {
        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("10-01-2015 01:00:00").getTime(),
                                     sdf.parse("21-01-2015 21:00:00").getTime(),
                                     "usr-123",
                                     "ac-5",
                                     "ws-7",
                                     "run-1254"));
        billingService.addPrepaid("ac-5", 100,
                                  sdf.parse("15-12-2014 00:00:00").getTime(),
                                  sdf.parse("15-01-2015 00:00:00").getTime() - 1);
        billingService.addPrepaid("ac-5", 100,
                                  sdf.parse("20-01-2015 00:00:00").getTime(),
                                  sdf.parse("15-02-2015 00:00:00").getTime());



        //when
        Period period = billingPeriod.get(sdf.parse("01-01-2015 00:00:00"));
        List<AccountResources> usage = billingService
                .getEstimatedUsageByAccount(ResourcesFilter.builder()
                                                           .withFromDate(period.getStartDate().getTime())
                                                           .withTillDate(period.getEndDate().getTime())
                                                           .withAccountId("ac-5")
                                                           .withFreeGbHMoreThan(4.12)
                                                           .withPrePaidGbHMoreThan(15.0)
                                                           .withPaidGbHMoreThan(100.0)
                                                           .withMaxItems(1)
                                                           .withSkipCount(0)
                                                           .build());

        //then
        assertEquals(usage.size(), 1);
        AccountResources resources = get(usage, 0);
        assertEquals(resources.getFreeAmount(), 10.0);
        assertEquals(resources.getPrePaidAmount(), 83.870968);
        assertEquals(resources.getPaidAmount(), 190.129032);

    }

    @Test(dataProvider = "storage")
    public void shouldBeAbleToLimitAndSkipEstimateUsageWithFreePrepaidAndPaidTime(MeterBasedStorage meterBasedStorage,
                                                                      BillingService billingService) throws ParseException,
                                                                                                            ServerException {
        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("10-01-2015 01:00:00").getTime(),
                                     sdf.parse("21-01-2015 21:00:00").getTime(),
                                     "usr-123",
                                     "ac-6",
                                     "ws-7",
                                     "run-1254"));

        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("01-01-2015 08:23:00").getTime(),
                                     sdf.parse("02-01-2015 18:00:00").getTime(),
                                     "usr-123",
                                     "ac-5",
                                     "ws-7",
                                     "run-1254"));

        billingService.addPrepaid("ac-6", 100,
                                  sdf.parse("15-12-2014 00:00:00").getTime(),
                                  sdf.parse("15-01-2015 00:00:00").getTime() - 1);


        //when
        Period period = billingPeriod.get(sdf.parse("01-01-2015 00:00:00"));
        List<AccountResources> usage = billingService
                .getEstimatedUsageByAccount(ResourcesFilter.builder()
                                                           .withFromDate(period.getStartDate().getTime())
                                                           .withTillDate(period.getEndDate().getTime())
                                                           .withMaxItems(1)
                                                           .withSkipCount(1)
                                                           .build());

        //then
        assertEquals(usage.size(), 1);
        AccountResources resources = get(usage, 0);
        assertEquals(resources.getAccountId(), "ac-6");
        assertEquals(resources.getFreeAmount(), 10.0);
        assertEquals(resources.getPrePaidAmount(), 45.161290);
        assertEquals(resources.getPaidAmount(), 228.838710);

    }



    @Test(dataProvider = "storage")
    public void shouldBeAbleToGetEstimatedUsage(MeterBasedStorage meterBasedStorage,
                                                                                  BillingService billingService) throws ParseException,
                                                                                                                        ServerException {
        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("10-01-2015 01:00:00").getTime(),
                                     sdf.parse("12-01-2015 21:00:00").getTime(),
                                     "usr-123",
                                     "ac-6",
                                     "ws-7",
                                     "run-1254"));

        meterBasedStorage.createMemoryUsedRecord(
                new MemoryUsedMetric(1024,
                                     sdf.parse("10-01-2015 08:23:00").getTime(),
                                     sdf.parse("11-01-2015 12:23:00").getTime(),
                                     "usr-123",
                                     "ac-5",
                                     "ws-7",
                                     "run-1256"));

        billingService.addPrepaid("ac-6", 100,
                                  sdf.parse("15-12-2014 00:00:00").getTime(),
                                  sdf.parse("15-02-2015 00:00:00").getTime() - 1);


        //when
        Period period = billingPeriod.get(sdf.parse("01-01-2015 00:00:00"));

        Resources usage = billingService
                .getEstimatedUsage(period.getStartDate().getTime(), period.getEndDate().getTime());
        List<AccountResources> usageAccount = billingService
                .getEstimatedUsageByAccount(ResourcesFilter.builder()
                                                           .withFromDate(period.getStartDate().getTime())
                                                           .withTillDate(period.getEndDate().getTime())
                                                           .build());

        //then
        assertEquals(usage.getFreeAmount(), 20.0);
        assertEquals(usage.getPrePaidAmount(), 58.0);
        assertEquals(usage.getPaidAmount(), 18.0);
        assertEquals(usageAccount.size(), 2);
        AccountResources ac5 = get(usageAccount, 0);
        AccountResources ac6 = get(usageAccount, 1);

        assertEquals(ac5.getFreeAmount(), 10.0);
        assertEquals(ac5.getPrePaidAmount(), 0.0);
        assertEquals(ac5.getPaidAmount(), 18.0);

        assertEquals(ac6.getFreeAmount(), 10.0);
        assertEquals(ac6.getPrePaidAmount(), 58.0);
        assertEquals(ac6.getPaidAmount(), 0.0);


    }

}