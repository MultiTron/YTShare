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

class AccountFragment : Fragment() {

    private lateinit var mainActivity : MainActivity
    private lateinit var ipText : EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivity = (activity as MainActivity)
        ipText = view.findViewById(R.id.ip_text)

        view.findViewById<Button>(R.id.add_button).setOnClickListener {
            if (ipText.text.isNotEmpty()){
                mainActivity.ipAddress = ipText.text.toString()
                ipText.text.clear()
                it.hideKeyboard()
            }
        }

        view.findViewById<Button>(R.id.qr_button).setOnClickListener {
            //TO-DO
        }
    }

    private fun View.hideKeyboard() {
        val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }
}