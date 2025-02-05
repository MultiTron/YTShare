package com.example.ytshare

import android.content.Context
import android.os.Bundle
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

        val ipAddress = mainActivity.sharedPref.getString("ip", "0.0.0.0")
        baseAddress = "http://${ipAddress}:5014/Share?link="
        ipText.text = ipAddress

        progressBar = view.findViewById(R.id.progressBar)
        progressBar.visibility = View.INVISIBLE

        uriText = view.findViewById(R.id.url_text)
        uriText.setText(mainActivity.sharedPref.getString("link", ""))

        view.findViewById<ImageView>(R.id.ip_background).setOnClickListener {
            mainActivity.replaceFragment(mainActivity.settingsFragment)
        }

        view.findViewById<Button>(R.id.url_button).setOnClickListener{
            if(uriText.text.isNotEmpty()){
                progressBar.visibility = View.VISIBLE

                shareRequest(removeProtocol(uriText.text.toString()))

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
                mainActivity.clearLink()
                Toast.makeText(mainActivity, response, Toast.LENGTH_LONG).show()
            },
            {
                progressBar.visibility = View.INVISIBLE
                Toast.makeText(mainActivity, "That didn't work!", Toast.LENGTH_SHORT).show()
            })

        // Add the request to the RequestQueue.
        mainActivity.queue.add(stringRequest)
    }

    private fun removeProtocol(url: String): String {
        val regex = Regex("^(https?://)")
        return url.replace(regex, "")
    }

    private fun saveLinkInfo(link: String){
        val stringRequest = StringRequest(
            Request.Method.GET, "https://www.youtube.com/oembed?url=${link}&format=json",
            { response ->
                //progressBar.visibility = View.INVISIBLE
                val json = JSONObject(response)
                mainActivity.db.addLink(json.get("title").toString(), link, json.get("thumbnail_url").toString())
            },
            {
                //progressBar.visibility = View.INVISIBLE
            })

        // Add the request to the RequestQueue.
        mainActivity.queue.add(stringRequest)
    }

    private fun View.hideKeyboard() {
        val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }
}