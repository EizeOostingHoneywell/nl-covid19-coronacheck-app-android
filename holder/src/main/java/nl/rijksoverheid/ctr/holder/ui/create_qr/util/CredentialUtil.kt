package nl.rijksoverheid.ctr.holder.ui.create_qr.util

import nl.rijksoverheid.ctr.holder.persistence.database.entities.CredentialEntity
import nl.rijksoverheid.ctr.shared.MobileCoreWrapper
import nl.rijksoverheid.ctr.shared.ext.getStringOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

interface CredentialUtil {
    fun getActiveCredential(entities: List<CredentialEntity>): CredentialEntity?
    fun isExpiring(credentialRenewalDays: Long, credential: CredentialEntity): Boolean
    fun getTestTypeForEuropeanCredentials(entities: List<CredentialEntity>): String
    fun getVaccinationDosesForEuropeanCredentials(
        entities: List<CredentialEntity>,
        getString: (String, String) -> String
    ): String

    fun vaccinationShouldBeHidden(readEuropeanCredential: JSONObject): Boolean
}

class CredentialUtilImpl(
    private val clock: Clock,
    private val mobileCoreWrapper: MobileCoreWrapper
) : CredentialUtil {

    private companion object {
        const val VACCINATION_HIDDEN_AFTER_DAYS = 25L
    }

    override fun getActiveCredential(entities: List<CredentialEntity>): CredentialEntity? {

        // All credentials that fall into the expiration window
        val credentialsInWindow = entities.filter {
            it.validFrom.isBefore(
                OffsetDateTime.now(clock)
            ) && it.expirationTime.isAfter(
                OffsetDateTime.now(
                    clock
                )
            )
        }

        // Return the credential with the longest expiration time if it exists
        return credentialsInWindow.maxByOrNull {
            it.expirationTime.toEpochSecond() - OffsetDateTime.now(clock)
                .toEpochSecond()
        }
    }

    override fun isExpiring(credentialRenewalDays: Long, credential: CredentialEntity): Boolean {
        val now = OffsetDateTime.now(clock)
        return credential.expirationTime.minusDays(credentialRenewalDays).isBefore(now)
    }

    override fun getTestTypeForEuropeanCredentials(entities: List<CredentialEntity>): String {
        val data = mobileCoreWrapper.readEuropeanCredential(entities.first().data)

        return try {
            val type = ((((data["dcc"] as JSONObject)["t"] as JSONArray)[0]) as JSONObject)["tt"]
            when (type) {
                "LP6464-4" -> "NAAT"
                "LP217198-3" -> "RAT"
                else -> ""
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            ""
        }
    }

    override fun getVaccinationDosesForEuropeanCredentials(
        entities: List<CredentialEntity>,
        getString: (String, String) -> String
    ): String {
        return try {
            val data = mobileCoreWrapper.readEuropeanCredential(entities.first().data)
            val vaccinationData = (((data["dcc"] as JSONObject)["v"] as JSONArray)[0]) as JSONObject
            val dn = vaccinationData["dn"] as Int
            val sd = vaccinationData["sd"] as Int
            getString("$dn", "$sd")
        } catch (exception: Exception) {
            exception.printStackTrace()
            ""
        }
    }

    override fun vaccinationShouldBeHidden(readEuropeanCredential: JSONObject): Boolean {
        val dcc = readEuropeanCredential.optJSONObject("dcc")
        val vaccination = dcc?.getJSONArray("v")?.optJSONObject(0)
        val dose = vaccination?.getStringOrNull("dn") ?: ""
        val totalDoses = vaccination?.getStringOrNull("sd") ?: ""
        val date = LocalDate.parse(vaccination?.getStringOrNull("dt"))
            ?.atStartOfDay()
            ?.atOffset(ZoneOffset.UTC)
        return date?.let {
            it.plusDays(VACCINATION_HIDDEN_AFTER_DAYS) < OffsetDateTime.now(clock)
                    && dose < totalDoses
        } ?: false
    }
}