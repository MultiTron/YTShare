package com.example.ytshare

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavView : BottomNavigationView
    public var homeFragment : HomeFragment = HomeFragment()
    public var historyFragment : HistoryFragment = HistoryFragment()
    public var accountFragment : AccountFragment = AccountFragment()
    public lateinit var queue : RequestQueue
    public var ipAddress : String = "0.0.0.0"

    var sharedUri : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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
                    replaceFragment(accountFragment)
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

    public fun replaceFragment(fragment:Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.frame_container, fragment).commit()
    }

    private fun handleSendText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            sharedUri = it
        }
    }
}