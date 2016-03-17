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
package com.codenvy.analytics.metrics.sessions.factory;

import com.codenvy.analytics.datamodel.ListValueData;
import com.codenvy.analytics.datamodel.ValueData;
import com.codenvy.analytics.metrics.Context;
import com.codenvy.analytics.metrics.MetricFilter;
import com.codenvy.analytics.metrics.MetricType;
import com.codenvy.analytics.metrics.OmitFilters;
import com.codenvy.analytics.metrics.ReadBasedMetric;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.util.ArrayList;
import java.util.List;

/** @author Alexander Reshetnyak */
@OmitFilters({MetricFilter.WS_ID, MetricFilter.PERSISTENT_WS})
public class ReferrersCountToSpecificFactory extends ReadBasedMetric {

    public static final String UNIQUE_REFERRERS_COUNT = "unique_referrers_count";

    public ReferrersCountToSpecificFactory() {
        super(MetricType.REFERRERS_COUNT_TO_SPECIFIC_FACTORY);
    }

    /** {@inheritDoc} */
    @Override
    public String[] getTrackedFields() {
        return new String[]{FACTORY, UNIQUE_REFERRERS_COUNT};
    }

    /** {@inheritDoc} */
    @Override
    public DBObject[] getSpecificDBOperations(Context clauses) {
        List<DBObject> dbOperations = new ArrayList<>();

        DBObject group = new BasicDBObject();
        group.put(ID, "$" + FACTORY);
        group.put("referrers", new BasicDBObject("$addToSet", "$" + REFERRER));
        dbOperations.add(new BasicDBObject("$group", group));

        dbOperations.add(new BasicDBObject("$unwind", "$referrers"));

        group = new BasicDBObject();
        group.put(ID, "$_id");
        group.put(UNIQUE_REFERRERS_COUNT, new BasicDBObject("$sum", 1));
        dbOperations.add(new BasicDBObject("$group", group));

        DBObject project = new BasicDBObject();
        project.put(FACTORY, "$_id");
        project.put(UNIQUE_REFERRERS_COUNT, 1);
        dbOperations.add(new BasicDBObject("$project", project));

        return dbOperations.toArray(new DBObject[dbOperations.size()]);
    }

    /** {@inheritDoc} */
    @Override
    public String getStorageCollectionName() {
        return getStorageCollectionName(MetricType.PRODUCT_USAGE_FACTORY_SESSIONS);
    }

    /** {@inheritDoc} */
    @Override
    public Class<? extends ValueData> getValueDataClass() {
        return ListValueData.class;
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "The referrers count to a specific factory";
    }
}
