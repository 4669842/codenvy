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
package com.codenvy.analytics.metrics.im;

import com.codenvy.analytics.metrics.AbstractCount;
import com.codenvy.analytics.metrics.InternalMetric;
import com.codenvy.analytics.metrics.MetricFilter;
import com.codenvy.analytics.metrics.MetricType;
import com.codenvy.analytics.metrics.OmitFilters;

/** @author Anatoliy Bazko */
@InternalMetric
@OmitFilters({MetricFilter.WS_ID, MetricFilter.PERSISTENT_WS})
public class IMDownloads extends AbstractCount {

    public IMDownloads() {
        super(MetricType.IM_DOWNLOADS,
              MetricType.IM_DOWNLOADS_LIST,
              USER);
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "The number of downloaded artifacts";
    }
}
