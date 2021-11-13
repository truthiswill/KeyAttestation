package io.github.vvb2060.keyattestation.home

import com.google.common.base.CharMatcher
import com.google.common.io.BaseEncoding
import io.github.vvb2060.keyattestation.R
import io.github.vvb2060.keyattestation.attestation.AttestationResult
import io.github.vvb2060.keyattestation.attestation.AuthorizationList
import io.github.vvb2060.keyattestation.attestation.RootOfTrust
import io.github.vvb2060.keyattestation.lang.AttestationException
import rikka.recyclerview.IdBasedRecyclerViewAdapter

class HomeAdapter(listener: Listener) : IdBasedRecyclerViewAdapter() {

    interface Listener {

        fun onSubtitleDataClick(data: SubtitleData)

        fun onCommonDataClick(data: CommonData)

        fun onSecurityLevelDataClick(data: SecurityLevelData)

        fun onAuthorizationItemDataClick(data: AuthorizationItemData)
    }

    init {
        setHasStableIds(false)
        setListener(listener)
    }

    fun updateData(attestationResult: AttestationResult) {
        val attestation = attestationResult.attestation
        val locked = attestation.teeEnforced?.rootOfTrust?.isDeviceLocked == true
        val selfSigned =
            attestation.teeEnforced?.rootOfTrust?.verifiedBootState == RootOfTrust.KM_VERIFIED_BOOT_SELF_SIGNED
        val isGoogleRootCertificate = attestationResult.isGoogleRootCertificate

        clear()
        if (!isGoogleRootCertificate && !(locked && selfSigned)) {
            addItem(
                HeaderViewHolder.CREATOR, HeaderData(
                    R.string.not_google_cert,
                    R.string.not_google_cert_summary,
                    R.drawable.ic_error_outline_24,
                    rikka.material.R.attr.colorWarning
                ), ID_NOT_GOOGLE_CERT
            )
        }
        addItem(BootStateViewHolder.CREATOR, attestationResult, ID_BOOT_STATE)

        var id = ID_DESCRIPTION_START
        addItem(
            CommonItemViewHolder.SECURITY_LEVEL_CREATOR, SecurityLevelData(
                R.string.attestation,
                R.string.attestation_version_description,
                R.string.security_level_description,
                attestation.attestationVersion,
                attestation.attestationSecurityLevel
            ), id++
        )

        addItem(
            CommonItemViewHolder.SECURITY_LEVEL_CREATOR, SecurityLevelData(
                R.string.keymaster,
                R.string.keymaster_version_description,
                R.string.security_level_description,
                attestation.keymasterVersion,
                attestation.keymasterSecurityLevel
            ), id++
        )

        addItem(CommonItemViewHolder.COMMON_CREATOR, CommonData(
            R.string.attestation_challenge,
            R.string.attestation_challenge_description,
            attestation.attestationChallenge?.let {
                val stringChallenge = String(it)
                if (CharMatcher.ascii().matchesAllOf(stringChallenge)) stringChallenge
                else BaseEncoding.base64().encode(it) + " (base64)"
            }), id++
        )

        addItem(CommonItemViewHolder.COMMON_CREATOR, CommonData(
            R.string.unique_id,
            R.string.unique_id_description,
            attestation.uniqueId?.let { BaseEncoding.base64().encode(it) }), id
        )

        id = ID_AUTHORIZATION_LIST_START
        addItem(
            SubtitleViewHolder.CREATOR, SubtitleData(
                R.string.authorization_list,
                R.string.authorization_list_description
            ), id++
        )

        val tee = createAuthorizationItems(attestation.teeEnforced)
        val sw = createAuthorizationItems(attestation.softwareEnforced)
        for (i in tee.indices) {
            if (tee[i] == null && sw[i] == null) {
                continue
            }

            addItem(
                CommonItemViewHolder.AUTHORIZATION_ITEM_CREATOR, AuthorizationItemData(
                    authorizationItemTitles[i],
                    authorizationItemDescriptions[i],
                    tee[i],
                    sw[i]
                ), id++
            )

            if (tee[i] != null && sw[i] != null) {
                addItem(
                    CommonItemViewHolder.AUTHORIZATION_ITEM_CREATOR, AuthorizationItemData(
                        authorizationItemTitles[i],
                        authorizationItemDescriptions[i],
                        sw[i],
                        false
                    ), id++
                )
            }
        }

        notifyDataSetChanged()
    }

