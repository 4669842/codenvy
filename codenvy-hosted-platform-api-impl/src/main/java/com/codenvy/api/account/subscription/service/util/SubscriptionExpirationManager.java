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
package com.codenvy.api.account.subscription.service.util;

import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.account.server.dao.Subscription;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Send to user email notification about subscription expiration and update properties.
 *
 * @author Alexander Garagatyi
 */
public class SubscriptionExpirationManager {
    private static final Logger LOG = LoggerFactory.getLogger(SubscriptionExpirationManager.class);

    @Inject
    private AccountDao accountDao;

    @Inject
    private SubscriptionMailSender mailUtil;

    public void sendEmailAboutExpiringTrial(String serviceId, Integer days) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.DATE, days);

            List<Subscription> subscriptions = accountDao.getSubscriptionQueryBuilder().getExpiringQuery(serviceId, days).execute();
            for (Subscription subscription : subscriptions) {
                try {
                    mailUtil.sendSubscriptionExpiredNotification(subscription.getAccountId(), days);
                    subscription.getProperties().put(String.format("codenvy:subscription-email-trialExpiring-%s", days), "true");
                    accountDao.updateSubscription(subscription);
                } catch (ServerException | NotFoundException | IOException | MessagingException e) {
                    LOG.error(e.getLocalizedMessage(), e);
                }
            }
        } catch (ServerException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }

    public void sendEmailAboutExpiredTrial(String serviceId, Integer days) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.DATE, -days);
            List<Subscription> subscriptions = accountDao.getSubscriptionQueryBuilder().getExpiredQuery(serviceId, days).execute();
            for (Subscription subscription : subscriptions) {
                try {
                    mailUtil.sendSubscriptionExpiredNotification(subscription.getAccountId(), days);
                    subscription.getProperties().put(String.format("codenvy:subscription-email-trialExpired-%s", days), "true");
                    accountDao.updateSubscription(subscription);
                } catch (ServerException | NotFoundException | IOException | MessagingException e) {
                    LOG.error(e.getLocalizedMessage(), e);
                }
            }
        } catch (ServerException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }
}
