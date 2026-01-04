package com.example.ytshare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ytshare.models.LinkModel

@Composable
fun HistoryScreen(
    videos: List<LinkModel>,
    isDescending: Boolean,
    onDeleteAll: () -> Unit,
    onSortChanged: (Boolean) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val sortedVideos = remember(videos, isDescending) {
        if (isDescending) {
            videos.sortedByDescending { it.date }
        } else {
            videos.sortedBy { it.date }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Delete All Button
            Button(
                onClick = { showDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Delete History",
                    fontSize = 35.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Video List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedVideos) { video ->
                    VideoHistoryCard(video = video)
                }
            }
        }

        // Sort Button (FAB style)
        FloatingActionButton(
            onClick = { onSortChanged(!isDescending) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 120.dp),
            containerColor = Color.Red,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = if (isDescending) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                contentDescription = "Sort",
                modifier = Modifier.size(30.dp)
            )
        }
    }

    // Delete Confirmation Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Delete History") },
            text = { Text("Are you sure you want to delete all history?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAll()
                        showDialog = false
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text("No")
                }
            }
        )
    }
}

@Composable
fun VideoHistoryCard(video: LinkModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(horizontal = 10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(video.thumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = "Video Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(80.dp)
                    .height(60.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Video Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = video.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.link,
                    fontSize = 18.sp,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.date,
                    fontSize = 20.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}
