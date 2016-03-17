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
package com.codenvy.analytics.metrics.tasks;

import com.codenvy.analytics.BaseTest;
import com.codenvy.analytics.datamodel.DoubleValueData;
import com.codenvy.analytics.datamodel.ListValueData;
import com.codenvy.analytics.datamodel.LongValueData;
import com.codenvy.analytics.datamodel.ValueData;
import com.codenvy.analytics.metrics.AbstractMetric;
import com.codenvy.analytics.metrics.Context;
import com.codenvy.analytics.metrics.Expandable;
import com.codenvy.analytics.metrics.Metric;
import com.codenvy.analytics.metrics.MetricFactory;
import com.codenvy.analytics.metrics.MetricType;
import com.codenvy.analytics.metrics.Parameters;
import com.codenvy.analytics.pig.scripts.ScriptType;
import com.codenvy.analytics.pig.scripts.util.Event;
import com.codenvy.analytics.pig.scripts.util.LogGenerator;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.codenvy.analytics.datamodel.ValueDataUtil.getAsDouble;
import static com.codenvy.analytics.datamodel.ValueDataUtil.getAsLong;
import static java.lang.Math.round;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/** @author Dmytro Nochevnov */
public class TestEditMetrics extends BaseTest {

    @BeforeClass
    public void setUp() throws Exception {
        prepareData();
    }

    @Test
    public void testEdits() throws Exception {
        Metric metric = MetricFactory.getMetric(MetricType.EDITS);

        LongValueData l = getAsLong(metric, Context.EMPTY);
        assertEquals(l.getAsLong(), 2);

        ListValueData expandedValue = (ListValueData)((Expandable)metric).getExpandedValue(Context.EMPTY);
        Map<String, Map<String, ValueData>> m = listToMap(expandedValue, AbstractMetric.TASK_ID);

        assertEquals(m.size(), 2);

        assertTrue(m.containsKey("session1"));
        assertTrue(m.get("session1").containsKey("id"));
        assertEquals(m.get("session1").get("id").getAsString(), "session1");

        assertTrue(m.containsKey("session2"));
        assertTrue(m.get("session2").containsKey("id"));
        assertEquals(m.get("session2").get("id").getAsString(), "session2");
    }

    @Test
    public void testGigabyteRamHours() throws Exception {
        Metric metric = MetricFactory.getMetric(MetricType.EDITS_GIGABYTE_RAM_HOURS);

        DoubleValueData d = getAsDouble(metric, Context.EMPTY);
        assertEquals(round(d.getAsDouble() * 10000), 25);

        ListValueData expandedValue = (ListValueData)((Expandable)metric).getExpandedValue(Context.EMPTY);
        Map<String, Map<String, ValueData>> m = listToMap(expandedValue, AbstractMetric.TASK_ID);

        assertTrue(m.containsKey("session1"));
        assertTrue(m.get("session1").containsKey("id"));
        assertEquals(m.get("session1").get("id").getAsString(), "session1");

        assertTrue(m.containsKey("session2"));
        assertTrue(m.get("session2").containsKey("id"));
        assertEquals(m.get("session2").get("id").getAsString(), "session2");

    }

    @Test
    public void testEditsTime() throws Exception {
        Metric metric = MetricFactory.getMetric(MetricType.EDITS_TIME);

        LongValueData l = getAsLong(metric, Context.EMPTY);
        assertEquals(l.getAsLong(), 360000);

        ListValueData expandedValue = (ListValueData)((Expandable)metric).getExpandedValue(Context.EMPTY);
        Map<String, Map<String, ValueData>> m = listToMap(expandedValue, AbstractMetric.TASK_ID);

        assertTrue(m.containsKey("session1"));
        assertTrue(m.get("session1").containsKey("id"));
        assertEquals(m.get("session1").get("id").getAsString(), "session1");

        assertTrue(m.containsKey("session2"));
        assertTrue(m.get("session2").containsKey("id"));
        assertEquals(m.get("session2").get("id").getAsString(), "session2");
    }

    private void prepareData() throws Exception {
        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20131020");
        builder.put(Parameters.TO_DATE, "20131020");
        builder.put(Parameters.LOG, initLogs().getAbsolutePath());

        builder.putAll(scriptsManager.getScript(ScriptType.TASKS, MetricType.TASKS).getParamsAsMap());
        pigServer.execute(ScriptType.TASKS, builder.build());
    }

    private File initLogs() throws Exception {
        List<Event> events = new ArrayList<>();
        events.add(Event.Builder.createSessionUsageEvent("anonymoususer_user11", "ws1", "session1", true)
                                .withDate("2013-10-20")
                                .withTime("16:00:00")
                                .build());
        events.add(Event.Builder.createSessionUsageEvent("anonymoususer_user11", "ws1", "session1", true)
                                .withDate("2013-10-20")
                                .withTime("16:03:00")
                                .build());

        events.add(Event.Builder.createSessionUsageEvent("user1@gmail.com", "ws1", "session2", false)
                                .withDate("2013-10-20")
                                .withTime("16:20:00")
                                .build());
        events.add(Event.Builder.createSessionUsageEvent("user1@gmail.com", "ws1", "session2", false)
                                .withDate("2013-10-20")
                                .withTime("16:23:00")
                                .build());

        return LogGenerator.generateLog(events);
    }
}
