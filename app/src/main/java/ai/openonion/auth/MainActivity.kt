package ai.openonion.auth

import android.content.ClipData
import android.os.Bundle
import android.os.PersistableBundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.openonion.auth.model.TotpCredential
import ai.openonion.auth.otp.TotpGenerator
import ai.openonion.auth.ui.OpenOnionTheme
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: AuthViewModel by viewModels()
    private var addDialogOpen by mutableStateOf(false)
    private var scannedUri by mutableStateOf("")

    private val scanner = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let {
            scannedUri = it
            addDialogOpen = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.ALLOW_SCREENSHOTS) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        setContent {
            OpenOnionTheme {
                val credentials by viewModel.credentials.collectAsStateWithLifecycle()
                AuthScreen(
                    credentials = credentials,
                    onAdd = { addDialogOpen = true },
                    onDelete = viewModel::deleteCredential,
                )

                if (addDialogOpen) {
                    AddCredentialDialog(
                        initialValue = scannedUri,
                        onDismiss = {
                            addDialogOpen = false
                            scannedUri = ""
                        },
                        onScan = ::launchScanner,
                        onImport = { rawUri ->
                            viewModel.importCredential(rawUri).also { error ->
                                if (error == null) {
                                    addDialogOpen = false
                                    scannedUri = ""
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    private fun launchScanner() {
        val options = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt("Scan a TOTP QR code")
            .setBeepEnabled(false)
            .setOrientationLocked(true)
        scanner.launch(options)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthScreen(
    credentials: List<TotpCredential>,
    onAdd: () -> Unit,
    onDelete: (TotpCredential) -> Unit,
) {
    var credentialToDelete by remember { mutableStateOf<TotpCredential?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("OpenOnion Auth", fontWeight = FontWeight.Bold)
                        Text(
                            "Offline TOTP",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Text("Add", modifier = Modifier.padding(horizontal = 16.dp))
            }
        },
    ) { padding ->
        if (credentials.isEmpty()) {
            EmptyState(padding, onAdd)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(credentials, key = { it.id }) { credential ->
                    CredentialCard(
                        credential = credential,
                        onDelete = { credentialToDelete = credential },
                    )
                }
            }
        }
    }

    credentialToDelete?.let { credential ->
        AlertDialog(
            onDismissRequest = { credentialToDelete = null },
            title = { Text("Remove ${credential.displayIssuer}?") },
            text = { Text("This removes the encrypted credential from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(credential)
                        credentialToDelete = null
                    },
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { credentialToDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun EmptyState(padding: PaddingValues, onAdd: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No codes yet", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                "Scan a standard TOTP QR code. Secrets stay encrypted on this device.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAdd) { Text("Add your first account") }
        }
    }
}

@Composable
private fun CredentialCard(credential: TotpCredential, onDelete: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(credential.id) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L - (now % 1_000L))
        }
    }
    val code = TotpGenerator.generate(
        secret = credential.secret,
        timestampMillis = now,
        periodSeconds = credential.periodSeconds,
        digits = credential.digits,
        algorithm = credential.algorithm,
    )
    val remaining = TotpGenerator.secondsRemaining(now, credential.periodSeconds)
    val clipboard = LocalClipboard.current
    val clipboardScope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        credential.displayIssuer,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (credential.accountName != credential.displayIssuer) {
                        Text(
                            credential.accountName,
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                TextButton(onClick = onDelete) { Text("Remove") }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                code.chunked(3).joinToString(" "),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { remaining.toFloat() / credential.periodSeconds },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "$remaining seconds",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                TextButton(
                    onClick = {
                        clipboardScope.launch {
                            val clip = ClipData.newPlainText("TOTP code", code).apply {
                                description.extras = PersistableBundle().apply {
                                    putBoolean("android.content.extra.IS_SENSITIVE", true)
                                }
                            }
                            clipboard.setClipEntry(ClipEntry(clip))
                        }
                    },
                ) {
                    Text("Copy code")
                }
            }
        }
    }
}

@Composable
private fun AddCredentialDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onScan: () -> Unit,
    onImport: (String) -> String?,
) {
    var rawUri by remember(initialValue) { mutableStateOf(initialValue) }
    var error by remember(initialValue) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add TOTP account") },
        text = {
            Column {
                Text("Scan the website's QR code, or paste its otpauth URI.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
                    Text("Scan QR code")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = rawUri,
                    onValueChange = {
                        rawUri = it
                        error = null
                    },
                    label = { Text("otpauth://totp/…") },
                    minLines = 3,
                    isError = error != null,
                    supportingText = error?.let { message -> { Text(message) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Version 0.1 keeps credentials only on this device. Use test accounts during the developer preview.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = rawUri.isNotBlank(),
                onClick = { error = onImport(rawUri) },
            ) { Text("Import") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
