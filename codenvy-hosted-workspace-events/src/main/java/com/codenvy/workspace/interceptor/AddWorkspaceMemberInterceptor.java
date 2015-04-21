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
package com.codenvy.workspace.interceptor;

import org.eclipse.che.api.user.server.dao.UserDao;
import org.eclipse.che.api.workspace.server.dao.Member;
import org.eclipse.che.api.workspace.server.dao.MemberDao;
import org.eclipse.che.api.workspace.shared.dto.MemberDescriptor;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.IoUtil;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.codenvy.mail.MailSenderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Intercepts calls to workspace/addMember() service and do some post actions
 * <p/>
 * Invalidate user roles
 * Sends welcome email to newly added user,
 *
 * @author Max Shaposhnik
 */
public class AddWorkspaceMemberInterceptor implements MethodInterceptor {


    @Inject
    private MailSenderClient mailSenderClient;

    @Inject
    private UserDao userDao;

    @Inject
    private MemberDao memberDao;

    @Inject
    @Named("api.endpoint")
    private String apiEndpoint;

    private static final String MAIL_TEMPLATE = "email-templates/user_added_into_workspace.html";

    private static final Logger LOG = LoggerFactory.getLogger(AddWorkspaceMemberInterceptor.class);

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object result = invocation.proceed();
        if ("addMember".equals(invocation.getMethod().getName())) {
            MemberDescriptor descriptor = (MemberDescriptor)((Response)result).getEntity();
            EnvironmentContext environmentContext = EnvironmentContext.getCurrent();
            // Do not send notifications on join to temporary ws.
            if (descriptor.getWorkspaceReference().isTemporary()) {
                return result;
            }
            List<Member> workspaceMembers = memberDao.getWorkspaceMembers(descriptor.getWorkspaceReference().getId());
            if (workspaceMembers.size() > 1) {
                try {
                    String adminEmail = "";
                    for (Member one : workspaceMembers) {
                        if (one.getRoles().contains("workspace/admin")) {
                            adminEmail = userDao.getById(one.getUserId()).getEmail();
                            break;
                        }
                    }

                    String recipientEmail = userDao.getById(descriptor.getUserId()).getEmail();
                    String senderUserid = environmentContext.getUser().getId();
                    String senderEmail = userDao.getById(senderUserid).getEmail();
                    Map<String, String> props = new HashMap<>();
                    props.put("com.codenvy.masterhost.url", apiEndpoint.substring(0, apiEndpoint.lastIndexOf("/")));
                    props.put("workspace", descriptor.getWorkspaceReference().getName());
                    props.put("usermail.whoInvited", senderEmail);
                    props.put("admin.email", adminEmail);

                    mailSenderClient.sendMail("Codenvy <noreply@codenvy.com>", recipientEmail, null,
                                              "Codenvy Workspace Invite",
                                              "text/html; charset=utf-8",
                                              IoUtil.readAndCloseQuietly(IoUtil.getResource("/" + MAIL_TEMPLATE)), props);

                    LOG.info("User added into ws message send to {}", recipientEmail);


                } catch (Exception e) {
                    LOG.warn("Unable to send user added notification email", e);
                }
            }
        }
        return result;
    }

}
