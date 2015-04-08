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

import org.eclipse.che.api.account.shared.dto.MemberDescriptor;
import org.eclipse.che.api.user.shared.dto.ProfileDescriptor;

import javax.annotation.security.RolesAllowed;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** @author Alexander Reshetnyak */
@RolesAllowed(value = {"system/admin", "system/manager"})
public class AccountsList extends AbstractAccountMetric {

    public AccountsList() {
        super(MetricType.ACCOUNTS_LIST);
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "Accounts data";
    }

    /** {@inheritDoc} */
    @Override
    public Class<? extends ValueData> getValueDataClass() {
        return ListValueData.class;
    }

    /** {@inheritDoc} */
    @Override
    public ValueData getValue(Context context) throws IOException {
        ProfileDescriptor profile = getProfile();
        List<MemberDescriptor> accountMemberships = getAccountMemberships();

        List<ValueData> list2Return = new ArrayList<>();
        String accountId = context.getAsString(MetricFilter.ACCOUNT_ID);

        for (MemberDescriptor accountMembership : accountMemberships) {
            if (!context.exists(MetricFilter.ACCOUNT_ID) || accountMembership.getAccountReference().getId().equals(accountId)) {

                Map<String, ValueData> m = new HashMap<>();
                m.put(ACCOUNT_ID, StringValueData.valueOf(accountMembership.getAccountReference().getId()));
                m.put(ACCOUNT_NAME, StringValueData.valueOf(accountMembership.getAccountReference().getName()));
                m.put(ACCOUNT_ROLES, StringValueData.valueOf(accountMembership.getRoles().toString()));
                try {
                    m.put(ACCOUNT_ATTRIBUTES, StringValueData.valueOf(getAccountDescriptorById(accountMembership.getAccountReference().getId())
                                                                              .getAttributes().toString()));
                } catch (IOException e) { // if use isn't owner of the account
                    m.put(ACCOUNT_ATTRIBUTES, StringValueData.DEFAULT);
                }
                m.put(ACCOUNT_USER_ID, StringValueData.valueOf(accountMembership.getUserId()));
                m.put(ACCOUNT_PROFILE_EMAIL, getEmail(profile));
                m.put(ACCOUNT_PROFILE_NAME, getFullName(profile));

                list2Return.add(new MapValueData(m));
            }
        }

        return new ListValueData(list2Return);
    }
}
