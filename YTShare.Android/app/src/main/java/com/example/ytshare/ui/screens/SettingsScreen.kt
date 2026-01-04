package com.example.ytshare.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ytshare.models.HostModel

@Composable
fun SettingsScreen(
    hosts: List<HostModel>,
    isTrackingEnabled: Boolean,
    onHostSelected: (HostModel) -> Unit,
    onTrackingChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = "Available Devices",
            fontSize = 30.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        // Host List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(hosts) { host ->
                HostCard(
                    host = host,
                    onHostClick = {
                        onHostSelected(host)
                        Toast.makeText(context, "Setting selected ip...", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Tracking Checkbox
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTrackingChanged(!isTrackingEnabled) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isTrackingEnabled,
                onCheckedChange = { onTrackingChanged(it) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Remove tracking from URLs",
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun HostCard(
    host: HostModel,
    onHostClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clickable { onHostClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = host.hostName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 10.dp)
            )
            Text(
                text = host.toString(),
                fontSize = 18.sp,
                fontStyle = FontStyle.Italic,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}
