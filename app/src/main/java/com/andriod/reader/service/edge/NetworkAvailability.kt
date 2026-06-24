package com.andriod.reader.service.edge

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkAvailability {
    fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return true
        }
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
