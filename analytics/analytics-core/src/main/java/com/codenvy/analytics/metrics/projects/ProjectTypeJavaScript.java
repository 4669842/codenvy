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
package com.codenvy.analytics.metrics.projects;

import com.codenvy.analytics.metrics.MetricType;

/** @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a> */
public class ProjectTypeJavaScript extends AbstractProjectType {

    public ProjectTypeJavaScript() {
        super(MetricType.PROJECT_TYPE_JAVASCRIPT, new String[]{JS,
                                                               GRANT_JS,
                                                               GULP_JS,
                                                               BASIC_JS,
                                                               ANGULAR_JAVA_SCRIPT,
                                                               HTML});
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "The number of JavaScript projects";
    }
}
