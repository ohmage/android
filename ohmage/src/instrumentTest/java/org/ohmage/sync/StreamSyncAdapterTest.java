/*
 * Copyright (C) 2013 ohmage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ohmage.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.RemoteException;

import org.apache.http.auth.AuthenticationException;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.ohmage.app.OhmageService;
import org.ohmage.auth.AuthUtil;
import org.ohmage.models.Stream;
import org.ohmage.models.Streams;
import org.ohmage.test.dagger.InjectedAndroidTestCase;

import java.io.IOException;
import java.util.ArrayList;

import javax.inject.Inject;

import retrofit.RetrofitError;
import retrofit.client.Header;
import retrofit.client.Response;
import retrofit.converter.ConversionException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Created by cketcham on 12/5/13.
 */
public class StreamSyncAdapterTest extends InjectedAndroidTestCase {

    @Inject AccountManager fakeAccountManager;

    @Inject OhmageService fakeOhmageService;

    private StreamSyncAdapter mSyncAdapter;

    private Context fakeContext;

    private Account fakeAccount;

    private static final String accessToken = "token";

    private ContentProviderClient fakeContentProviderClient;

    private SyncResult fakeSyncResult;

    private String fakeStreamId = "fakeStreamId";

    private String fakeStreamVersion = "fakeStreamVersion";

    private Stream fakeStream = new Stream(fakeStreamId, fakeStreamVersion);

    private StreamWriterOutput fakeWriter;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        fakeContext = mock(Context.class);
        fakeContentProviderClient = mock(ContentProviderClient.class);
        fakeAccount = spy(new Account("name", AuthUtil.AUTHTOKEN_TYPE));
        fakeWriter = mock(StreamWriterOutput.class);
        fakeSyncResult = new SyncResult();

