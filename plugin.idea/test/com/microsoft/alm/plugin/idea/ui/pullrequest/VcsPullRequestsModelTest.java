// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.operations.PullRequestLookupOperation;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitPullRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Observer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class VcsPullRequestsModelTest extends IdeaAbstractTest {
    VcsPullRequestsModel underTest;

    Project projectMock;

    @Before
    public void setUp() {
        projectMock = Mockito.mock(Project.class);
    }

    @Test
    public void testObservable() {
        underTest = new VcsPullRequestsModel(projectMock);

        final Observer observerMock = Mockito.mock(Observer.class);
        underTest.addObserver(observerMock);

        underTest.setConnected(underTest.isConnected());
        verify(observerMock, never()).update(underTest, VcsPullRequestsModel.PROP_CONNECTED);
        underTest.setConnected(!underTest.isConnected());
        verify(observerMock, times(1)).update(underTest, VcsPullRequestsModel.PROP_CONNECTED);

        underTest.setAuthenticated(underTest.isAuthenticated());
        verify(observerMock, never()).update(underTest, VcsPullRequestsModel.PROP_AUTHENTICATED);
        underTest.setAuthenticated(!underTest.isAuthenticated());
        verify(observerMock, times(1)).update(underTest, VcsPullRequestsModel.PROP_AUTHENTICATED);

        underTest.setLoading(underTest.isLoading());
        verify(observerMock, never()).update(underTest, VcsPullRequestsModel.PROP_LOADING);
        underTest.setLoading(!underTest.isLoading());
        verify(observerMock, times(1)).update(underTest, VcsPullRequestsModel.PROP_LOADING);
    }

    @Test
    public void testPullRequestsTreeModel() {
        underTest = new VcsPullRequestsModel(projectMock);
        assertEquals(0, underTest.getPullRequestsTreeModel().getRequestedByMeRoot().getChildCount());

        final List<GitPullRequest> myPullRequests = new ArrayList<GitPullRequest>();
        myPullRequests.add(new GitPullRequest());
        underTest.appendPullRequests(myPullRequests, PullRequestLookupOperation.PullRequestScope.REQUESTED_BY_ME);
        assertEquals(1, underTest.getPullRequestsTreeModel().getRequestedByMeRoot().getChildCount());

        underTest.appendPullRequests(myPullRequests, PullRequestLookupOperation.PullRequestScope.ASSIGNED_TO_ME);
        assertEquals(1, underTest.getPullRequestsTreeModel().getAssignedToMeRoot().getChildCount());

        underTest.clearPullRequests();
        assertEquals(0, underTest.getPullRequestsTreeModel().getRequestedByMeRoot().getChildCount());
    }
}