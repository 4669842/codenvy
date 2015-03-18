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

import javax.annotation.security.RolesAllowed;

/** @author Anatoliy Bazko */
@RolesAllowed({"user", "system/admin", "system/manager"})
public class CodeCompletionsBasedOnIdeUsage extends AbstractIdeUsage {
    public static final String[] SOURCE = {
            "com.codenvy.ide.ext.java.client.editor.JavaCodeAssistProcessor",
            "com.codenvy.ide.jseditor.java.client.editor.JavaCodeAssistProcessor",
            "org.eclipse.che.ide.jseditor.java.client.editor.JavaCodeAssistProcessor"
    };
    public static final String ACTION = "Autocompleting";

    public CodeCompletionsBasedOnIdeUsage() {
        super(ACTION, MetricType.CODE_COMPLETIONS_BASED_ON_IDE_USAGES, SOURCE);
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "The number of code completion actions";
    }

    /** {@inheritDoc} */
    @Override
    public String getExpandedField() {
        return PROJECT_ID;
    }
}
