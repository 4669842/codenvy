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

import org.aopalliance.intercept.MethodInvocation;
import org.codenvy.mail.MailSenderClient;
import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.user.server.dao.User;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.eclipse.che.api.workspace.server.WorkspaceService;
import org.eclipse.che.api.workspace.server.dao.Member;
import org.eclipse.che.api.workspace.server.dao.MemberDao;
import org.eclipse.che.api.workspace.shared.dto.MemberDescriptor;
import org.eclipse.che.api.workspace.shared.dto.NewMembership;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceReference;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.UserImpl;
import org.eclipse.che.dto.server.DtoFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.ws.rs.core.SecurityContext;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.status;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.anyMapOf;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@Listeners(value = {MockitoTestNGListener.class})
public class AddWorkspaceMemberInterceptorTest {
    private Field notificationTurnedOn;

    @Mock
    private MailSenderClient mailSenderClient;

    @Mock
    private UserDao userDao;

    @Mock
    private MemberDao memberDao;

    @Mock
    private AccountDao accountDao;

    @Mock
    private EnvironmentContext environmentContext;

    @Mock
    private User user;

    @Mock
    private MethodInvocation invocation;

    @Mock
    private MemberDescriptor memberDescriptor;

    @InjectMocks
    private AddWorkspaceMemberInterceptor interceptor;

    @BeforeMethod
    public void setup() throws Exception {
        notificationTurnedOn = interceptor.getClass().getDeclaredField("sendEmailOnMemberAdded");
        notificationTurnedOn.setAccessible(true);
        notificationTurnedOn.set(interceptor, true);

        EnvironmentContext context = EnvironmentContext.getCurrent();
        context.setUser(new UserImpl("test@user2.com", "askd123123", null, null, false));
        context.setAccountId("AccountID");

    }

    @Test(expectedExceptions = ConflictException.class)
    public void shouldNotSendEmailIfInvocationThrowsException() throws Throwable {
        when(invocation.proceed()).thenThrow(new ConflictException("conflict"));
        interceptor.invoke(invocation);
        verifyZeroInteractions(mailSenderClient);
    }

    @Test
    public void shouldNotSendEmailIfJoinToTemporaryWs() throws Throwable {
        Method method =
                WorkspaceService.class.getMethod("addMember", String.class, NewMembership.class, SecurityContext.class);
        when(invocation.proceed()).thenReturn(status(CREATED).entity(memberDescriptor).build());
        when(invocation.getMethod()).thenReturn(method);
        when(memberDescriptor.getWorkspaceReference())
                .thenReturn(DtoFactory.getInstance().createDto(WorkspaceReference.class).withName("testWSName").withTemporary(true));

        interceptor.invoke(invocation);

        verifyZeroInteractions(mailSenderClient);
    }

    @Test
    public void shouldNotSendEmailIfNotificationIsTurnedOff() throws Throwable {
        notificationTurnedOn.set(interceptor, false);
        interceptor.invoke(invocation);

        verifyZeroInteractions(mailSenderClient);
    }

    @Test
    public void shouldSendEmail() throws Throwable {

        String recipient = "test@user.com";
        Method method =
                WorkspaceService.class.getMethod("addMember", String.class, NewMembership.class, SecurityContext.class);
        Field f = interceptor.getClass().getDeclaredField("apiEndpoint");
        f.setAccessible(true);
        f.set(interceptor, "http://dev.box.com/api");
        Map<String, String> profileAttributes = new HashMap<>();
        profileAttributes.put("firstName", "First");
        profileAttributes.put("lastName", "Last");

        when(invocation.proceed()).thenReturn(status(CREATED).entity(memberDescriptor).build());
        when(invocation.getMethod()).thenReturn(method);
        when(memberDescriptor.getUserId()).thenReturn("user1234566");
        when(memberDescriptor.getWorkspaceReference())
                .thenReturn(DtoFactory.getInstance().createDto(WorkspaceReference.class).withId("ws29301").withName("testWSName"));
        when(userDao.getById(anyString())).thenReturn(user);
        when(user.getEmail()).thenReturn(recipient);

        when(memberDao.getWorkspaceMembers(eq("ws29301"))).thenReturn(Arrays.asList(new Member(), new Member()));

        List<String> accountRole = Arrays.asList("account/owner");
        when(accountDao.getMembers(eq("AccountID"))).thenReturn(Arrays.asList(new org.eclipse.che.api.account.server.dao.Member().withRoles(accountRole)));

        interceptor.invoke(invocation);
        verify(mailSenderClient)
                .sendMail(anyString(), eq(recipient), anyString(), anyString(), eq("text/html; charset=utf-8"),
                          anyString(), anyMapOf(String.class, String.class));
    }

    @Test
    public void shouldNotSendEmailOnFirstUser() throws Throwable {
        String recipient = "test@user.com";
        Method method = WorkspaceService.class.getMethod("addMember", String.class, NewMembership.class, SecurityContext.class);
        Field f = interceptor.getClass().getDeclaredField("apiEndpoint");
        f.setAccessible(true);
        f.set(interceptor, "http://dev.box.com/api");
        Map<String, String> profileAttributes = new HashMap<>();
        profileAttributes.put("firstName", "First");
        profileAttributes.put("lastName", "Last");

        when(invocation.proceed()).thenReturn(status(CREATED).entity(memberDescriptor).build());
        when(invocation.getMethod()).thenReturn(method);
        when(memberDescriptor.getUserId()).thenReturn("user1234566");
        when(memberDescriptor.getWorkspaceReference())
                .thenReturn(DtoFactory.getInstance().createDto(WorkspaceReference.class).withId("ws29301").withName("testWSName"));
        when(userDao.getById(anyString())).thenReturn(user);
        when(user.getEmail()).thenReturn(recipient);

        when(memberDao.getWorkspaceMembers(eq("ws29301"))).thenReturn(Arrays.asList(new Member()));


        interceptor.invoke(invocation);
        verifyZeroInteractions(mailSenderClient);
    }
}
