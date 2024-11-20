package com.example.ytshare

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.zxing.client.result.URLTOResultParser

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavView : BottomNavigationView
    var homeFragment : HomeFragment = HomeFragment()
    var historyFragment : HistoryFragment = HistoryFragment()
    var settingsFragment : SettingsFragment = SettingsFragment()
    lateinit var queue : RequestQueue

    lateinit var sharedPref: SharedPreferences
    lateinit var db : DBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = DBHelper(this, null)


        sharedPref = this.getPreferences(Context.MODE_PRIVATE)

        queue = Volley.newRequestQueue(this@MainActivity)

        bottomNavView = findViewById(R.id.bottom_navigation)

        bottomNavView.setOnItemSelectedListener { menuItem ->
            when(menuItem.itemId){
                R.id.bottom_home ->{
                    replaceFragment(homeFragment)
                    true
                }
                R.id.bottom_bookmarks ->{
                    replaceFragment(historyFragment)
                    true
                }
                R.id.bottom_account ->{
                    replaceFragment(settingsFragment)
                    true
                }
                else -> false
            }
        }

        replaceFragment(homeFragment)

        when{
            intent?.action == Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    handleSendText(intent)
                }
            }
        }
    }

    fun replaceFragment(fragment:Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.frame_container, fragment).commit()
    }

    private fun handleSendText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            saveLink(it)
        }
    }

    private fun saveLink(text: String) {
        val link = Uri.parse(text)
        if (link.host.toString().contains("youtube.com") || link.host.toString().contains("youtu.be")) {
            val editor = sharedPref.edit()
            editor.putString("link", text).apply()
        }
    }

    public fun clearLink() {
        val editor = sharedPref.edit()
        editor.remove("link").apply()
    }
}