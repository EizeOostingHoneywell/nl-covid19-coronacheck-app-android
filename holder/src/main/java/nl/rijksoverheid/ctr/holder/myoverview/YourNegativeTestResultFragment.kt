package nl.rijksoverheid.ctr.holder.myoverview

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import nl.rijksoverheid.ctr.holder.BaseFragment
import nl.rijksoverheid.ctr.holder.R
import nl.rijksoverheid.ctr.holder.databinding.FragmentYourNegativeTestResultsBinding
import nl.rijksoverheid.ctr.holder.usecase.SignedTestResult
import nl.rijksoverheid.ctr.shared.livedata.EventObserver
import org.koin.androidx.viewmodel.ViewModelOwner
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.scope.emptyState
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
class YourNegativeTestResultFragment : BaseFragment(R.layout.fragment_your_negative_test_results) {

    //TODO depending on the graph and reuse we probably need to know if this is GGD or commercial
    private val viewModel: TestResultsViewModel by sharedViewModel(
        state = emptyState(),
        owner = {
            ViewModelOwner.from(
                findNavController().getViewModelStoreOwner(R.id.nav_commercial_test),
                this
            )
        })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentYourNegativeTestResultsBinding.bind(view)

        val result = viewModel.retrievedResult?.remoteTestResult?.result
        if (result == null) {
            // restored from state, no result anymore
            findNavController().navigate(YourNegativeTestResultFragmentDirections.actionHome())
        } else {
            binding.rowSubtitle.text =
                result.sampleDate.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
        }

        binding.button.setOnClickListener {
            viewModel.saveTestResult()
        }

        viewModel.loading.observe(viewLifecycleOwner, EventObserver {
            presentLoading(it)
        })

        viewModel.signedTestResult.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                is SignedTestResult.Complete -> {
                    findNavController().navigate(YourNegativeTestResultFragmentDirections.actionCreateQr())
                }
                is SignedTestResult.AlreadySigned -> {
                    findNavController().navigate(
                        CommercialTestCodeFragmentDirections.actionNoTestResult(
                            title = getString(R.string.test_result_already_signed_title),
                            description = getString(R.string.test_result_already_signed_description)
                        )
                    )
                }
                is SignedTestResult.ServerError -> {
                    presentError()
                }
            }
        })
    }

}
