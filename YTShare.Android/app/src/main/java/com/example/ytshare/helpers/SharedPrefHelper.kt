package com.example.ytshare.helpers

import android.content.SharedPreferences
import com.example.ytshare.Constants
import androidx.core.content.edit
import androidx.core.net.toUri

object SharedPrefHelper {
    fun saveLink(text:String, sharedPref: SharedPreferences){
        val link = text.toUri()
        if (link.host.toString().contains("youtube.com") || link.host.toString().contains("youtu.be")) {
            sharedPref.edit {
                putString(Constants.link, text)
            }
        }
    }

    fun clearLink(sharedPref: SharedPreferences) {
        sharedPref.edit {
            remove(Constants.link)
        }
    }

    fun saveIp(text:String, sharedPref: SharedPreferences){
        sharedPref.edit {
            putString(Constants.ip, text)
        }
    }

    fun clearIp(sharedPref: SharedPreferences) {
        sharedPref.edit {
            remove(Constants.ip)
        }
    }

    fun saveSort(isHistoryDesc:Boolean, sharedPref: SharedPreferences) {
        sharedPref.edit {
            putBoolean(Constants.isHistoryDesc, isHistoryDesc)
        }
    }

    fun clearSort(sharedPref: SharedPreferences) {
        sharedPref.edit {
            remove(Constants.isHistoryDesc)
        }
    }

    fun savePref(isTracking:Boolean, sharedPref: SharedPreferences){
        sharedPref.edit {
            putBoolean(Constants.isTracking, isTracking)
        }
    }

    fun clearPref(sharedPref: SharedPreferences) {
        sharedPref.edit {
            remove(Constants.isTracking)
        }
    }
}