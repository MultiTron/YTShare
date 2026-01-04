package com.example.ytshare.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.example.ytshare.R
import com.example.ytshare.helpers.DBHelper
import org.json.JSONObject

@Composable
fun HomeScreen(
    savedLink: String?,
    ipAddress: String,
    isTracking: Boolean,
    queue: RequestQueue,
    db: DBHelper,
    onNavigateToSettings: () -> Unit,
    onClearLink: () -> Unit
) {
    val context = LocalContext.current
    var urlText by remember { mutableStateOf(modifyLink(savedLink, isTracking)) }
    var isLoading by remember { mutableStateOf(false) }
    val baseAddress = "http://$ipAddress/Share?link="

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        // IP Background with text
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToSettings() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp) // Matches android:height="30dp"
                    .background(
                        // Replicates the <gradient> tag
                        brush = Brush.linearGradient(
                            colors = listOf(
                                colorResource(id = R.color.red),      // startColor
                                colorResource(id = R.color.dark_red)  // endColor
                            )
                            // Note: XML angle="45" is Bottom-Left to Top-Right.
                            // Default linearGradient is Top-Left to Bottom-Right, which is visually very similar.
                        ),
                        // Replicates the <corners> tag
                        shape = RoundedCornerShape(
                            bottomStart = 20.dp,
                            bottomEnd = 20.dp
                        )
                    )
            )
            Text(
                text = ipAddress,
                color = Color.White,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 25.dp)
            )
        }

        Spacer(modifier = Modifier.height(50.dp))

        // URL Input
        OutlinedTextField(
            value = urlText,
            onValueChange = { urlText = it },
            label = { Text("Enter URL") },
            placeholder = { Text("URL Hint") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 24.sp)
        )

        // Share Button
        Button(
            onClick = {
                if (urlText.isNotEmpty()) {
                    isLoading = true
                    shareRequest(
                        link = urlText,
                        baseAddress = baseAddress,
                        queue = queue,
                        db = db,
                        context = context,
                        onSuccess = {
                            isLoading = false
                            urlText = ""
                            onClearLink()
                        },
                        onError = {
                            isLoading = false
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Share",
                fontSize = 35.sp
            )
        }

        // Progress Indicator
        if (isLoading) {
            Spacer(modifier = Modifier.height(100.dp))
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(48.dp)
            )
        }
    }
}

private fun modifyLink(url: String?, isTracking: Boolean): String {
    val protocolRegex = Regex("""^(https?://)""")
    val trackingRegex = Regex("""[\?&]si=[^&]+|[\?&]t=[^&]+""")
    val remainsRegex = Regex("""[?&]$""")

    val withoutProtocol = url?.replace(protocolRegex, "") ?: ""

    return if (isTracking) {
        withoutProtocol.replace(trackingRegex, "").replace(remainsRegex, "")
    } else {
        withoutProtocol
    }
}

private fun shareRequest(
    link: String,
    baseAddress: String,
    queue: RequestQueue,
    db: DBHelper,
    context: android.content.Context,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    val stringRequest = StringRequest(
        Request.Method.GET, "$baseAddress$link",
        { response ->
            saveLinkInfo(link, queue, db)
            Toast.makeText(context, response, Toast.LENGTH_LONG).show()
            onSuccess()
        },
        {
            Toast.makeText(
                context,
                "Could not establish connection with server",
                Toast.LENGTH_SHORT
            ).show()
            onError()
        })

    queue.add(stringRequest)
}

private fun saveLinkInfo(link: String, queue: RequestQueue, db: DBHelper) {
    val stringRequest = StringRequest(
        Request.Method.GET, "https://www.youtube.com/oembed?url=$link&format=json",
        { response ->
            val json = JSONObject(response)
            db.addLink(
                json.get("title").toString(),
                link,
                json.get("thumbnail_url").toString()
            )
        },
        {
            android.util.Log.e("LinkInfo", "Unable to reach YouTube Info Server...")
        })

    queue.add(stringRequest)
}
