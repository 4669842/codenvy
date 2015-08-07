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
package com.codenvy.analytics.metrics.users;

import com.codenvy.analytics.metrics.AbstractActiveEntities;
import com.codenvy.analytics.metrics.Context;
import com.codenvy.analytics.metrics.MetricFilter;
import com.codenvy.analytics.metrics.MetricType;
import com.codenvy.analytics.metrics.OmitFilters;
import com.codenvy.analytics.metrics.Parameters;
import com.codenvy.analytics.metrics.WithoutFromDateParam;

import java.io.IOException;

/**
 * @author Anatoliy Bazko
 */
@OmitFilters({MetricFilter.WS_ID, MetricFilter.PERSISTENT_WS})
public class ActiveUsersFromBeginning extends AbstractActiveEntities implements WithoutFromDateParam {

    public ActiveUsersFromBeginning() {
        super(MetricType.ACTIVE_USERS_FROM_BEGINNING, MetricType.ACTIVE_USERS_SET, USER);
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "The number of active registered users";
    }

    /** {@inheritDoc} */
    @Override
    public Context applySpecificFilter(Context clauses) throws IOException {
        Context.Builder builder = new Context.Builder(clauses);
        builder.putDefaultValue(Parameters.FROM_DATE);
        if (!clauses.exists(MetricFilter.USER_ID)) {
            builder.put(MetricFilter.REGISTERED_USER, 1);
        }

        return super.applySpecificFilter(builder.build());
    }
}
