package com.example.ytshare.helpers

import android.content.SharedPreferences
import android.net.Uri
import com.example.ytshare.Constants

object SharedPrefHelper {
    public fun saveLink(text:String, sharedPref: SharedPreferences){
        val link = Uri.parse(text)
        if (link.host.toString().contains("youtube.com") || link.host.toString().contains("youtu.be")) {
            val editor = sharedPref.edit()
            editor.putString(Constants.link, text).apply()
        }
    }

    public fun clearLink(sharedPref: SharedPreferences) {
        val editor = sharedPref.edit()
        editor.remove(Constants.link).apply()
    }

    public fun saveIp(text:String, sharedPref: SharedPreferences){
        val editor = sharedPref.edit()
        editor.putString(Constants.ip, text)
        editor.apply()
    }

    public fun clearIp(sharedPref: SharedPreferences) {
        val editor = sharedPref.edit()
        editor.remove(Constants.ip).apply()
    }

    public fun savePref(isTracking:Boolean, sharedPref: SharedPreferences){
        val editor = sharedPref.edit()
        editor.putBoolean(Constants.isTracking, isTracking).apply()
    }

    public fun clearPref(sharedPref: SharedPreferences) {
        val editor = sharedPref.edit()
        editor.remove(Constants.isTracking).apply()
    }
}