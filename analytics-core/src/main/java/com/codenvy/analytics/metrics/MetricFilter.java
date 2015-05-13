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
package com.codenvy.analytics.metrics;

/** @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a> */
public enum MetricFilter {
    PARAMETERS,

    _ID,
    DATE(true),

    WS,                     // either id or name
    WS_ID,                  // id
    WS_NAME,                // name
    PERSISTENT_WS(true),    // persistent flag

    USER,                   // either id of name
    USER_ID,                // id
    ALIASES,                // alias
    REGISTERED_USER(true),  // registered flag

    EVENT,
    DOMAIN,
    USER_COMPANY,
    USER_LAST_NAME,
    USER_FIRST_NAME,

    ACTION,
    SOURCE,

    ORG_ID,
    FACTORY,
    FACTORY_ID,
    ENCODED_FACTORY(true),
    TIME(true),
    LIFETIME(true),
    TIMEOUT(true),
    STOPPED_BY_USER(true),
    FINISHED_NORMALLY(true),
    BUILDS(true),
    CONVERTED_FACTORY_SESSION(true),
    RUNS(true),
    DEPLOYS(true),
    REFERRER,
    REPOSITORY,
    SESSION_ID,
    AFFILIATE_ID,
    PROJECT,
    ACCOUNT_ID,
    DATA_UNIVERSE(true),
    PROJECT_TYPE,
    TASK_ID,
    TASK_TYPE,
    STOP_TIME,
    SHUTDOWN_TYPE,
    LAUNCH_TYPE,
    ARTIFACT,
    VERSION,
    EXISTS;

    private boolean isNumeric;

    MetricFilter(boolean isNumeric) {
        this.isNumeric = isNumeric;
    }

    MetricFilter() {
        this.isNumeric = false;
    }

    public boolean isNumericType() {
        return isNumeric;
    }
}

