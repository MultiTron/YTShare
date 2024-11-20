package com.example.ytshare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult

class SettingsFragment : Fragment() {

    private lateinit var mainActivity : MainActivity
    private lateinit var ipText : EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivity = (activity as MainActivity)
        ipText = view.findViewById(R.id.ip_text)

        view.findViewById<Button>(R.id.add_button).setOnClickListener {
            // TODO Regex for IPv4 ^((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.?\b){4}$
            if (ipText.text.isNotEmpty() and validateIp(ipText.text.toString())){
                saveIp(ipText.text.toString())
                Toast.makeText(mainActivity, resources.getString(R.string.ip_set) + ipText.text, Toast.LENGTH_SHORT).show()
                ipText.text.clear()
                it.hideKeyboard()
            }
            else{
                ipText.text.clear()
                Toast.makeText(mainActivity, resources.getString(R.string.ip_invalid), Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<Button>(R.id.qr_button).setOnClickListener {
            startQrCodeScanner()
        }
    }

    private fun startQrCodeScanner() {
        val integrator = IntentIntegrator.forSupportFragment(this)
        integrator.setPrompt(resources.getString(R.string.qr_prompt))
        integrator.setCameraId(0)
        integrator.setBeepEnabled(false)
        integrator.setOrientationLocked(false)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                // Handle cancel
                Toast.makeText(mainActivity, resources.getString(R.string.qr_cancel), Toast.LENGTH_SHORT).show()
            }
            else {
                // Handle scanned result
                if (validateIp(result.contents)){
                    saveIp(result.contents)
                    Toast.makeText(mainActivity, resources.getString(R.string.ip_set) + result.contents, Toast.LENGTH_SHORT).show()
                }
                else{
                    Toast.makeText(mainActivity, resources.getString(R.string.ip_invalid), Toast.LENGTH_SHORT).show()
                }
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun saveIp(text:String){
        val editor = mainActivity.sharedPref.edit()
        editor.putString("ip", text)
        editor.apply()
    }

    private fun validateIp(text: String):Boolean {
        val regex = Regex("^(25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})(\\.(25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})){3}\$")
        return text.matches(regex)
    }

    private fun View.hideKeyboard() {
        val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }
}