package nl.rijksoverheid.ctr.verifier.ui.scanqr

import android.os.Bundle
import androidx.lifecycle.ViewModel
import nl.rijksoverheid.ctr.verifier.persistance.PersistenceManager

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

abstract class ScanQrViewModel : ViewModel() {
    abstract fun hasSeenScanInstructions() : Boolean
    abstract fun setScanInstructionsSeen()
    abstract fun getNextScannerScreenState(): NextScannerScreenState
}

class ScanQrViewModelImpl(
    private val persistenceManager: PersistenceManager
) : ScanQrViewModel() {
    override fun hasSeenScanInstructions(): Boolean {
        return persistenceManager.getScanInstructionsSeen()
    }

    override fun setScanInstructionsSeen() {
        if (!hasSeenScanInstructions()) {
            persistenceManager.setScanInstructionsSeen()
        }
    }

    override fun getNextScannerScreenState(): NextScannerScreenState {
        return if (!hasSeenScanInstructions()) {
            NextScannerScreenState.Instructions
        } else if (!persistenceManager.isRiskModeSelectionSet()) {
            NextScannerScreenState.RiskModeSelection
        } else {
            NextScannerScreenState.Scanner
        }
    }
}
