package com.achtien.networking

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant

@Serializable
data class EarthquakeFeed(
    val features: List<EarthquakeFeature>,
)

@Serializable
data class EarthquakeFeature(
    val id: String,
    val properties: EarthquakeProperties,
    val geometry: PointGeometry,
)

@Serializable
data class EarthquakeProperties(
    val mag: Double? = null,
    val magType: String? = null,
    val place: String? = null,
    @Serializable(with = EpochMillisSerializer::class)
    val time: Instant,
)

@Serializable
data class PointGeometry(
    val coordinates: List<Double>,
) {
    val longitude: Double get() = coordinates[0]
    val latitude: Double get() = coordinates[1]
    val depthKm: Double get() = coordinates[2]
}

// We do this because we get a long, and Instant doesn't have a built-in serializer for
// epoch milliseconds like it does for a string.
private object EpochMillisSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("EpochMillis", PrimitiveKind.LONG)

    override fun serialize(
        encoder: Encoder,
        value: Instant,
    ) = encoder.encodeLong(value.toEpochMilliseconds())

    override fun deserialize(decoder: Decoder) = Instant.fromEpochMilliseconds(decoder.decodeLong())
}
