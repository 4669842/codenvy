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
package com.codenvy.analytics.metrics.workspaces;

import com.codenvy.analytics.datamodel.LongValueData;
import com.codenvy.analytics.datamodel.ValueData;
import com.codenvy.analytics.datamodel.ValueDataUtil;
import com.codenvy.analytics.metrics.CalculatedMetric;
import com.codenvy.analytics.metrics.Context;
import com.codenvy.analytics.metrics.Expandable;
import com.codenvy.analytics.metrics.MetricType;

import java.io.IOException;

/** @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a> */
public class ReturningActiveWorkspaces extends CalculatedMetric implements Expandable {

    public ReturningActiveWorkspaces() {
        super(MetricType.RETURNING_ACTIVE_WORKSPACES, new MetricType[]{MetricType.ACTIVE_WORKSPACES,
                                                                       MetricType.CREATED_WORKSPACES});
    }

    /** {@inheritDoc} */
    @Override
    public ValueData getValue(Context context) throws IOException {
        LongValueData active = ValueDataUtil.getAsLong(basedMetric[0], context);
        LongValueData created = ValueDataUtil.getAsLong(basedMetric[1], context);
        return active.subtract(created);
    }

    /** {@inheritDoc} */
    @Override
    public Class<? extends ValueData> getValueDataClass() {
        return LongValueData.class;
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "Non-active workspaces";
    }

    /** {@inheritDoc} */
    @Override
    public ValueData getExpandedValue(Context context) throws IOException {
        ValueData active = ((Expandable)basedMetric[0]).getExpandedValue(context);
        ValueData created = ((Expandable)basedMetric[1]).getExpandedValue(context);
        return active.subtract(created);
    }

    /** {@inheritDoc} */
    @Override
    public String getExpandedField() {
        return ((Expandable)basedMetric[0]).getExpandedField();
    }
}
