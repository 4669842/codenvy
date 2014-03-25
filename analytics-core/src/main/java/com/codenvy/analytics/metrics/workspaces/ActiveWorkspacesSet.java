/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.analytics.metrics.workspaces;

import com.codenvy.analytics.metrics.*;
import com.mongodb.DBObject;

import javax.annotation.security.RolesAllowed;
import java.io.IOException;
import java.text.ParseException;

/** @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a> */
@RolesAllowed({"system/admin", "system/manager"})
public class ActiveWorkspacesSet extends AbstractSetValueResulted {

    public ActiveWorkspacesSet() {
        super(MetricType.ACTIVE_WORKSPACES_SET, WS);
    }

    @Override
    public String getStorageCollectionName() {
        return getStorageCollectionName(MetricType.USERS_ACTIVITY_LIST);
    }

    @Override
    public DBObject getFilter(Context clauses) throws ParseException, IOException {
        if (!clauses.exists(MetricFilter.WS)) {
            Context.Builder builder = new Context.Builder(clauses);
            builder.put(MetricFilter.WS, Parameters.WS_TYPES.PERSISTENT.name());

            clauses = builder.build();

        }

        return super.getFilter(clauses);
    }

    @Override
    public String getDescription() {
        return "Active workspaces list";
    }
}
