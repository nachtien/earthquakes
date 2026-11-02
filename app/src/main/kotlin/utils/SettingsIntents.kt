package com.achtien.codingtemplate.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

fun Context.openAppSettings() =
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))

fun Context.openLocationSettings() = startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
