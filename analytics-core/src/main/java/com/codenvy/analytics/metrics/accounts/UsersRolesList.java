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
package com.codenvy.analytics.metrics.accounts;

import com.codenvy.analytics.datamodel.ListValueData;
import com.codenvy.analytics.datamodel.MapValueData;
import com.codenvy.analytics.datamodel.StringValueData;
import com.codenvy.analytics.datamodel.ValueData;
import com.codenvy.analytics.metrics.Context;
import com.codenvy.analytics.metrics.MetricFilter;
import com.codenvy.analytics.metrics.MetricType;
import com.codenvy.analytics.metrics.RequiredFilter;

import org.eclipse.che.api.account.shared.dto.MemberDescriptor;
import org.eclipse.che.api.user.shared.dto.UserDescriptor;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor;

import javax.annotation.security.RolesAllowed;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Alexander Reshetnyak
 */
@RolesAllowed(value = {"system/admin", "system/manager"})
@RequiredFilter(MetricFilter.ACCOUNT_ID)
public class UsersRolesList extends AbstractAccountMetric {

    public UsersRolesList() {
        super(MetricType.USERS_ROLES_LIST);
    }

    /** {@inheritDoc} */
    @Override
    public ValueData getValue(Context context) throws IOException {
        MemberDescriptor accountById = getAccountMembership(context);

        UserDescriptor currentUser = getCurrentUser();
        String currentUserId = currentUser.getId();

        List<ValueData> list2Return = new ArrayList<>();
        for (WorkspaceDescriptor workspace : getWorkspaces(accountById.getAccountReference().getId())) {
            String rolesCurrentUserInWorkspace = getUserRoleInWorkspace(currentUserId, workspace.getId());
            boolean hasAdminRoles = rolesCurrentUserInWorkspace.contains(ROLE_WORKSPACE_ADMIN.toLowerCase());

            for (MemberDescriptor member : getMembers(workspace.getId())) {
                if (hasAdminRoles || member.getUserId().equals(currentUserId)) {
                    Map<String, ValueData> m = new HashMap<>();
                    m.put(ROLES, StringValueData.valueOf(member.getRoles().toString()));
                    m.put(USER, StringValueData.valueOf(member.getUserId()));
                    m.put(WS, StringValueData.valueOf(workspace.getId()));

                    list2Return.add(new MapValueData(m));
                }
            }
        }

        list2Return = sort(list2Return, context);
        list2Return = keepSpecificPage(list2Return, context);
        return new ListValueData(list2Return);
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "Users roles in workspaces";
    }

    /** {@inheritDoc} */
    @Override
    public Class<? extends ValueData> getValueDataClass() {
        return ListValueData.class;
    }
}
