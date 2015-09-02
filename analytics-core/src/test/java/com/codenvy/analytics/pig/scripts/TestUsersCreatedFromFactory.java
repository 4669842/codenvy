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
package com.codenvy.analytics.pig.scripts;

import com.codenvy.analytics.BaseTest;
import com.codenvy.analytics.datamodel.LongValueData;
import com.codenvy.analytics.metrics.Context;
import com.codenvy.analytics.metrics.Metric;
import com.codenvy.analytics.metrics.MetricFactory;
import com.codenvy.analytics.metrics.MetricFilter;
import com.codenvy.analytics.metrics.MetricType;
import com.codenvy.analytics.metrics.Parameters;
import com.codenvy.analytics.pig.scripts.util.Event;
import com.codenvy.analytics.pig.scripts.util.LogGenerator;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

/** @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a> */
public class TestUsersCreatedFromFactory extends BaseTest {

    @BeforeClass
    public void prepare() throws Exception {
        addRegisteredUser(UID1, "user1@gmail.com");
        addRegisteredUser(UID2, "user2@gmail.com");
        addRegisteredUser(UID3, "user3@gmail.com");
        addRegisteredUser(UID4, "user4@gmail.com");

        List<Event> events = new ArrayList<>();

        events.add(
                Event.Builder.createFactoryUrlAcceptedEvent("tmp-1", "factoryUrl1", "referrer1", "org1", "affiliate1", "named", "acceptor")
                             .withDate("2013-01-01").withTime("11:00:00").build());

        events.add(Event.Builder.createUserAddedToWsEvent("anonymoususer_1", "tmp-1", "website")
                                .withDate("2013-01-01").build());

        events.add(Event.Builder.createUserChangedNameEvent("user1@gmail.com", "user2@gmail.com").withDate("2013-01-01")
                                .build());
        events.add(Event.Builder.createUserChangedNameEvent("anonymoususer_2", "user4@gmail.com").withDate("2013-01-01")
                                .build());
        events.add(Event.Builder.createUserChangedNameEvent("anonymoususer_1", "user3@gmail.com").withDate("2013-01-01")
                                .build());
        events.add(Event.Builder.createUserChangedNameEvent("anonymoususer_2", "user4@gmail.com").withDate("2013-01-01")
                                .build());

        events.add(Event.Builder.createUserCreatedEvent(UID3, "user2@gmail.com", "user2@gmail.com").withDate("2013-01-01").build());
        events.add(Event.Builder.createUserCreatedEvent(UID4, "user3@gmail.com", "user3@gmail.com").withDate("2013-01-01").build());
        events.add(Event.Builder.createUserCreatedEvent(AUID1, "anonymoususer_1", "anonymoususer_1").withDate("2013-01-01").build());
        events.add(Event.Builder.createUserCreatedEvent(AUID2, "anonymoususer_2", "anonymoususer_2").withDate("2013-01-01").build());

        File log = LogGenerator.generateLog(events);

        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20130101");
        builder.put(Parameters.TO_DATE, "20130101");
        builder.put(Parameters.LOG, log.getAbsolutePath());
        builder.putAll(scriptsManager.getScript(ScriptType.CREATED_USERS_FROM_FACTORY, MetricType.CREATED_USERS_FROM_FACTORY).getParamsAsMap());
        pigServer.execute(ScriptType.CREATED_USERS_FROM_FACTORY, builder.build());
    }

    @Test
    public void shouldReturnAllUsers() throws Exception {
        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20130101");
        builder.put(Parameters.TO_DATE, "20130101");

        Metric metric = MetricFactory.getMetric(MetricType.CREATED_USERS_FROM_FACTORY);
        assertEquals(metric.getValue(builder.build()), LongValueData.valueOf(1));
    }

    @Test
    public void shouldReturnAllUsersForSpecificOrgId() throws Exception {
        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20130101");
        builder.put(Parameters.TO_DATE, "20130101");
        builder.put(MetricFilter.ORG_ID, "org1");

        Metric metric = MetricFactory.getMetric(MetricType.CREATED_USERS_FROM_FACTORY);
        assertEquals(metric.getValue(builder.build()), LongValueData.valueOf(1));
    }

    @Test
    public void shouldReturnAllUsersForSpecificAffiliateId() throws Exception {
        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20130101");
        builder.put(Parameters.TO_DATE, "20130101");
        builder.put(MetricFilter.AFFILIATE_ID, "affiliate1");

        Metric metric = MetricFactory.getMetric(MetricType.CREATED_USERS_FROM_FACTORY);
        assertEquals(metric.getValue(builder.build()), LongValueData.valueOf(1));
    }

    @Test
    public void shouldNotReturnAllUsersForSpecificAffiliateId() throws Exception {
        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20130101");
        builder.put(Parameters.TO_DATE, "20130101");
        builder.put(MetricFilter.AFFILIATE_ID, "affiliate2");

        Metric metric = MetricFactory.getMetric(MetricType.CREATED_USERS_FROM_FACTORY);
        assertEquals(metric.getValue(builder.build()), LongValueData.valueOf(0));
    }
}
