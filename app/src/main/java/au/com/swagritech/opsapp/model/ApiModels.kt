package au.com.swagritech.opsapp.model

data class AuthIdentityResponse(
    val authenticated: Boolean? = null,
    val principalName: String? = null,
    val principalId: String? = null,
    val isChiefPilot: Boolean? = null,
    val isAdmin: Boolean? = null,
    val canEditBatterySerial: Boolean? = null,
    val roles: List<String>? = null,
    val status: String? = null,
    val error: String? = null
)

data class StartJobRequest(
    val ClientName: String,
    val LocationProperty: String,
    val SiteBlockId: String,
    val AircraftType: String,
    val OperationType: String,
    val Pilot: String,
    val ExpectedDate: String?,
    val Notes: String?
)

data class StartJobResponse(
    val status: String? = null,
    val JobId: String? = null,
    val DisplayJobId: String? = null,
    val error: String? = null
)

data class ActiveJobResponse(
    val status: String? = null,
    val JobId: String? = null,
    val DisplayJobId: String? = null,
    val Pilot: String? = null,
    val ClientName: String? = null,
    val LocationProperty: String? = null,
    val SiteBlockId: String? = null,
    val AircraftType: String? = null,
    val OperationType: String? = null,
    val JobStatus: String? = null,
    val error: String? = null
)
