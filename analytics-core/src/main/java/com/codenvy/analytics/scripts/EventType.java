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

package com.codenvy.analytics.scripts;


/** @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a> */
public enum EventType {
    USER_CODE_REFACTOR,
    BUILD_STARTED,
    BUILD_FINISHED,
    RUN_STARTED,
    RUN_FINISHED,
    DEBUG_STARTED,
    DEBUG_FINISHED,
    TENANT_CREATED,
    TENANT_DESTROYED;

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return super.toString().toLowerCase().replace("_", "-");
    }
}
