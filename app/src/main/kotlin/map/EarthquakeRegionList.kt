package com.achtien.codingtemplate.map

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.achtien.codingtemplate.ui.components.LoadingView
import com.achtien.codingtemplate.ui.components.MagnitudeBadge
import com.achtien.codingtemplate.ui.components.MessageView
import com.achtien.codingtemplate.ui.theme.DividerColor
import com.achtien.codingtemplate.ui.theme.SecondaryText
import com.achtien.codingtemplate.utils.distanceText
import com.achtien.codingtemplate.utils.placeTitle
import com.achtien.networking.EarthquakeFeature
import com.achtien.repository.location.UserLocation
import kotlin.time.Instant

@Composable
internal fun ColumnScope.EarthquakeRegionList(
    state: EarthquakeMapState,
    userLocation: UserLocation,
    listState: LazyListState,
    onRefresh: () -> Unit,
    onSelect: (EarthquakeFeature) -> Unit,
) {
    val loaded = state as? EarthquakeMapState.Loaded

    Text(
        text = loaded?.results?.headline() ?: "Earthquakes in view",
        fontWeight = FontWeight.SemiBold,
        // One line: shrinks from 18sp only as far as the headline needs on a narrow screen.
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 18.sp),
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 12.dp),
    )
    if (loaded?.isRefreshing == true) {
        LinearProgressIndicator(Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp))
    }
    val fetchedAt = loaded?.results?.fetchedAt
    if (loaded?.refreshError != null && fetchedAt != null) {
        val fetched = relativeTime(fetchedAt)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            Text(
                text = "Couldn't refresh. Showing earthquakes from $fetched.",
                color = SecondaryText,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRefresh) { Text("Retry") }
        }
    }
    HorizontalDivider(color = DividerColor)

    val fill = Modifier.weight(1f).fillMaxWidth()
    when (state) {
        EarthquakeMapState.Loading -> LoadingView(fill)

        is EarthquakeMapState.Failed -> MessageView(state.message, fill, onRetry = onRefresh)

        is EarthquakeMapState.Loaded ->
            if (state.results.earthquakes.isEmpty()) {
                MessageView("No earthquakes here. Zoom out or pan to explore.", fill)
            } else {
                LazyColumn(Modifier.weight(1f), state = listState) {
                    itemsIndexed(state.results.earthquakes, key = { _, quake -> quake.id }) { index, quake ->
                        if (index > 0) HorizontalDivider(color = DividerColor)
                        EarthquakeRow(quake, userLocation, onSelect)
                    }
                }
            }
    }
}

// DateUtils says "0 minutes ago" for anything under a minute.
private fun relativeTime(instant: Instant): CharSequence {
    val millis = instant.toEpochMilliseconds()
    val now = System.currentTimeMillis()
    return if (now - millis < DateUtils.MINUTE_IN_MILLIS) {
        "just now"
    } else {
        DateUtils.getRelativeTimeSpanString(millis, now, DateUtils.MINUTE_IN_MILLIS)
    }
}

@Composable
private fun EarthquakeRow(
    quake: EarthquakeFeature,
    userLocation: UserLocation,
    onSelect: (EarthquakeFeature) -> Unit,
) {
    val properties = quake.properties
    val subtitle =
        listOfNotNull(
            relativeTime(properties.time),
            (userLocation as? UserLocation.Known)?.distanceText(quake.geometry)?.plus(" away"),
        ).joinToString(" · ")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(quake) }
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        MagnitudeBadge(properties.mag, size = 40.dp, fontSize = 14.sp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = placeTitle(properties.place),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = subtitle, color = SecondaryText, fontSize = 13.sp, maxLines = 1)
        }
    }
}
