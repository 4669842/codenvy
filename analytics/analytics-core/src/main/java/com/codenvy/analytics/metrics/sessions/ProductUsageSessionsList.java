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
package com.codenvy.analytics.metrics.sessions;

import com.codenvy.analytics.datamodel.ListValueData;
import com.codenvy.analytics.datamodel.MapValueData;
import com.codenvy.analytics.datamodel.StringValueData;
import com.codenvy.analytics.datamodel.ValueData;
import com.codenvy.analytics.datamodel.ValueDataUtil;
import com.codenvy.analytics.metrics.Context;
import com.codenvy.analytics.metrics.MetricType;
import com.codenvy.analytics.metrics.ReadBasedSummariziable;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** @author Anatoliy Bazko */
public class ProductUsageSessionsList extends AbstractProductUsageSessionsList implements ReadBasedSummariziable {

    public ProductUsageSessionsList() {
        super(MetricType.PRODUCT_USAGE_SESSIONS_LIST);
    }

    /** {@inheritDoc} */
    @Override
    public String[] getTrackedFields() {
        return new String[]{WS,
                            SESSIONS,
                            TIME,
                            USER,
                            USER_COMPANY,
                            DOMAIN,
                            SESSION_ID,
                            DATE,
                            END_TIME,
                            LOGOUT_INTERVAL,
                            REFERRER};
    }

    /** {@inheritDoc} */
    @Override
    public ValueData postComputation(ValueData valueData, Context clauses) throws IOException {
        List<ValueData> list2Return = new ArrayList<>();

        for (ValueData items : ((ListValueData)valueData).getAll()) {

            MapValueData prevItems = (MapValueData)items;
            Map<String, ValueData> items2Return = new HashMap<>(prevItems.getAll());

            long delta = ValueDataUtil.treatAsLong(items2Return.get(TIME));

            // replace empty session_id field on explanation message
            if (items2Return.get(SESSION_ID).getAsString().isEmpty() && delta == 0) {
                items2Return.put(SESSION_ID, StringValueData.valueOf(EMPTY_SESSION_MESSAGE));
            }

            list2Return.add(new MapValueData(items2Return));
        }

        return new ListValueData(list2Return);
    }

    /** {@inheritDoc} */
    @Override
    public DBObject[] getSpecificSummarizedDBOperations(Context clauses) {
        DBObject group = new BasicDBObject();
        group.put(ID, null);
        group.put(TIME, new BasicDBObject("$sum", "$" + TIME));

        DBObject project = new BasicDBObject();
        project.put(TIME, 1);

        return new DBObject[]{new BasicDBObject("$group", group),
                              new BasicDBObject("$project", project)};
    }


    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "Users' sessions";
    }
}
