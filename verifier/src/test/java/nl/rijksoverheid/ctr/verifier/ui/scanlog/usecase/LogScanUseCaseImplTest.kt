package nl.rijksoverheid.ctr.verifier.ui.scanlog.usecase

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.ctr.shared.models.VerificationPolicy
import nl.rijksoverheid.ctr.verifier.persistance.database.VerifierDatabase
import nl.rijksoverheid.ctr.verifier.persistance.database.entities.ScanLogEntity
import nl.rijksoverheid.ctr.verifier.ui.policy.VerificationPolicyUseCase
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

class LogScanUseCaseImplTest {

    @Test
    fun `logs a single scan in the database`() = runBlocking {
        val clock = Clock.fixed(Instant.parse("2021-01-01T00:00:00.00Z"),
            ZoneId.of("UTC"))

        val verificationPolicyUseCase = mockk<VerificationPolicyUseCase>(relaxed = true)
        every { verificationPolicyUseCase.get() } answers { VerificationPolicy.VerificationPolicy3G }

        val verifierDatabase = mockk<VerifierDatabase>(relaxed = true)

        val usecase = LogScanUseCaseImpl(
            clock = clock,
            verificationPolicyUseCase = verificationPolicyUseCase,
            verifierDatabase = verifierDatabase
        )

        usecase.log()

        coVerify { verifierDatabase.scanLogDao().insert(
            ScanLogEntity(
                policy = VerificationPolicy.VerificationPolicy3G,
                date = OffsetDateTime.ofInstant(
                    Instant.parse("2021-01-01T00:00:00.00Z"),
                    ZoneId.of("UTC")
                )
            )
        ) }
    }
}