package nl.rijksoverheid.ctr.shared

import nl.rijksoverheid.ctr.shared.usecase.TestResultAttributesUseCase
import nl.rijksoverheid.ctr.shared.usecase.TestResultAttributesUseCaseImpl
import nl.rijksoverheid.ctr.shared.util.MLKitQrCodeScannerUtil
import nl.rijksoverheid.ctr.shared.util.QrCodeScannerUtil
import nl.rijksoverheid.ctr.shared.util.QrCodeUtil
import nl.rijksoverheid.ctr.shared.util.TestResultUtil
import org.koin.dsl.module
import java.time.Clock

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

val sharedModule = module {

    single { Clock.systemDefaultZone() }

    // Usecases
    factory<TestResultAttributesUseCase> {
        TestResultAttributesUseCaseImpl(get())
    }

    single<QrCodeScannerUtil> { MLKitQrCodeScannerUtil() }

    // Utils
    single { QrCodeUtil(get()) }
    single { TestResultUtil(get()) }
}
