package com.example.ui.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(
    imageUri: String,
    onCropComplete: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // 4 corners of the document
    var topLeft by remember { mutableStateOf(Offset.Zero) }
    var topRight by remember { mutableStateOf(Offset.Zero) }
    var bottomLeft by remember { mutableStateOf(Offset.Zero) }
    var bottomRight by remember { mutableStateOf(Offset.Zero) }
    
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        val uri = Uri.parse(imageUri)
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val decoded = BitmapFactory.decodeStream(stream)
            bitmap = decoded
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crop Document") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        bitmap?.let { img ->
                            if (viewSize.width > 0 && viewSize.height > 0) {
                                val cropped = applyPerspectiveCorrection(
                                    img,
                                    viewSize,
                                    topLeft, topRight, bottomLeft, bottomRight
                                )
                                // Save to temp file and return
                                val tempFile = File.createTempFile("cropped_", ".jpg", context.cacheDir)
                                FileOutputStream(tempFile).use { out ->
                                    cropped.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                }
                                onCropComplete(Uri.fromFile(tempFile))
                            }
                        }
                    }) {
                        Icon(Icons.Default.Check, "Accept")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
                .onSizeChanged { size ->
                    viewSize = size
                    if (!isInitialized && size.width > 0 && size.height > 0) {
                        // Initial inset points
                        val paddingX = size.width * 0.1f
                        val paddingY = size.height * 0.1f
                        topLeft = Offset(paddingX, paddingY)
                        topRight = Offset(size.width - paddingX, paddingY)
                        bottomLeft = Offset(paddingX, size.height - paddingY)
                        bottomRight = Offset(size.width - paddingX, size.height - paddingY)
                        isInitialized = true
                    }
                }
        ) {
            bitmap?.let { img ->
                Image(
                    bitmap = img.asImageBitmap(),
                    contentDescription = "Document to crop",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            if (isInitialized) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val touchPos = change.position
                                // Find closest point
                                val threshold = 100.dp.toPx()
                                
                                val distTl = hypot(touchPos.x - topLeft.x, touchPos.y - topLeft.y)
                                val distTr = hypot(touchPos.x - topRight.x, touchPos.y - topRight.y)
                                val distBl = hypot(touchPos.x - bottomLeft.x, touchPos.y - bottomLeft.y)
                                val distBr = hypot(touchPos.x - bottomRight.x, touchPos.y - bottomRight.y)
                                
                                val minDist = minOf(distTl, distTr, distBl, distBr)
                                if (minDist < threshold) {
                                    val safeX = max(0f, min(touchPos.x, viewSize.width.toFloat()))
                                    val safeY = max(0f, min(touchPos.y, viewSize.height.toFloat()))
                                    val safePoint = Offset(safeX, safeY)
                                    
                                    when (minDist) {
                                        distTl -> topLeft = safePoint
                                        distTr -> topRight = safePoint
                                        distBl -> bottomLeft = safePoint
                                        distBr -> bottomRight = safePoint
                                    }
                                }
                            }
                        }
                ) {
                    val path = Path().apply {
                        moveTo(topLeft.x, topLeft.y)
                        lineTo(topRight.x, topRight.y)
                        lineTo(bottomRight.x, bottomRight.y)
                        lineTo(bottomLeft.x, bottomLeft.y)
                        close()
                    }

                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.5f),
                        style = Stroke(width = 4.dp.toPx())
                    )
                    drawPath(
                        path = path,
                        color = Color.Green.copy(alpha = 0.3f)
                    )

                    val radius = 12.dp.toPx()
                    drawCircle(Color.White, radius, topLeft)
                    drawCircle(Color.White, radius, topRight)
                    drawCircle(Color.White, radius, bottomLeft)
                    drawCircle(Color.White, radius, bottomRight)
                    
                    drawCircle(Color.Green, radius * 0.8f, topLeft)
                    drawCircle(Color.Green, radius * 0.8f, topRight)
                    drawCircle(Color.Green, radius * 0.8f, bottomLeft)
                    drawCircle(Color.Green, radius * 0.8f, bottomRight)
                }
            }
        }
    }
}

private fun applyPerspectiveCorrection(
    sourceBitmap: Bitmap,
    viewSize: IntSize,
    tl: Offset,
    tr: Offset,
    bl: Offset,
    br: Offset
): Bitmap {
    // Determine the scaling factor between the view and the actual bitmap
    // Assuming ContentScale.Fit is used, we need to calculate the actual image bounds in the view
    val imgRatio = sourceBitmap.width.toFloat() / sourceBitmap.height.toFloat()
    val viewRatio = viewSize.width.toFloat() / viewSize.height.toFloat()

    var drawnWidth = viewSize.width.toFloat()
    var drawnHeight = viewSize.height.toFloat()
    var offsetX = 0f
    var offsetY = 0f

    if (imgRatio > viewRatio) {
        drawnHeight = drawnWidth / imgRatio
        offsetY = (viewSize.height - drawnHeight) / 2f
    } else {
        drawnWidth = drawnHeight * imgRatio
        offsetX = (viewSize.width - drawnWidth) / 2f
    }

    val scaleX = sourceBitmap.width / drawnWidth
    val scaleY = sourceBitmap.height / drawnHeight

    val getScaledPoint = { pt: Offset ->
        val mappedX = (pt.x - offsetX) * scaleX
        val mappedY = (pt.y - offsetY) * scaleY
        // Clamp mapping to actual bitmap limits
        floatArrayOf(
            max(0f, min(mappedX, sourceBitmap.width.toFloat())),
            max(0f, min(mappedY, sourceBitmap.height.toFloat()))
        )
    }

    val src = floatArrayOf(
        getScaledPoint(tl)[0], getScaledPoint(tl)[1],
        getScaledPoint(tr)[0], getScaledPoint(tr)[1],
        getScaledPoint(br)[0], getScaledPoint(br)[1],
        getScaledPoint(bl)[0], getScaledPoint(bl)[1]
    )

    // Calculate width and height of new document
    val widthTop = hypot((src[2] - src[0]).toDouble(), (src[3] - src[1]).toDouble())
    val widthBottom = hypot((src[4] - src[6]).toDouble(), (src[5] - src[7]).toDouble())
    val newWidth = max(widthTop, widthBottom).toInt()

    val heightLeft = hypot((src[6] - src[0]).toDouble(), (src[7] - src[1]).toDouble())
    val heightRight = hypot((src[4] - src[2]).toDouble(), (src[5] - src[3]).toDouble())
    val newHeight = max(heightLeft, heightRight).toInt()
    
    if (newWidth <= 0 || newHeight <= 0) return sourceBitmap

    val dst = floatArrayOf(
        0f, 0f,
        newWidth.toFloat(), 0f,
        newWidth.toFloat(), newHeight.toFloat(),
        0f, newHeight.toFloat()
    )

    val matrix = Matrix()
    matrix.setPolyToPoly(src, 0, dst, 0, 4)

    val outputBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(outputBitmap)
    canvas.drawBitmap(sourceBitmap, matrix, Paint(Paint.ANTI_ALIAS_FLAG))
    
    return outputBitmap
}
