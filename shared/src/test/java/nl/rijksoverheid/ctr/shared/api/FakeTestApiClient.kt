/*
  *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
  *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
  *  
  *   SPDX-License-Identifier: EUPL-1.2
  *
  */

package nl.rijksoverheid.ctr.shared.api

import nl.rijksoverheid.ctr.shared.models.*
import okhttp3.ResponseBody
import retrofit2.Response

abstract class FakeTestApiClient : TestApiClient {

    override suspend fun getAgent(id: String): RemoteAgent {
        TODO("Not yet implemented")
    }

    override suspend fun getEvent(id: String): RemoteEvent {
        TODO("Not yet implemented")
    }

    override suspend fun getHolderConfig(): Config {
        TODO("Not yet implemented")
    }

    override suspend fun getIssuers(): Issuers {
        TODO("Not yet implemented")
    }

    override suspend fun getNonce(): nl.rijksoverheid.ctr.holder.models.RemoteNonce {
        TODO("Not yet implemented")
    }

    override suspend fun getTestIsm(
        accessToken: String,
        sToken: String,
        icm: String
    ): Response<ResponseBody> {
        TODO("Not yet implemented")
    }

    override suspend fun getVerifierConfig(): Config {
        TODO("Not yet implemented")
    }
}