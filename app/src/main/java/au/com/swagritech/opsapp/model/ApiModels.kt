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

data class CreateFlightRequest(
    val JobId: String?,
    val Pilot: String,
    val AircraftIdentifier: String,
    val AircraftType: String,
    val LocationProperty: String,
    val SiteBlockId: String?,
    val OperationType: String,
    val VlosBvlos: String = "VLOS",
    val BatteryId: String,
    val TakeoffTimeUtc: String,
    val LandingTimeUtc: String,
    val FlightMinutes: Int,
    val FlightDateTimeUtc: String,
    val Notes: String?,
    val OfflineClientId: String,
    val BatteryCounted: Boolean = true,
    val AircraftCounted: Boolean = true,
    val BatteryCyclesAtFlight: Int? = null,
    val AircraftMinutesAtFlight: Double? = null,
    val IsCorrection: Boolean = false,
    val CorrectionNotes: String? = null,
    val Voided: Boolean = false,
    val VoidReason: String? = null,
    val Latitude: Double? = null,
    val Longitude: Double? = null
)

data class CreateFlightResponse(
    val status: String? = null,
    val error: String? = null,
    val message: String? = null
)

data class QueuedFlightItem(
    val localQueueId: String,
    val payload: CreateFlightRequest,
    val queuedAtUtc: String
)
