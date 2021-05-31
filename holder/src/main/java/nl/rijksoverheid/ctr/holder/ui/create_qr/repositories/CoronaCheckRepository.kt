package nl.rijksoverheid.ctr.holder.ui.create_qr.repositories

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.rijksoverheid.ctr.holder.ui.create_qr.api.HolderApiClient
import nl.rijksoverheid.ctr.holder.ui.create_qr.models.*
import nl.rijksoverheid.ctr.holder.ui.create_qr.models.post.AccessTokenPostData
import nl.rijksoverheid.ctr.holder.ui.create_qr.models.post.GetCredentialsPostData
import nl.rijksoverheid.ctr.holder.ui.create_qr.models.post.GetTestIsmPostData
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Converter
import retrofit2.HttpException

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

interface CoronaCheckRepository {
    suspend fun configProviders(): RemoteConfigProviders
    suspend fun accessTokens(tvsToken: String): RemoteAccessTokens
    suspend fun getTestIsm(test: String, sToken: String, icm: String): TestIsmResult
    suspend fun remoteNonce(): RemoteNonce
    suspend fun getCredentials(
        stoken: String,
        events: List<String>,
        issueCommitmentMessage: String
    ): RemoteCredentials

    suspend fun getPrepareIssue(): RemotePrepareIssue
}

open class CoronaCheckRepositoryImpl(
    private val api: HolderApiClient,
    private val errorResponseConverter: Converter<ResponseBody, ResponseError>
) : CoronaCheckRepository {

    override suspend fun configProviders(): RemoteConfigProviders {
        return api.getConfigCtp()
    }

    override suspend fun accessTokens(tvnToken: String): RemoteAccessTokens {
        return api.getAccessTokens(
            data = AccessTokenPostData("999999011")
        )
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun getTestIsm(
        test: String,
        sToken: String,
        icm: String
    ): TestIsmResult {
        val response = api.getTestIsm(
            GetTestIsmPostData(
                test = test,
                sToken = sToken,
                icm = JSONObject(icm).toString()
            )
        )

        return if (response.isSuccessful) {
            val body =
                response.body()?.string()
                    ?: throw IllegalStateException("Body should not be null")
            TestIsmResult.Success(body)
        } else {
            val errorBody = response.errorBody() ?: throw HttpException(response)
            withContext(Dispatchers.IO) {
                val responseError =
                    errorResponseConverter.convert(errorBody) ?: throw HttpException(response)
                TestIsmResult.Error(response.code(), responseError)
            }
        }
    }

    override suspend fun remoteNonce(): RemoteNonce {
        return api.getNonce()
    }

    override suspend fun getCredentials(
        stoken: String,
        events: List<String>,
        issueCommitmentMessage: String
    ): RemoteCredentials {
        return api.getCredentials(
            data = GetCredentialsPostData(
                stoken = stoken,
                events = events,
                issueCommitmentMessage = Base64.encodeToString(
                    issueCommitmentMessage.toByteArray(),
                    Base64.NO_WRAP
                )
            )
        )
    }

    override suspend fun getPrepareIssue(): RemotePrepareIssue {
        return api.getPrepareIssue()
    }
}
