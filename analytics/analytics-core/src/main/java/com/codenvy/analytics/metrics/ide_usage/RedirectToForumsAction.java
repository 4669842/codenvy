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
package com.codenvy.analytics.metrics.ide_usage;

import com.codenvy.analytics.metrics.MetricType;

/**
 * @author Dmytro Nochevnov
 */
public class RedirectToForumsAction extends AbstractIdeUsage {
    public static final String[] SOURCE = {
            "com.codenvy.ide.actions.RedirectToForumsAction",
            "org.eclipse.che.ide.actions.RedirectToForumsAction",
            "org.eclipse.che.ide.ext.help.client.RedirectToForumsAction"
    };

    public RedirectToForumsAction() {
        super(MetricType.REDIRECT_TO_FORUMS_ACTION, SOURCE);
    }
}
