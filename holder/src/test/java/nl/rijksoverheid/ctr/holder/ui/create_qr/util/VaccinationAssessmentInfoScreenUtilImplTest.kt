/*
 * Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 * Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.rijksoverheid.ctr.holder.ui.create_qr.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import nl.rijksoverheid.ctr.holder.ui.create_qr.models.RemoteEventVaccinationAssessment
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.AutoCloseKoinTest
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "nl-land")
class VaccinationAssessmentInfoScreenUtilImplTest: AutoCloseKoinTest() {

    @Test
    fun `check correct copy when getting info screen()`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val event = RemoteEventVaccinationAssessment(
            type = "",
            unique = "123",
            vaccinationAssessment = RemoteEventVaccinationAssessment.VaccinationAssessment(
                assessmentDate = OffsetDateTime.ofInstant(Instant.parse("2021-01-01T00:00:00.00Z"), ZoneId.of("UTC")),
                digitallyVerified = true,
                country = "NLD"
            )
        )

        val util = VaccinationAssessmentInfoScreenUtilImpl(context)
        val infoScreen = util.getForVaccinationAssessment(
            event = event,
            fullName = "Bob de Bouwer",
            birthDate = "1 jan 1970"
        )

        assertEquals("Details", infoScreen.title)
        assertEquals("De volgende gegevens zijn opgehaald:<br/><br/>Naam: <b>Bob de Bouwer</b><br/>Geboortedatum: <b>1 jan 1970</b><br/><br/>Beoordelingdatum: <b>vrijdag 1 januari 01:00</b><br/><br/>Uniek beoordelingscode: <b>123</b><br/>", infoScreen.description)
    }
}