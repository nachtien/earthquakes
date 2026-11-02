package com.achtien.networking

import kotlin.time.Instant

/** One real event (USGS nc75460037), for Compose previews. */
val singleFeature =
    EarthquakeFeature(
        id = "nc75460037",
        properties =
            EarthquakeProperties(
                mag = 0.23,
                magType = "md",
                place = "8 km WNW of Cobb, CA",
                time = Instant.fromEpochMilliseconds(1793393066960),
            ),
        geometry = PointGeometry(listOf(-122.807334899902, 38.8381652832031, 2.10999989509583)),
    )
