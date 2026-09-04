package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.screens.MusicLibraryScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MusicDarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current

                var showSplashScreen by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(1200)
                    showSplashScreen = false
                }

                val requiredPermissions = remember {
                    val list = mutableListOf<String>()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        list.add(Manifest.permission.READ_MEDIA_AUDIO)
                        list.add(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    list.toTypedArray()
                }

                fun checkHasAudioPermission(): Boolean {
                    val audioPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    return ContextCompat.checkSelfPermission(
                        context,
                        audioPerm
                    ) == PackageManager.PERMISSION_GRANTED
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val audioPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    val isAudioGranted = permissions[audioPerm] == true || checkHasAudioPermission()
                    viewModel.onPermissionResult(isAudioGranted)
                }

                // Initial permission check & request
                LaunchedEffect(Unit) {
                    val alreadyGranted = checkHasAudioPermission()
                    viewModel.onPermissionResult(alreadyGranted)
                    if (!alreadyGranted) {
                        permissionLauncher.launch(requiredPermissions)
                    }
                }

                // Re-scan when app returns to foreground (ON_RESUME) to detect new downloads
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            val granted = checkHasAudioPermission()
                            viewModel.onPermissionResult(granted)
                            if (granted) {
                                viewModel.refreshLibrary()
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MusicDarkBackground
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MusicLibraryScreen(
                            viewModel = viewModel,
                            onRequestPermission = {
                                permissionLauncher.launch(requiredPermissions)
                            },
                            onOpenSettings = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                                context.startActivity(intent)
                            }
                        )

                        AnimatedVisibility(
                            visible = showSplashScreen,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            SplashScreen()
                        }
                    }
                }
            }
        }
    }
}