    fun updateData(e: AttestationException) {
        clear()
        addItem(
            HeaderViewHolder.CREATOR, HeaderData(
                e.titleResId,
                0,
                R.drawable.ic_error_outline_24,
                rikka.material.R.attr.colorInactive
            ), ID_ERROR
        )

        addItem(ErrorViewHolder.CREATOR, e, ID_ERROR_MESSAGE)
        notifyDataSetChanged()
    }

    fun allowFrameAt(position: Int): Boolean {
        if (position < 0) return false
        val id = getItemId(position)
        return id >= ID_DESCRIPTION_START
    }

    fun shouldCommitFrameAt(position: Int): Boolean {
        if (position < 0) return false
        val id = getItemId(position)
        if (position == itemCount - 1) {
            return true
        }
        return if (id < ID_DESCRIPTION_START) {
            false
        } else {
            (getItemId(position + 1) / 1000 - id / 1000) > 0
        }
    }

    companion object {

        private const val ID_ERROR = 0L
        private const val ID_BOOT_STATE = 1L
        private const val ID_NOT_GOOGLE_CERT = 2L
        private const val ID_DESCRIPTION_START = 3000L
        private const val ID_AUTHORIZATION_LIST_START = 4000L
        private const val ID_ERROR_MESSAGE = 100000L

        private fun createAuthorizationItems(list: AuthorizationList): Array<String?> {
            return arrayOf(
                list.purposes?.let { AuthorizationList.purposesToString(it) },
                list.algorithm?.let { AuthorizationList.algorithmToString(it) },
                list.keySize?.toString(),
                list.digests?.let { AuthorizationList.digestsToString(it) },
                list.paddingModes?.let { AuthorizationList.paddingModesToString(it) },
                list.ecCurve?.let { AuthorizationList.ecCurveAsString(it) },
                list.rsaPublicExponent?.toString(),
                list.rollbackResistance?.toString(),
                list.activeDateTime?.let { AuthorizationList.formatDate(it) },
                list.originationExpireDateTime?.let { AuthorizationList.formatDate(it) },
                list.usageExpireDateTime?.let { AuthorizationList.formatDate(it) },
                list.noAuthRequired?.toString(),
                list.userAuthType?.let { AuthorizationList.userAuthTypeToString(it) },
                list.authTimeout?.toString(),
                list.allowWhileOnBody?.toString(),
                list.isUserPresenceRequired?.toString(),
                list.confirmationRequired?.toString(),
                list.unlockedDeviceRequired?.toString(),
                list.allApplications?.toString(),
                if (list.applicationId != null) String(list.applicationId) else null,
                list.creationDateTime?.let { AuthorizationList.formatDate(it) },
                list.origin?.let { AuthorizationList.originToString(it) },
                list.rollbackResistant?.toString(),
                list.rootOfTrust?.toString(),
                list.osVersion?.toString(),
                list.osPatchLevel?.toString(),
                list.attestationApplicationId?.toString()?.trim(),
                list.brand,
                list.device,
                list.product,
                list.serialNumber,
                list.imei,
                list.meid,
                list.manufacturer,
                list.model,
                list.vendorPatchLevel?.toString(),
                list.bootPatchLevel?.toString()
            )
        }

        private val authorizationItemTitles = arrayOf(
            R.string.authorization_list_purpose,
            R.string.authorization_list_algorithm,
            R.string.authorization_list_keySize,
            R.string.authorization_list_digest,
            R.string.authorization_list_padding,
            R.string.authorization_list_ecCurve,
            R.string.authorization_list_rsaPublicExponent,
            R.string.authorization_list_rollbackResistance,
            R.string.authorization_list_activeDateTime,
            R.string.authorization_list_originationExpireDateTime,
            R.string.authorization_list_usageExpireDateTime,
            R.string.authorization_list_noAuthRequired,
            R.string.authorization_list_userAuthType,
            R.string.authorization_list_authTimeout,
            R.string.authorization_list_allowWhileOnBody,
            R.string.authorization_list_trustedUserPresenceRequired,
            R.string.authorization_list_trustedConfirmationRequired,
            R.string.authorization_list_unlockedDeviceRequired,
            R.string.authorization_list_allApplications,
            R.string.authorization_list_applicationId,
            R.string.authorization_list_creationDateTime,
            R.string.authorization_list_origin,
            R.string.authorization_list_rollbackResistant,
            R.string.authorization_list_rootOfTrust,
            R.string.authorization_list_osVersion,
            R.string.authorization_list_osPatchLevel,
            R.string.authorization_list_attestationApplicationId,
            R.string.authorization_list_attestationIdBrand,
            R.string.authorization_list_attestationIdDevice,
            R.string.authorization_list_attestationIdProduct,
            R.string.authorization_list_attestationIdSerial,
            R.string.authorization_list_attestationIdImei,
            R.string.authorization_list_attestationIdMeid,
            R.string.authorization_list_attestationIdManufacturer,
            R.string.authorization_list_attestationIdModel,
            R.string.authorization_list_vendorPatchLevel,
            R.string.authorization_list_bootPatchLevel
        )

        private val authorizationItemDescriptions = arrayOf(
            R.string.authorization_list_purpose_description,
            R.string.authorization_list_algorithm_description,
            R.string.authorization_list_keySize_description,
            R.string.authorization_list_digest_description,
            R.string.authorization_list_padding_description,
            R.string.authorization_list_ecCurve_description,
            R.string.authorization_list_rsaPublicExponent_description,
            R.string.authorization_list_rollbackResistance_description,
            R.string.authorization_list_activeDateTime_description,
            R.string.authorization_list_originationExpireDateTime_description,
            R.string.authorization_list_usageExpireDateTime_description,
            R.string.authorization_list_noAuthRequired_description,
            R.string.authorization_list_userAuthType_description,
            R.string.authorization_list_authTimeout_description,
            R.string.authorization_list_allowWhileOnBody_description,
            R.string.authorization_list_trustedUserPresenceRequired_description,
            R.string.authorization_list_trustedConfirmationRequired_description,
            R.string.authorization_list_unlockedDeviceRequired_description,
            R.string.authorization_list_allApplications_description,
            R.string.authorization_list_applicationId_description,
            R.string.authorization_list_creationDateTime_description,
            R.string.authorization_list_origin_description,
            R.string.authorization_list_rollbackResistant_description,
            R.string.authorization_list_rootOfTrust_description,
            R.string.authorization_list_osVersion_description,
            R.string.authorization_list_osPatchLevel_description,
            R.string.authorization_list_attestationApplicationId_description,
            R.string.authorization_list_attestationIdBrand_description,
            R.string.authorization_list_attestationIdDevice_description,
            R.string.authorization_list_attestationIdProduct_description,
            R.string.authorization_list_attestationIdSerial_description,
            R.string.authorization_list_attestationIdImei_description,
            R.string.authorization_list_attestationIdMeid_description,
            R.string.authorization_list_attestationIdManufacturer_description,
            R.string.authorization_list_attestationIdModel_description,
            R.string.authorization_list_vendorPatchLevel_description,
            R.string.authorization_list_bootPatchLevel_description
        )
    }
}
