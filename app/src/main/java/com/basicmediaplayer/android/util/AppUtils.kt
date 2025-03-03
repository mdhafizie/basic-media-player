package com.basicmediaplayer.android.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

object AppUtils {

    /**
     * Check if the device is connected to the internet.
     */
    private fun hasInternetConnection(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } else {
            val activeNetwork = connectivityManager.activeNetworkInfo
            activeNetwork != null && activeNetwork.isConnected
        }
    }

    /**
     * Show a toast message if no internet connection is available.
     */
    fun checkInternetAndShowToast(context: Context): Boolean {
        return if (!hasInternetConnection(context)) {
            showToast(context, "Please check your internet connection")
            false
        } else {
            true
        }
    }

    /**
     * Validate if a given URL is correctly formatted and points to a valid video file.
     */
    fun isValidUrl(url: String): Boolean {
        val uri = Uri.parse(url)

        // Ensure it starts with "http" or "https"
        val isValidScheme = uri.scheme in listOf("http", "https")

        // Ensure the file has a valid video extension (MP4, MKV, etc.)
        val isValidExtension = url.matches(Regex(".*\\.(mp4|mkv|avi|mov|flv|wmv|webm|3gp)$", RegexOption.IGNORE_CASE))

        return isValidScheme && isValidExtension
    }

    /**
     * Checks if the given URL is reachable.
     * @param url The video URL to check.
     * @param callback Callback function returning `true` if reachable, otherwise `false`.
     */
    fun checkUrlReachable(url: String, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                val isReachable = connection.responseCode in 200..299
                connection.disconnect()
                callback(isReachable)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    /**
     * Show a toast message in any activity or fragment.
     */
    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
