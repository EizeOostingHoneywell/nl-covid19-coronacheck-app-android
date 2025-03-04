package nl.rijksoverheid.ctr.verifier.ui.scanqr

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import nl.rijksoverheid.ctr.appconfig.usecases.ClockDeviationUseCase
import nl.rijksoverheid.ctr.appconfig.usecases.FeatureFlagUseCase
import nl.rijksoverheid.ctr.design.fragments.info.DescriptionData
import nl.rijksoverheid.ctr.design.fragments.info.InfoFragmentData
import nl.rijksoverheid.ctr.design.utils.InfoFragmentUtil
import nl.rijksoverheid.ctr.shared.ext.findNavControllerSafety
import nl.rijksoverheid.ctr.shared.ext.navigateSafety
import nl.rijksoverheid.ctr.shared.livedata.EventObserver
import nl.rijksoverheid.ctr.shared.models.VerificationPolicy.*
import nl.rijksoverheid.ctr.shared.utils.AndroidUtil
import nl.rijksoverheid.ctr.verifier.DeeplinkManager
import nl.rijksoverheid.ctr.verifier.R
import nl.rijksoverheid.ctr.verifier.databinding.FragmentScanQrBinding
import nl.rijksoverheid.ctr.verifier.models.ScannerState
import nl.rijksoverheid.ctr.verifier.persistance.usecase.VerifierCachedAppConfigUseCase
import nl.rijksoverheid.ctr.verifier.ui.policy.*
import nl.rijksoverheid.ctr.verifier.ui.scanner.utils.ScannerUtil
import nl.rijksoverheid.ctr.verifier.ui.scanqr.util.ScannerStateCountdownUtil
import nl.rijksoverheid.ctr.verifier.usecase.ScannerStateUseCase
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.TimeUnit

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
class ScanQrFragment : Fragment(R.layout.fragment_scan_qr) {

    private var _binding: FragmentScanQrBinding? = null
    private val binding get() = _binding!!

    private val scanQrViewModel: ScanQrViewModel by viewModel()
    private val scannerUtil: ScannerUtil by inject()
    private val clockDeviationUseCase: ClockDeviationUseCase by inject()
    private val infoFragmentUtil: InfoFragmentUtil by inject()
    private val scannerStateCountdownUtil: ScannerStateCountdownUtil by inject()
    private val verifierCachedAppConfigUseCase: VerifierCachedAppConfigUseCase by inject()
    private val scannerStateUseCase: ScannerStateUseCase by inject()
    private val androidUtil: AndroidUtil by inject()
    private val featureFlagUseCase: FeatureFlagUseCase by inject()
    private val deeplinkManager: DeeplinkManager by inject()

    private var scannerStateCountDownTimer: ScannerStateCountDownTimer? = null

    private fun onTimerFinish() {
        onStateUpdated(
            scannerStateUseCase.get()
        )
    }

    private fun startTimer() {
        stopTimer()
        val lockTimer = ScannerStateCountDownTimer(scannerStateCountdownUtil, ::updateTitle, ::onTimerFinish)
        lockTimer.start()
        scannerStateCountDownTimer = lockTimer
    }

    private fun stopTimer() {
        scannerStateCountDownTimer?.cancel()
        scannerStateCountDownTimer = null
    }

    override fun onStart() {
        super.onStart()
        scanQrViewModel.checkPolicyUpdate()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentScanQrBinding.bind(view)
        binding.instructionsButton.setOnClickListener {
            navigateSafety(R.id.nav_scan_qr, ScanQrFragmentDirections.actionScanInstructions())
        }

        binding.bottom.setButtonClick {
            scanQrViewModel.nextScreen()
        }

        scanQrViewModel.scannerNavigationStateEvent.observe(viewLifecycleOwner, EventObserver {
            goToNextScreen(it)
        })

        scanQrViewModel.scannerStateLiveData.observe(viewLifecycleOwner, EventObserver {
            onStateUpdated(it)
        })

        setupClockDeviation()

        if (deeplinkManager.getReturnUri() != null) {
            scanQrViewModel.nextScreen()
        }
    }

