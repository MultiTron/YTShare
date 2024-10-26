package com.example.ytshare

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest

class HomeFragment : Fragment() {

    private lateinit var uriText : EditText
    private lateinit var outputText : TextView
    private lateinit var mainActivity: MainActivity
    val baseAddress = "http://192.168.170.42:5233/Share?link="
//    private lateinit var ipSpinner: Spinner

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        mainActivity = (activity as MainActivity)
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        ipSpinner = view.findViewById(R.id.ip_select)
//        ArrayAdapter.createFromResource(
//            context,
//            R.array.ips,
//            android.R.layout.simple_spinner_item
//        ).also{
//            adapter -> adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//            ipSpinner.adapter = adapter
//        }
        outputText = view.findViewById(R.id.output_text)
        uriText = view.findViewById(R.id.url_text)
        uriText.setText(mainActivity.sharedUri)
        view.findViewById<Button>(R.id.url_button).setOnClickListener{
            outputText.setText("Somthing is happening")
            shareRequest()
            uriText.text.clear()
        }
    }
    fun shareRequest(){
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
}