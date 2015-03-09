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
package com.codenvy.auth.sso.server.handler;

import org.eclipse.che.api.auth.AuthenticationException;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.user.server.dao.Profile;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.eclipse.che.api.user.server.dao.UserProfileDao;
import org.eclipse.che.api.user.server.dao.User;
import com.codenvy.service.http.IdeVersionHolder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Disallows login to IDE2 for users, who created after the specified time.
 */

@Singleton
public class RestrictedAccessBearerTokenAuthenticationHandler extends BearerTokenAuthenticationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RestrictedAccessBearerTokenAuthenticationHandler.class);

    private UserDao        userDao;
    private UserProfileDao profileDao;
    private        long             ide2LoginLimit = Long.MAX_VALUE;
    private static SimpleDateFormat format         = new SimpleDateFormat("yyyy-MM-dd");

    @Inject
    public RestrictedAccessBearerTokenAuthenticationHandler(UserProfileDao profileDao, UserDao userDao,
                                                            @Nullable @Named("ide2.login.limit.time") String limitTime)
            throws ParseException {
        this.profileDao = profileDao;
        this.userDao = userDao;
        if (limitTime != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(format.parse(limitTime));
            this.ide2LoginLimit = calendar.getTimeInMillis();
        }
    }

    @Override
    public void authenticate(final String userId, final String userSecret) throws AuthenticationException {
        if (IdeVersionHolder.get()) {
            try {
                User user = userDao.getByAlias(userId);
                Profile profile = profileDao.getById(user.getId());
                Long created = profile.getAttributes().containsKey("codenvy:created") ? Long
                        .parseLong(profile.getAttributes().get("codenvy:created")) : Long.MIN_VALUE;
                if (created.compareTo(ide2LoginLimit) > 0) {
                    throw new AuthenticationException(401,
                                                      "Authentication failed. Please use latest Codenvy version.");
                }
            } catch (NotFoundException nf) {
                /* Uncomment to block IDE2 registrations
                    throw new AuthenticationException(409,
                                                      "Registrations to this Codenvy version are closed. Please use " +
                                                      "latest Codenvy version."
                    );
                } */
            } catch (ApiException e) {
                LOG.debug(e.getLocalizedMessage(), e);
                throw new AuthenticationException(401, "Authentication failed. Please check username and password.");
            }
        }
        super.authenticate(userId, userSecret);
    }
}
