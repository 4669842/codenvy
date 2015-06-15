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
package com.codenvy.api.subscription.saas.server.dao.sql;

import com.codenvy.api.subscription.saas.server.billing.BillingService;
import com.codenvy.api.subscription.saas.server.dao.MeterBasedStorage;
import com.google.inject.AbstractModule;

/**
 * @author Sergii Kabashniuk
 */
public class SQLModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ConnectionFactory.class).to(JndiDataSourcedConnectionFactory.class);
        bind(MeterBasedStorage.class).to(SqlMeterBasedStorage.class);
        bind(BillingService.class).to(SqlBillingService.class);
        bind(StorageInitializer.class).asEagerSingleton();
    }
}
