package com.example.documentscanner

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.documentscanner.ui.theme.DocumentScannerTheme
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(100)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .build()
        val scanner = GmsDocumentScanning.getClient(options)
        setContent {
            DocumentScannerTheme {
                var imageUris by remember {
                    mutableStateOf<List<Uri>>(emptyList())
                }
                var pdfName by remember { mutableStateOf("") }
                var scanningInProgress by remember { mutableStateOf(false) }
                var cardVisible by remember { mutableStateOf(false) }

                val imagescanner = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult(),
                    onResult = { result ->
                        if (result.resultCode == ComponentActivity.RESULT_OK) {
                            val resultData = result.data
                            val result = GmsDocumentScanningResult.fromActivityResultIntent(resultData)
                            imageUris = result?.pages?.map { it.imageUri } ?: emptyList()

                            result?.pdf?.let { pdf ->
                                val filename = if (pdfName.isEmpty()) {
                                    System.currentTimeMillis().toString() + ".pdf"
                                } else {
                                    pdfName + ".pdf"
                                }
                                val downloadDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)
                                val file = File(downloadDir, filename)
                                val fos = FileOutputStream(file)
                                contentResolver.openInputStream(pdf.uri)?.use {
                                    it.copyTo(fos)
                                }
                                // Show toast after PDF is saved
                                Toast.makeText(
                                    applicationContext,
                                    "PDF saved successfully!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        scanningInProgress = false
                    }
                )

                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        imageUris.forEach { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (!scanningInProgress) {
                            Button(
                                onClick = {
                                    cardVisible = true
                                },
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Text("Scan PDF/Document")
                            }
                        }
                        if (cardVisible) {
                            Card(
                                modifier = Modifier.fillMaxWidth(0.8f),
                                elevation = CardDefaults.cardElevation(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    TextField(
                                        value = pdfName,
                                        onValueChange = { pdfName = it },
                                        label = { Text("Enter PDF Name") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(

                                            onClick = {
                                                scanningInProgress = true
                                                scanner.getStartScanIntent(this@MainActivity)
                                                    .addOnSuccessListener { intentSender ->
                                                        imagescanner.launch(
                                                            IntentSenderRequest.Builder(intentSender).build()
                                                        )
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Toast.makeText(
                                                            applicationContext,
                                                            "Error: ${e.message}",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                        cardVisible = false
                                                    }
                                            },
                                            modifier = Modifier.padding(top = 16.dp)
                                        ) {
                                            Text("OK")
                                        }
                                        Button(
                                            onClick = {
                                                cardVisible = false
                                            },
                                            modifier = Modifier.padding(top = 16.dp, start = 16.dp)
                                        ) {
                                            Text("Cancel")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}