    private fun setupClockDeviation() {
        binding.clockdeviationView.clockdeviationButton.setOnClickListener {
            infoFragmentUtil.presentAsBottomSheet(
                childFragmentManager, InfoFragmentData.TitleDescription(
                    title = getString(R.string.clockdeviation_page_title),
                    descriptionData = DescriptionData(
                        R.string.clockdeviation_page_message,
                        customLinkIntent = Intent(Settings.ACTION_DATE_SETTINGS)
                    ),
                )
            )
        }
        // Handle clock deviation view
        observeServerTimeSynced()
        showDeviationViewIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        startTimer()
    }

    override fun onPause() {
        super.onPause()
        stopTimer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onStateUpdated(scannerState: ScannerState) {
        val imageDrawable = when (scannerState.verificationPolicyState) {
            VerificationPolicyState.None -> {
                binding.bottom.hidePolicyIndication()
                R.drawable.illustration_scanner_get_started_3g
            }
            VerificationPolicyState.Policy2G -> {
                binding.bottom.setPolicy(VerificationPolicy2G)
                R.drawable.illustration_scanner_get_started_2g
            }
            VerificationPolicyState.Policy3G -> {
                binding.bottom.setPolicy(VerificationPolicy3G)
                R.drawable.illustration_scanner_get_started_3g
            }
            VerificationPolicyState.Policy2GPlus -> {
                binding.bottom.setPolicy(VerificationPolicy2GPlus)
                R.drawable.illustration_scanner_get_started_2g_plus
            }
        }

        // hide the image on landscape
        if (!androidUtil.isSmallScreen()) {
            binding.image.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(), imageDrawable
                )
            )
        }

        when (scannerState) {
            is ScannerState.Locked -> lockScanner()
            is ScannerState.Unlocked -> unlockScanner()
        }
    }

    private fun deeplinkReturnUri(): String? {
        return deeplinkManager.getReturnUri().apply {
            deeplinkManager.removeReturnUri()
        }
    }

    private fun goToNextScreen(scannerNavigationState: ScannerNavigationState) {
        when (scannerNavigationState) {
            is ScannerNavigationState.Instructions -> findNavController().navigate(ScanQrFragmentDirections.actionScanInstructions(
                returnUri = deeplinkReturnUri()
            ))
            is ScannerNavigationState.VerificationPolicySelection -> findNavControllerSafety()?.navigate(
                ScanQrFragmentDirections.actionPolicySelection(
                    selectionType = VerificationPolicySelectionType.FirstTimeUse(scannerStateUseCase.get()),
                    toolbarTitle = getString(R.string.verifier_menu_risksetting),
                    returnUri = deeplinkReturnUri(),
                )
            )
            is ScannerNavigationState.Scanner -> scannerUtil.launchScanner(requireActivity(), deeplinkReturnUri())
        }
    }

    /**
     * Whenever the server time is synced we want to check
     * if we want to inform the user that the clock is not correct
     */
    private fun observeServerTimeSynced() {
        clockDeviationUseCase.serverTimeSyncedLiveData.observe(viewLifecycleOwner, EventObserver {
            showDeviationViewIfNeeded()
        })
    }

    private fun showDeviationViewIfNeeded() {
        binding.clockdeviationView.root.isGone = !clockDeviationUseCase.hasDeviation()
    }

    private fun lockScanner() {
        binding.instructionsButton.visibility = GONE
        binding.clockdeviationView.root.visibility = GONE
        binding.description.text = getString(R.string.verifier_home_countdown_subtitle,
            TimeUnit.SECONDS.toMinutes(verifierCachedAppConfigUseCase.getCachedAppConfig().scanLockSeconds.toLong()))
        binding.bottom.lock()
    }

    private fun unlockScanner() {
        binding.title.setText(R.string.scan_qr_header)
        binding.description.setText(if (featureFlagUseCase.isVerificationPolicyEnabled()) R.string.scan_qr_description_2G else R.string.scan_qr_description)
        binding.instructionsButton.visibility = VISIBLE
        showDeviationViewIfNeeded()
        binding.bottom.unlock()
    }

    private fun updateTitle(timeLeft: String) {
        binding.title.text = getString(R.string.verifier_home_countdown_title, timeLeft)
    }
}
