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

import com.codenvy.analytics.metrics.AbstractListValueResulted;
import com.codenvy.analytics.metrics.Context;
import com.codenvy.analytics.metrics.InternalMetric;
import com.codenvy.analytics.metrics.MetricFactory;
import com.codenvy.analytics.metrics.MetricFilter;
import com.codenvy.analytics.metrics.MetricType;
import com.codenvy.analytics.metrics.OmitFilters;
import com.codenvy.analytics.metrics.PrecomputedDataMetric;
import com.codenvy.analytics.metrics.ReadBasedSummariziable;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author Alexander Reshetnyak
 */
@InternalMetric
@OmitFilters({MetricFilter.WS_ID, MetricFilter.PERSISTENT_WS})
public class FactoryStatisticsListPrecomputed extends AbstractListValueResulted implements PrecomputedDataMetric, ReadBasedSummariziable {

    public FactoryStatisticsListPrecomputed() {
        super(MetricType.FACTORY_STATISTICS_LIST_PRECOMPUTED);
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "The statistic of factory";
    }

    /** {@inheritDoc} */
    @Override
    public String getStorageCollectionName() {
        return getStorageCollectionName(MetricType.FACTORY_STATISTICS_PRECOMPUTED);
    }

    /** {@inheritDoc} */
    @Override
    public String[] getTrackedFields() {
        return new String[]{FACTORY,
                            WS_CREATED,
                            SESSIONS,
                            TIME,
                            BUILDS,
                            BUILDS_GIGABYTE_RAM_HOURS,
                            RUNS,
                            RUNS_GIGABYTE_RAM_HOURS,
                            DEBUGS,
                            DEBUGS_GIGABYTE_RAM_HOURS,
                            DEPLOYS,
                            AUTHENTICATED_SESSION,
                            CONVERTED_SESSION,
                            ENCODED_FACTORY,
                            ORG_ID,
                            PROJECT_TYPE,
                            EDITS_GIGABYTE_RAM_HOURS,
                            FACTORY_ROUTING_FLAGS};
    }

    /** {@inheritDoc} */
    @Override
    public DBObject[] getSpecificSummarizedDBOperations(Context clauses) {
        ReadBasedSummariziable summariziable = (ReadBasedSummariziable)MetricFactory.getMetric(getBasedMetric());
        DBObject[] dbOperations = summariziable.getSpecificSummarizedDBOperations(clauses);

        ((DBObject)(dbOperations[1].get("$group"))).put(SESSIONS, new BasicDBObject("$sum", "$" + SESSIONS));
        ((DBObject)(dbOperations[1].get("$group"))).put(AUTHENTICATED_SESSION, new BasicDBObject("$sum", "$" + AUTHENTICATED_SESSION));

        return dbOperations;
    }

    /** {@inheritDoc} */
    @Override
    public Context getContextForBasedMetric() {
        return Context.EMPTY;
    }

    /** {@inheritDoc} */
    @Override
    public MetricType getBasedMetric() {
        return MetricType.FACTORY_STATISTICS_LIST;
    }


    /** {@inheritDoc} */
    @Override
    public boolean canReadPrecomputedData(Context context) {
        return true;
    }
}
