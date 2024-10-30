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
import android.widget.TextView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest

class HomeFragment : Fragment() {

    private lateinit var mainActivity : MainActivity

    private lateinit var ipText: TextView
    private lateinit var uriText : EditText
    private lateinit var outputText : TextView
    private lateinit var baseAddress : String

    override fun onResume() {
        super.onResume()
    }

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

        baseAddress = "http://${mainActivity.ipAddress}:5233/Share?link="
        ipText.text = mainActivity.ipAddress

        outputText = view.findViewById(R.id.output_text)
        uriText = view.findViewById(R.id.url_text)
        uriText.setText(mainActivity.sharedUri)

        view.findViewById<ImageView>(R.id.ip_background).setOnClickListener {
            mainActivity.replaceFragment(mainActivity.accountFragment)
        }

        view.findViewById<Button>(R.id.url_button).setOnClickListener{
            outputText.text = "Something is happening"
            shareRequest()
            uriText.text.clear()
            it.hideKeyboard()
        }
    }

    private fun shareRequest(){
        val stringRequest = StringRequest(
            Request.Method.GET, "${baseAddress}${uriText.text}",
            { response ->
                // Display the first 500 characters of the response string.
                outputText.text = response
            },
            { outputText.text = "That didn't work!" })

        // Add the request to the RequestQueue.
        mainActivity.queue.add(stringRequest)
    }

    private fun View.hideKeyboard() {
        val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }
}