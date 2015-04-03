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
package com.codenvy.api.account.subscription;

import com.codenvy.api.account.subscription.factory.FactorySubscriptionService;
import com.codenvy.api.account.subscription.onpremises.OnPremisesSubscriptionService;
import com.codenvy.api.account.subscription.saas.SaasSubscriptionService;
import com.codenvy.api.account.subscription.saas.job.RefillJob;
import com.codenvy.api.account.subscription.saas.limit.ActiveTasksHolder;
import com.codenvy.api.account.subscription.saas.limit.ResourcesUsageLimitProvider;
import com.codenvy.api.account.subscription.schedulers.GenerateInvoicesJob;
import com.codenvy.api.account.subscription.schedulers.MailScheduler;
import com.codenvy.api.account.subscription.schedulers.MeterBasedCharger;
import com.codenvy.api.account.subscription.schedulers.SubscriptionScheduler;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import org.eclipse.che.api.account.server.SubscriptionService;

/**
 * @author Sergii Kabashniuk
 */
public class SubscriptionModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<SubscriptionService> subscriptionServiceBinder =
                Multibinder.newSetBinder(binder(), org.eclipse.che.api.account.server.SubscriptionService.class);
        subscriptionServiceBinder.addBinding().to(FactorySubscriptionService.class);
        subscriptionServiceBinder.addBinding().to(OnPremisesSubscriptionService.class);
        subscriptionServiceBinder.addBinding().to(SaasSubscriptionService.class);

        bind(ActiveTasksHolder.class).asEagerSingleton();
        bind(ResourcesUsageLimitProvider.class).asEagerSingleton();
        bind(RefillJob.class);
        bind(MeterBasedCharger.class);
        bind(MailScheduler.class);
        bind(GenerateInvoicesJob.class);

        bind(SubscriptionScheduler.class).asEagerSingleton();
    }
}
