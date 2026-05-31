package com.example.ytshare.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ytshare.data.remote.dto.FriendshipOutputDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel,
    onFriendClick: (friendUserId: String) -> Unit
) {
    val friends by viewModel.friends.collectAsState()
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf<FriendshipOutputDto?>(null) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Red,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Add Friend")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.loadData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!isLoading && friends.isEmpty() && pendingRequests.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("No friends yet. Tap + to add someone.", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    if (pendingRequests.isNotEmpty()) {
                        item {
                            SectionHeader("PENDING REQUESTS (${pendingRequests.size})")
                        }
                        items(pendingRequests, key = { it.id }) { request ->
                            PendingRequestRow(
                                friendship = request,
                                isIncoming = viewModel.isIncomingRequest(request),
                                friendUser = viewModel.getFriendUser(request),
                                onAccept = { viewModel.acceptRequest(request.id) },
                                onReject = { viewModel.rejectRequest(request.id) }
                            )
                        }
                    }
                    if (friends.isNotEmpty()) {
                        item {
                            SectionHeader("FRIENDS (${friends.size})")
                        }
                        items(friends, key = { it.id }) { friendship ->
                            val friendUser = viewModel.getFriendUser(friendship)
                            FriendRow(
                                name = "${friendUser.firstName} ${friendUser.lastName}",
                                email = friendUser.email,
                                onClick = { onFriendClick(friendUser.id) },
                                onLongClick = { showRemoveDialog = friendship }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFriendDialog(
            onDismiss = { showAddDialog = false },
            onSend = { email ->
                viewModel.sendFriendRequest(email)
                showAddDialog = false
            }
        )
    }

    showRemoveDialog?.let { friendship ->
        val friendUser = viewModel.getFriendUser(friendship)
        AlertDialog(
            onDismissRequest = { showRemoveDialog = null },
            title = { Text("Remove Friend") },
            text = { Text("Remove ${friendUser.firstName} ${friendUser.lastName} from your friends?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeFriend(friendship.id)
                    showRemoveDialog = null
                }) { Text("Remove", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFFEF9A9A),
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun Avatar(letter: Char, color: Color, size: Int = 40) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.uppercase(),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size / 2.5).sp
        )
    }
}

private val avatarColors = listOf(
    Color(0xFF6A1B9A), Color(0xFF00695C), Color(0xFFE65100),
    Color(0xFF1565C0), Color(0xFFC62828), Color(0xFF2E7D32)
)

private fun avatarColor(name: String): Color {
    return avatarColors[name.hashCode().mod(avatarColors.size).let { if (it < 0) it + avatarColors.size else it }]
}

@Composable
private fun FriendRow(name: String, email: String, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(letter = name.first(), color = avatarColor(name))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(email, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
    HorizontalDivider(modifier = Modifier.padding(start = 68.dp))
}

@Composable
private fun PendingRequestRow(
    friendship: FriendshipOutputDto,
    isIncoming: Boolean,
    friendUser: com.example.ytshare.data.remote.dto.UserOutputDto,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(letter = friendUser.email.first(), color = avatarColor(friendUser.email))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(friendUser.email, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (isIncoming) "Incoming request" else "Outgoing · Pending",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        if (isIncoming) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Accept", fontSize = 12.sp) }
                OutlinedButton(
                    onClick = onReject,
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Reject", fontSize = 12.sp) }
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 68.dp))
}

@Composable
private fun AddFriendDialog(onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Friend") },
        text = {
            Column {
                Text("Enter the email address of the person you want to add.", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("friend@example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (email.isNotBlank()) onSend(email.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) { Text("Send Request") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
