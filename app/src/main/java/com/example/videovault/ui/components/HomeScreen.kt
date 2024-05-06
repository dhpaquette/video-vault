package com.example.videovault.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.videovault.Destination
import com.example.videovault.R
import com.example.videovault.data.model.VideoRecording
import com.example.videovault.ui.theme.AltText
import com.example.videovault.ui.theme.VideoVaultTheme
import com.example.videovault.viewmodel.RecordingsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    recordingsViewModel: RecordingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrowback),
                            contentDescription = "Back",
                            tint = Color.Unspecified
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Show info */ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_help),
                            contentDescription = "Help",
                            tint = Color.Unspecified
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }

    ) {
        innerPadding ->
        Screen(
            navController,
            innerPadding,
            recordingsViewModel,
            snackbarHostState,
            context
        )
    }
}
@Composable
fun Screen(
    navController: NavHostController,
    innerPaddingValues: PaddingValues,
    recordingsViewModel: RecordingsViewModel,
    snackbarHostState: SnackbarHostState,
    context: Context
) {

    val recordings by recordingsViewModel.recordings.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedRecording by remember { mutableStateOf<VideoRecording?>(null) }
    val coroutineScope = rememberCoroutineScope()


    // Dialog to confirm deleting recording
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Delete Recording") },
            text = { Text("Are you sure you want to delete this recording?") },
            confirmButton = {
                TextButton(onClick = {
                    selectedRecording?.let {
                        recordingsViewModel.deleteRecording(it.id)
                        showDialog = false
                    }
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = Modifier.padding(innerPaddingValues)) {
        // Header content
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "Android Video Recorder",
                style = MaterialTheme.typography.headlineMedium,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (recordings.isNotEmpty())
                            stringResource(id = R.string.alt_description)
                        else
                            stringResource(id = R.string.description),
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(recordings.size) { recording ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(recording) {
                            detectTapGestures(
                                onLongPress = {
                                    selectedRecording = recordings[recording]
                                    showDialog = true
                                },
                                onTap = {
                                    openVideo(context, recordings[recording])
                                }
                            )
                        }
                        .padding(
                            top = 16.dp,
                            start = 24.dp,
                            end = 24.dp,
                            bottom = 16.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_camera),
                        contentDescription = "Camera",
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Recording ${recordings[recording].counter}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${String.format("%.1f", recordings[recording].videoSize)} MB",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AltText
                    )
                }
                Divider(
                    modifier = Modifier
                        .padding(horizontal = 24.dp),
                    thickness = 1.dp
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 28.dp,
                            start = 36.dp,
                            end = 36.dp,
                            bottom = 28.dp
                        )
                        .clickable { navController.navigate(Destination.CameraDestination.route) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = "Add",
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(24.dp)) // Adjust space as needed
                    Text(
                        text = stringResource(R.string.add_recording),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
private fun openVideo(context: Context, videoRecording: VideoRecording) {
    val videoFile = File(videoRecording.filePath)
    val videoUri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        videoFile
    )

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(videoUri, "video/*")

        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "No application found to open the video", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun previewNavController() = rememberNavController().apply {
    //No-op for preview purposes
}

@Preview
@Composable
fun HomeScreenPreview() {
    VideoVaultTheme {
        HomeScreen(navController = previewNavController())
    }
}

