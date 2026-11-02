package com.achtien.codingtemplate.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import com.achtien.networking.PointGeometry
import com.achtien.repository.location.UserLocation
import java.util.Locale
import kotlin.time.Instant

private const val MILES_PER_KM = 0.621371

// e.g. "10/30, 7:23:51 PM"
private val EVENT_TIME =
    LocalDateTime.Format {
        monthNumber(Padding.NONE)
        char('/')
        day(Padding.NONE)
        chars(", ")
        amPmHour(Padding.NONE)
        char(':')
        minute()
        char(':')
        second()
        char(' ')
        amPmMarker("AM", "PM")
    }

fun formatDistance(km: Double): String = "%.1f miles".format(Locale.US, km * MILES_PER_KM)

/** How far [geometry] is from the user, or why that isn't known. */
fun UserLocation.distanceText(geometry: PointGeometry): String =
    when (this) {
        is UserLocation.Known -> formatDistance(distanceKmTo(geometry))
        UserLocation.Unknown -> "Locating…"
        UserLocation.Unavailable -> "Unavailable"
        UserLocation.PermissionDenied, UserLocation.ServicesOff -> "Location off"
    }

fun formatEventTime(
    time: Instant,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): String = time.toLocalDateTime(zone).format(EVENT_TIME)

fun placeTitle(place: String?): String =
    place?.substringAfter(" of ", missingDelimiterValue = place) ?: "Unknown location"

fun magnitudeTypeName(magType: String?): String =
    when (magType?.lowercase()) {
        "md" -> "Duration Magnitude"
        "ml" -> "Local Magnitude"
        "ms", "ms_20" -> "Surface-wave Magnitude"
        "mb" -> "Body-wave Magnitude"
        "mb_lg", "mlg" -> "Lg-wave Magnitude"
        "mw", "mww", "mwc", "mwb", "mwr" -> "Moment Magnitude"
        "mh" -> "Hand-picked Magnitude"
        "me" -> "Energy Magnitude"
        else -> "Magnitude"
    }

fun magnitudeDescription(mag: Double?): String =
    when {
        mag == null -> "Magnitude not yet computed."
        mag < 1.0 -> "Not felt."
        mag < 3.0 -> "Felt only by sensitive people."
        mag < 4.0 -> "Felt by many people; rarely causes damage."
        mag < 5.0 -> "Noticeable shaking indoors; minor damage possible."
        mag < 6.0 -> "Can damage poorly built structures."
        mag < 7.0 -> "Strong; can be destructive in populated areas."
        mag < 8.0 -> "Major; serious damage over large areas."
        else -> "Great; can devastate areas near the epicenter."
    }
