package com.achtien.codingtemplate.map

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.achtien.codingtemplate.R
import com.achtien.codingtemplate.ui.components.MagnitudeBadge
import com.achtien.codingtemplate.ui.components.OverlayCard
import com.achtien.codingtemplate.ui.theme.DividerColor
import com.achtien.codingtemplate.ui.theme.SecondaryText
import com.achtien.codingtemplate.utils.distanceText
import com.achtien.codingtemplate.utils.formatDistance
import com.achtien.codingtemplate.utils.formatEventTime
import com.achtien.codingtemplate.utils.magnitudeDescription
import com.achtien.codingtemplate.utils.magnitudeTypeName
import com.achtien.codingtemplate.utils.placeTitle
import com.achtien.networking.EarthquakeFeature
import com.achtien.networking.singleFeature
import com.achtien.repository.location.UserLocation

@Composable
internal fun EarthquakeDetails(
    earthquake: EarthquakeFeature,
    userLocation: UserLocation,
    onBack: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
) {
    val properties = earthquake.properties

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Row(Modifier.offset(x = (-12).dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.TwoTone.ArrowBack, contentDescription = "Back")
            }
            Column {
                Text(
                    text = placeTitle(properties.place),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = properties.place ?: "",
                    color = SecondaryText,
                    fontSize = 15.sp,
                )
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 20.dp), color = DividerColor)

        Row(verticalAlignment = Alignment.CenterVertically) {
            MagnitudeBadge(properties.mag)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = magnitudeTypeName(properties.magType),
                    fontSize = 18.sp,
                )
                Text(
                    text = magnitudeDescription(properties.mag),
                    color = SecondaryText,
                    fontSize = 15.sp,
                )
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 20.dp), color = DividerColor)

        val distance = userLocation.distanceText(earthquake.geometry)
        val onDistanceClick: (() -> Unit)? = when (userLocation) {
            UserLocation.PermissionDenied -> onOpenAppSettings
            UserLocation.ServicesOff -> onOpenLocationSettings
            else -> null
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row {
                InfoCell(
                    icon = R.drawable.ic_schedule,
                    value = formatEventTime(properties.time),
                    label = "Date & Time",
                    modifier = Modifier.weight(1f),
                )
                InfoCell(
                    icon = R.drawable.ic_near_me,
                    value = distance,
                    label = if (onDistanceClick != null) "Tap to enable location" else "Distance Away",
                    modifier = Modifier.weight(1f),
                    onClick = onDistanceClick,
                )
            }
            Row {
                InfoCell(
                    icon = R.drawable.ic_globe,
                    value = "%.2f°, %.2f°".format(
                        earthquake.geometry.latitude,
                        earthquake.geometry.longitude,
                    ),
                    label = "Location",
                    modifier = Modifier.weight(1f),
                )
                InfoCell(
                    icon = R.drawable.ic_arrow_downward,
                    value = formatDistance(earthquake.geometry.depthKm),
                    label = "Depth",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun InfoCell(
    @DrawableRes icon: Int,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = value, fontSize = 16.sp)
            Text(text = label, color = SecondaryText, fontSize = 13.sp)
        }
    }
}

@Preview
@Composable
private fun PreviewEarthquakeDetails() {
    OverlayCard { EarthquakeDetails(singleFeature, UserLocation.Known(37.77, -122.42), {}, {}) {} }
}
