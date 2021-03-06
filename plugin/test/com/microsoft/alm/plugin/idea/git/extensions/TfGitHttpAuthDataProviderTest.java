// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.extensions;

import com.intellij.openapi.project.Project;
import com.intellij.util.AuthData;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.settings.TeamServicesSecrets;
import com.microsoft.alm.plugin.idea.common.ui.common.AzureDevOpsNotifications;
import com.microsoft.alm.plugin.idea.git.utils.TfGitHelper;
import git4idea.repo.GitRemote;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AzureDevOpsNotifications.class, GitRemote.class, TeamServicesSecrets.class, TfGitHelper.class})
public class TfGitHttpAuthDataProviderTest extends IdeaAbstractTest {
    private final String SERVER_URL = "https://dev.azure.com/username";
    private final AuthenticationInfo authenticationInfo = new AuthenticationInfo(
            "userName",
            "password",
            "serverUri",
            "userNameForDisplay");
    private final Project project = Mockito.mock(Project.class);


    private TfGitHttpAuthDataProvider authDataProvider;

    @Before
    public void setUpTest() {
        PowerMockito.mockStatic(AzureDevOpsNotifications.class, TeamServicesSecrets.class, TfGitHelper.class);

        authDataProvider = new TfGitHttpAuthDataProvider();
        ServerContext context = Mockito.mock(ServerContext.class);
        when(context.getKey()).thenReturn(ServerContext.getKey(SERVER_URL));
        when(context.getAuthenticationInfo()).thenReturn(authenticationInfo);
        ServerContextManager.getInstance().add(context);
    }

    @After
    public void cleanupTest() {
        ServerContextManager.getInstance().remove(SERVER_URL);
    }

    @Test
    public void httpAuthShouldWorkOnUrlThatDoesNotRequireConversion() {
        AuthData authData = authDataProvider.getAuthData("https://dev.azure.com/username");

        assertAuthenticationInfoEquals(authenticationInfo, authData);
    }

    @Test
    public void httpAuthShouldConvertUrlProperly() {
        AuthData authData = authDataProvider.getAuthData("https://username@dev.azure.com/");

        assertAuthenticationInfoEquals(authenticationInfo, authData);
    }

    @Test
    public void usernameShouldBeCombinedWithAnUrl() {
        TfGitHttpAuthDataProvider mockedAuthDataProvider = Mockito.mock(TfGitHttpAuthDataProvider.class);
        when(mockedAuthDataProvider.getAuthData(any(Project.class), any(String.class), any(String.class))).thenCallRealMethod();

        mockedAuthDataProvider.getAuthData(project, "https://dev.azure.com", "username");

        verify(mockedAuthDataProvider, times(1)).getAuthData(
                project,
                "https://username@dev.azure.com");
    }

    @Test
    public void testAuthDataWithValidRemoteUrl() {
        AuthData result = authDataProvider.getAuthData(project, "https://username@dev.azure.com");

        assertAuthenticationInfoEquals(authenticationInfo, result);
    }

    @Test
    public void testAuthDataWithZeroRemotes() {
        when(TfGitHelper.getTfGitRemotes(any(Project.class))).thenReturn(Collections.emptyList());
        AuthData result = authDataProvider.getAuthData(project, "https://dev.azure.com");
        assertNull(result);
    }

    @Test
    public void testAuthDataWithOneRemote() {
        GitRemote gitRemote = PowerMockito.mock(GitRemote.class);
        when(gitRemote.getFirstUrl()).thenReturn("https://dev.azure.com/username/myproject/_git/myproject");
        when(TfGitHelper.getTfGitRemotes(any(Project.class))).thenReturn(Collections.singleton(gitRemote));

        AuthData result = authDataProvider.getAuthData(project, "https://dev.azure.com");

        assertAuthenticationInfoEquals(authenticationInfo, result);
    }

    @Test
    public void testAuthDataWithTwoRemotesSameOrganization() {
        GitRemote gitRemote1 = Mockito.mock(GitRemote.class);
        when(gitRemote1.getFirstUrl()).thenReturn("https://dev.azure.com/username/myproject1/_git/myproject1");

        GitRemote gitRemote2 = Mockito.mock(GitRemote.class);
        when(gitRemote2.getFirstUrl()).thenReturn("https://dev.azure.com/username/myproject2/_git/myproject2");

        when(TfGitHelper.getTfGitRemotes(any(Project.class))).thenReturn(Arrays.asList(gitRemote1, gitRemote2));

        AuthData result = authDataProvider.getAuthData(project, "https://dev.azure.com");

        assertAuthenticationInfoEquals(authenticationInfo, result);
    }

    @Test
    public void testAuthDataWithTwoRemotesDifferentOrganizations() {
        GitRemote gitRemote1 = Mockito.mock(GitRemote.class);
        when(gitRemote1.getFirstUrl()).thenReturn("https://dev.azure.com/username1/myproject1/_git/myproject1");

        GitRemote gitRemote2 = Mockito.mock(GitRemote.class);
        when(gitRemote2.getFirstUrl()).thenReturn("https://dev.azure.com/username2/myproject2/_git/myproject2");

        when(TfGitHelper.getTfGitRemotes(any(Project.class))).thenReturn(Arrays.asList(gitRemote1, gitRemote2));

        AuthData result = authDataProvider.getAuthData(project, "https://dev.azure.com");

        assertNull(result);

        verifyStatic();
        AzureDevOpsNotifications.showManageRemoteUrlsNotification(
                project,
                "dev.azure.com");
    }

    @Test
    public void testAuthDataWithNonDevAzureUrl() {
        TfGitHttpAuthDataProvider mockedAuthDataProvider = Mockito.mock(TfGitHttpAuthDataProvider.class);
        when(mockedAuthDataProvider.getAuthData(any(Project.class), any(String.class))).thenCallRealMethod();

        mockedAuthDataProvider.getAuthData(project, "https://username.visualstudio.com");

        verify(mockedAuthDataProvider, times(1))
                .getAuthData("https://username.visualstudio.com");

    }

    private static void assertAuthenticationInfoEquals(AuthenticationInfo authenticationInfo, AuthData result) {
        assertNotNull(result);
        assertEquals(authenticationInfo.getUserName(), result.getLogin());
        assertEquals(authenticationInfo.getPassword(), result.getPassword());
    }
}