        mSyncAdapter = new StreamSyncAdapter(fakeContext, false, false);
    }

    public void testOnPerformSyncForStreams_noStreams_doesNothing() throws Exception {
        Streams emptyStreams = new Streams();

        mSyncAdapter.performSyncForStreams(fakeAccount, emptyStreams, fakeWriter, fakeSyncResult);

        verifyZeroInteractions(fakeAccountManager, fakeWriter, fakeOhmageService, fakeAccount,
                fakeContentProviderClient);
    }

    public void testOnPerformSyncForStreams_cantGetAccessToken_setsAuthExceptionInSyncResult()
            throws Exception {
        Streams fakeStreams = new Streams();
        fakeStreams.add(fakeStream);
        when(fakeWriter.moveToNextBatch()).thenReturn(true, false);
        when(fakeAccountManager.blockingGetAuthToken(fakeAccount, AuthUtil.AUTHTOKEN_TYPE, true))
                .thenReturn(null);

        mSyncAdapter.performSyncForStreams(fakeAccount, fakeStreams, fakeWriter, fakeSyncResult);

        assertEquals(1, fakeSyncResult.stats.numAuthExceptions);
    }

    public void testOnPerformSyncForStreams_hasStreams_triesToUploadPoints() throws Exception {
        Streams fakeStreams = new Streams();
        fakeStreams.add(fakeStream);
        when(fakeWriter.moveToNextBatch()).thenReturn(true, false);
        when(fakeAccountManager.blockingGetAuthToken(fakeAccount, AuthUtil.AUTHTOKEN_TYPE, true))
                .thenReturn(accessToken);

        mSyncAdapter.performSyncForStreams(fakeAccount, fakeStreams, fakeWriter, fakeSyncResult);

        verify(fakeOhmageService).uploadStreamData("ohmage " + accessToken, fakeStreamId,
                fakeStreamVersion, fakeWriter);
    }

    public void testOnPerformSyncForStreams_authErrorUploading_invalidatesAuthTokenOnce()
            throws Exception {
        Streams fakeStreams = new Streams();
        fakeStreams.add(fakeStream);
        when(fakeWriter.moveToNextBatch()).thenReturn(true, false);
        when(fakeAccountManager.blockingGetAuthToken(fakeAccount, AuthUtil.AUTHTOKEN_TYPE, true))
                .thenReturn(accessToken);
        when(fakeOhmageService.uploadStreamData("ohmage " + accessToken, fakeStreamId,
                fakeStreamVersion, fakeWriter)).thenThrow(new AuthenticationException(""));

        mSyncAdapter.performSyncForStreams(fakeAccount, fakeStreams, fakeWriter, fakeSyncResult);

        verify(fakeAccountManager).invalidateAuthToken(AuthUtil.ACCOUNT_TYPE, accessToken);
    }

    public void testOnPerformSyncForStreams_authErrorUploading_triesAgainAfterInvalidating()
            throws Exception {
        Streams fakeStreams = new Streams();
        fakeStreams.add(fakeStream);
        when(fakeWriter.moveToNextBatch()).thenReturn(true, false);
        String refreshedToken = "refreshedToken";
        when(fakeAccountManager.blockingGetAuthToken(fakeAccount, AuthUtil.AUTHTOKEN_TYPE, true))
                .thenReturn(accessToken, refreshedToken);
        when(fakeOhmageService.uploadStreamData("ohmage " + accessToken, fakeStreamId,
                fakeStreamVersion, fakeWriter)).thenThrow(new AuthenticationException(""))
                .thenReturn(new Response(200, "", new ArrayList<Header>(), null));

        // TODO: Does it not throw the exception the second time?
        mSyncAdapter.performSyncForStreams(fakeAccount, fakeStreams, fakeWriter, fakeSyncResult);

        InOrder inOrder = Mockito.inOrder(fakeAccountManager, fakeOhmageService);
        inOrder.verify(fakeOhmageService).uploadStreamData("ohmage " + accessToken, fakeStreamId,
                fakeStreamVersion, fakeWriter);
        inOrder.verify(fakeAccountManager).invalidateAuthToken(AuthUtil.ACCOUNT_TYPE, accessToken);
        inOrder.verify(fakeOhmageService).uploadStreamData("ohmage " + refreshedToken, fakeStreamId,
                fakeStreamVersion, fakeWriter);
    }

    public void testOnPerformSyncForStreams_authErrorAfterRefreshing_setsAuthExceptionInSyncResult()
            throws Exception {
        Streams fakeStreams = new Streams();
        fakeStreams.add(fakeStream);
        when(fakeWriter.moveToNextBatch()).thenReturn(true, false);
        when(fakeAccountManager.blockingGetAuthToken(fakeAccount, AuthUtil.AUTHTOKEN_TYPE, true))
                .thenReturn(accessToken);
        when(fakeOhmageService.uploadStreamData("ohmage " + accessToken, fakeStreamId,
                fakeStreamVersion, fakeWriter)).thenThrow(new AuthenticationException(""));

        mSyncAdapter.performSyncForStreams(fakeAccount, fakeStreams, fakeWriter, fakeSyncResult);

        assertEquals(1, fakeSyncResult.stats.numAuthExceptions);
    }

    public void testOnPerformSyncForStreams_networkError_setsIOExceptionInSyncResult()
            throws Exception {
        Streams fakeStreams = new Streams();
        fakeStreams.add(fakeStream);
        when(fakeWriter.moveToNextBatch()).thenReturn(true, false);
        when(fakeAccountManager.blockingGetAuthToken(fakeAccount, AuthUtil.AUTHTOKEN_TYPE, true))
                .thenReturn(accessToken);
        when(fakeOhmageService.uploadStreamData("ohmage " + accessToken, fakeStreamId,
                fakeStreamVersion, fakeWriter))
                .thenThrow(RetrofitError.networkError("", new IOException()));

        mSyncAdapter.performSyncForStreams(fakeAccount, fakeStreams, fakeWriter, fakeSyncResult);

        assertEquals(1, fakeSyncResult.stats.numIoExceptions);
    }

    public void testOnPerformSyncForStreams_invalidData_setsParseExceptionInSyncResult()
            throws Exception {
        Streams fakeStreams = new Streams();
        fakeStreams.add(fakeStream);
        when(fakeWriter.moveToNextBatch()).thenReturn(true, false);
        when(fakeAccountManager.blockingGetAuthToken(fakeAccount, AuthUtil.AUTHTOKEN_TYPE, true))
                .thenReturn(accessToken);
        when(fakeOhmageService.uploadStreamData("ohmage " + accessToken, fakeStreamId,
                fakeStreamVersion, fakeWriter))
                .thenThrow(RetrofitError.conversionError("", null, null, null,
                        new ConversionException("")));

        mSyncAdapter.performSyncForStreams(fakeAccount, fakeStreams, fakeWriter, fakeSyncResult);

        assertEquals(1, fakeSyncResult.stats.numParseExceptions);
    }

    public void testOnPerformSyncForStreams_httpError_setsSkippedEntriesInSyncResult()
            throws Exception {
        Streams fakeStreams = new Streams();
        fakeStreams.add(fakeStream);
        when(fakeWriter.moveToNextBatch()).thenReturn(true, false);
        when(fakeAccountManager.blockingGetAuthToken(fakeAccount, AuthUtil.AUTHTOKEN_TYPE, true))
                .thenReturn(accessToken);
        when(fakeOhmageService.uploadStreamData("ohmage " + accessToken, fakeStreamId,
                fakeStreamVersion, fakeWriter))
                .thenThrow(RetrofitError.httpError("", null, null, null));

        mSyncAdapter.performSyncForStreams(fakeAccount, fakeStreams, fakeWriter, fakeSyncResult);

        assertEquals(1, fakeSyncResult.stats.numSkippedEntries);
    }

    public void testOnPerformSyncForStreams_remoteExceptionAccessingDb_setsDbErrorInSyncResult()
            throws Exception {
        Streams fakeStreams = new Streams();
        fakeStreams.add(fakeStream);
        when(fakeWriter.moveToNextBatch()).thenReturn(true, false);
        when(fakeAccountManager.blockingGetAuthToken(fakeAccount, AuthUtil.AUTHTOKEN_TYPE, true))
                .thenReturn(accessToken);
        doThrow(new RemoteException()).when(fakeWriter).query(anyString(), any(Stream.class));

        mSyncAdapter.performSyncForStreams(fakeAccount, fakeStreams, fakeWriter, fakeSyncResult);

        assertTrue(fakeSyncResult.databaseError);
    }

    public void testOnPerformSyncForStreams_noError_closesWriter()
            throws Exception {
        Streams fakeStreams = new Streams();
        fakeStreams.add(fakeStream);
        when(fakeWriter.moveToNextBatch()).thenReturn(true, false);
        when(fakeAccountManager.blockingGetAuthToken(fakeAccount, AuthUtil.AUTHTOKEN_TYPE, true))
                .thenReturn(accessToken);

        mSyncAdapter.performSyncForStreams(fakeAccount, fakeStreams, fakeWriter, fakeSyncResult);

        verify(fakeWriter).close();
    }

    public void testOnPerformSyncForStreams_retroFitError_closesWriter()
            throws Exception {
        Streams fakeStreams = new Streams();
        fakeStreams.add(fakeStream);
        when(fakeWriter.moveToNextBatch()).thenReturn(true, false);
        when(fakeAccountManager.blockingGetAuthToken(fakeAccount, AuthUtil.AUTHTOKEN_TYPE, true))
                .thenReturn(accessToken);
        when(fakeOhmageService.uploadStreamData("ohmage " + accessToken, fakeStreamId,
                fakeStreamVersion, fakeWriter))
                .thenThrow(RetrofitError.unexpectedError("", null));

        mSyncAdapter.performSyncForStreams(fakeAccount, fakeStreams, fakeWriter, fakeSyncResult);

        verify(fakeWriter).close();
    }

    public void testOnPerformSyncForStreams_authError_closesWriter()
            throws Exception {
        Streams fakeStreams = new Streams();
        fakeStreams.add(fakeStream);
        when(fakeWriter.moveToNextBatch()).thenReturn(true, false);
        when(fakeAccountManager.blockingGetAuthToken(fakeAccount, AuthUtil.AUTHTOKEN_TYPE, true))
                .thenReturn(accessToken);
        when(fakeOhmageService.uploadStreamData("ohmage " + accessToken, fakeStreamId,
                fakeStreamVersion, fakeWriter))
                .thenThrow(new AuthenticationException());

        mSyncAdapter.performSyncForStreams(fakeAccount, fakeStreams, fakeWriter, fakeSyncResult);

        verify(fakeWriter).close();
    }
}