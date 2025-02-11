package com.example.ytshare.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.example.ytshare.Constants
import com.example.ytshare.MainActivity
import com.example.ytshare.R
import com.example.ytshare.helpers.SharedPrefHelper
import org.json.JSONObject

class HomeFragment : Fragment() {

    private lateinit var mainActivity : MainActivity

    private lateinit var ipText: TextView
    private lateinit var uriText : EditText
    private lateinit var baseAddress : String

    private lateinit var progressBar: ProgressBar


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivity = (activity as MainActivity)
        ipText = view.findViewById(R.id.ip_text)

        val ipAddress = mainActivity.sharedPref.getString(Constants.ip, "0.0.0.0")
        baseAddress = "http://${ipAddress}/Share?link="
        ipText.text = ipAddress

        progressBar = view.findViewById(R.id.progressBar)
        progressBar.visibility = View.INVISIBLE

        uriText = view.findViewById(R.id.url_text)
        uriText.setText(modifyLink(mainActivity.sharedPref.getString(Constants.link, ""), mainActivity.sharedPref.getBoolean(Constants.isTracking, false)))

        view.findViewById<ImageView>(R.id.ip_background).setOnClickListener {
            mainActivity.replaceFragment(mainActivity.settingsFragment)
        }

        view.findViewById<Button>(R.id.url_button).setOnClickListener{
            if(uriText.text.isNotEmpty()){
                progressBar.visibility = View.VISIBLE

                shareRequest(uriText.text.toString())

                it.hideKeyboard()
            }
        }
    }

    private fun shareRequest(link:String){
        val stringRequest = StringRequest(
            Request.Method.GET, "${baseAddress}${link}",
            { response ->
                progressBar.visibility = View.INVISIBLE
                saveLinkInfo(link)

                uriText.text.clear()
                SharedPrefHelper.clearLink(mainActivity.sharedPref)
                Toast.makeText(mainActivity, response, Toast.LENGTH_LONG).show()
            },
            {
                progressBar.visibility = View.INVISIBLE
                Toast.makeText(mainActivity,
                    getString(R.string.could_not_establish_connection_with_server),
                    Toast.LENGTH_SHORT).show()
            })

        mainActivity.queue.add(stringRequest)
    }

    private fun modifyLink(url: String?, isTracking: Boolean): String? {
        val protocolRegex = Regex("""^(https?://)""")
        val trackingRegex = Regex("""[\?&]si=[^&]+|[\?&]t=[^&]+""")
        val remainsRegex = Regex("""[?&]$""")

        val withoutProtocol = url?.replace(protocolRegex, "")

        if (isTracking){
            return withoutProtocol?.replace(trackingRegex, "")?.replace(remainsRegex, "")
        }
        return withoutProtocol
    }

    private fun saveLinkInfo(link: String){
        val stringRequest = StringRequest(
            Request.Method.GET, "https://www.youtube.com/oembed?url=${link}&format=json",
            { response ->
                val json = JSONObject(response)
                mainActivity.db.addLink(json.get("title").toString(),
                    link,
                    json.get("thumbnail_url").toString())
            },
            {
                Log.e("LinkInfo", "Unable to reach YouTube Info Server...")
            })

        mainActivity.queue.add(stringRequest)
    }

    private fun View.hideKeyboard() {
        val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }
}