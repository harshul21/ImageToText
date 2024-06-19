package com.example.chatgptapi

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.startActivityForResult
import com.example.chatgptapi.ui.theme.ChatGptApiTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChatGptApiTheme {

                MyApp()
//                Column(
//                    modifier = Modifier.fillMaxSize(),
//                    verticalArrangement = Arrangement.Center,
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//
//
//                    Button(onClick = {
//                        CoroutineScope(Dispatchers.IO).launch {
//                            getResponse()
//                        }
//                    }) {
//                        Text("Send")
//                    }
//                }
            }
        }

    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MyApp() {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var text by remember { mutableStateOf<String>("") }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    val imageLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        bitmap?.let {
            Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.size(200.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { imageLauncher.launch("image/*") }) {
            Text("Select Image")

            bitmap?.let { bitmap->
                File(context.filesDir, "screenshot.png")
                    .writeBitmap(bitmap, Bitmap.CompressFormat.PNG, 85)

                Log.i("MYLOG","${context.filesDir}")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                text = getResponse()
            }
        }) {
            Text("Recognize Text")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text)
    }
}

private fun File.writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int) {
    outputStream().use { out ->
        bitmap.compress(format, quality, out)
        out.flush()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun encodeImage(imagePath: String): String {
    val file = File(imagePath)
    val bytes = file.readBytes()

    Log.i("MYLOG", "Bytes: ${bytes.size}")
    return Base64.getEncoder().encodeToString(bytes)
}


@RequiresApi(Build.VERSION_CODES.O)
fun getResponse(): String {
    val apiKey = "API_KEY"
    var rtrResponse = ""
    // Path to your image
    val imagePath = "/data/user/0/com.example.chatgptapi/files/screenshot.png"

    // Getting the base64 string
    val base64Image = encodeImage(imagePath)

    val client = OkHttpClient()

    val mediaType = "application/json".toMediaTypeOrNull()
    val body = RequestBody.create(
        mediaType, """
        {
          "model": "gpt-4o",
          "messages": [
            {
              "role": "user",
              "content": [
                {
                  "type": "text",
                  "text": "What is the answer to the question in the image"
                },
                {
                  "type": "image_url",
                  "image_url": {
                    "url": "data:image/jpeg;base64,$base64Image"
                  }
                }
              ]
            }
          ],
          "max_tokens": 300
        }
    """.trimIndent())

    val request = Request.Builder()
        .url("https://api.openai.com/v1/chat/completions")
        .post(body)
        .addHeader("Content-Type", "application/json")
        .addHeader("Authorization", "Bearer $apiKey")
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    response.use {
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")

                        val jsonResponse = JSONObject(response.body?.string())
                        val content = jsonResponse
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")

                        rtrResponse = content
                        Log.i("MYLOG", "Parsed Content: $content")

                }
            }
        }
    })

    return rtrResponse
}



