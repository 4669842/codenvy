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

import com.codenvy.analytics.metrics.AbstractCount;
import com.codenvy.analytics.metrics.MetricType;

/** @author Dmytro Nochevnov  */
public class Tasks extends AbstractCount {
    public static final String BUILDER  = "builder";
    public static final String RUNNER   = "runner";
    public static final String DEBUGGER = "debugger";
    public static final String EDITOR   = "editor";

    public Tasks() {
        this(MetricType.TASKS);
    }

    public Tasks(MetricType metricType) {
        super(metricType, MetricType.TASKS_LIST, TASK_ID);
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "The total number of tasks";
    }
}
