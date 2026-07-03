package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.Event
import com.example.data.EventType
import java.io.File
import java.io.FileOutputStream

object EventExporter {

    fun generateEventSharingImage(
        context: Context,
        events: List<Event>,
        settings: CalculationSettings,
        includeBackgroundPhotos: Boolean = true
    ): File? {
        if (events.isEmpty()) return null

        val cardWidth = 800
        val cardHeight = 220
        val gap = 30
        val padding = 40

        val totalWidth = cardWidth + padding * 2
        val totalHeight = (cardHeight + gap) * events.size - gap + padding * 2

        val bitmap = try {
            Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            val scaledHeight = (120 + gap) * events.size - gap + padding * 2
            Bitmap.createBitmap(totalWidth, scaledHeight, Bitmap.Config.ARGB_8888)
        }
        val canvas = Canvas(bitmap)

        // Background canvas paint
        val bgPaint = Paint().apply {
            color = Color.parseColor("#12131C")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, totalWidth.toFloat(), totalHeight.toFloat(), bgPaint)

        val cardPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 34f
            isFakeBoldText = true
        }

        val datePaint = Paint().apply {
            isAntiAlias = true
            textSize = 24f
        }

        val ddayPaint = Paint().apply {
            isAntiAlias = true
            textSize = 54f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }

        val notesPaint = Paint().apply {
            isAntiAlias = true
            textSize = 20f
            style = Paint.Style.FILL
        }

        var currentY = padding.toFloat()

        for (event in events) {
            val currentCardHeight = if (bitmap.height < totalHeight) 120f else cardHeight.toFloat()
            val cardRect = RectF(
                padding.toFloat(),
                currentY,
                (padding + cardWidth).toFloat(),
                currentY + currentCardHeight
            )

            // Setup custom colors
            val textColorStr = event.textColorHex.takeIf { it.startsWith("#") } ?: "#FFFFFF"
            val textColor = try { Color.parseColor(textColorStr) } catch (e: Exception) { Color.WHITE }
            
            titlePaint.color = textColor
            datePaint.color = Color.argb((0.8f * 255).toInt(), Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            ddayPaint.color = textColor
            notesPaint.color = Color.argb((0.7f * 255).toInt(), Color.red(textColor), Color.green(textColor), Color.blue(textColor))

            // Background Drawing
            var backgroundDrawn = false
            if (includeBackgroundPhotos && event.backgroundType == "IMAGE" && !event.localImageUri.isNullOrEmpty()) {
                val pickedBitmap = loadBitmapFromUri(context, event.localImageUri)
                if (pickedBitmap != null) {
                    canvas.save()
                    // Clip round rect
                    val clipPath = Path().apply {
                        addRoundRect(cardRect, 24f, 24f, Path.Direction.CW)
                    }
                    canvas.clipPath(clipPath)
                    
                    drawBitmapCenterCrop(canvas, pickedBitmap, cardRect, cardPaint)
                    
                    // Draw semi-transparent color overlay filter
                    val filterColorHex = event.colorFilterHex?.takeIf { it.startsWith("#") } ?: "#000000"
                    val filterColor = try { Color.parseColor(filterColorHex) } catch (e: Exception) { Color.BLACK }
                    val overlayPaint = Paint().apply {
                        color = Color.argb((event.colorFilterOpacity * 255).toInt(), Color.red(filterColor), Color.green(filterColor), Color.blue(filterColor))
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(cardRect, overlayPaint)
                    canvas.restore()
                    backgroundDrawn = true
                }
            }

            if (!backgroundDrawn) {
                if (event.backgroundType == "GRADIENT" && !event.gradientColorsHex.isNullOrEmpty()) {
                    val split = event.gradientColorsHex.split(",")
                    val color1 = try { Color.parseColor(split[0]) } catch (e: Exception) { Color.parseColor("#3B82F6") }
                    val color2 = try { Color.parseColor(split[1]) } catch (e: Exception) { Color.parseColor("#111827") }
                    
                    val gradientShader = LinearGradient(
                        cardRect.left, cardRect.top, cardRect.right, cardRect.bottom,
                        color1, color2, Shader.TileMode.CLAMP
                    )
                    cardPaint.shader = gradientShader
                    canvas.drawRoundRect(cardRect, 24f, 24f, cardPaint)
                    cardPaint.shader = null
                } else {
                    // Solid Color
                    val themeColor = try {
                        Color.parseColor(event.themeColorHex)
                    } catch (e: Exception) {
                        Color.parseColor("#3B82F6")
                    }
                    cardPaint.color = themeColor
                    canvas.drawRoundRect(cardRect, 24f, 24f, cardPaint)
                }
            }

            // Draw Repeating Effects
            event.backgroundEffect?.let { effect ->
                if (effect != "NONE") {
                    canvas.save()
                    val clipPath = Path().apply {
                        addRoundRect(cardRect, 24f, 24f, Path.Direction.CW)
                    }
                    canvas.clipPath(clipPath)
                    drawRepeatingEffectOnCanvas(canvas, cardRect, effect)
                    canvas.restore()
                }
            }

            // Draw Sticker Emoji on the right side if selected
            if (!event.stickerId.isNullOrEmpty()) {
                val emojiText = when (event.stickerId) {
                    "sticker_love" -> "❤️"
                    "sticker_done" -> "🏆"
                    "sticker_smiley" -> "😊"
                    "sticker_star" -> "⭐"
                    "sticker_party" -> "🎉"
                    else -> ""
                }
                if (emojiText.isNotEmpty()) {
                    val emojiPaint = Paint().apply {
                        isAntiAlias = true
                        textSize = 48f
                    }
                    canvas.drawText(
                        emojiText,
                        cardRect.right - 140f,
                        cardRect.top + 70f,
                        emojiPaint
                    )
                }
            }

            // Calculations
            val ddayText = DDayCalculator.calculateEventDDay(event, settings)

            var displayTitle = event.title
            if (displayTitle.length > 22) {
                displayTitle = displayTitle.take(20) + "..."
            }

            // Title
            canvas.drawText(
                displayTitle,
                cardRect.left + 40f,
                cardRect.top + (currentCardHeight * 0.35f),
                titlePaint
            )

            // Date line
            val timeLabel = if (event.targetTime.isNullOrEmpty()) "" else " at ${event.targetTime}"
            canvas.drawText(
                "${event.targetDate}$timeLabel",
                cardRect.left + 40f,
                cardRect.top + (currentCardHeight * 0.58f),
                datePaint
            )

            // Notes description if exists
            if (!event.eventNote.isNullOrEmpty()) {
                var displayNote = event.eventNote
                if (displayNote.length > 35) {
                    displayNote = displayNote.take(32) + "..."
                }
                canvas.drawText(
                    displayNote,
                    cardRect.left + 40f,
                    cardRect.bottom - (currentCardHeight * 0.15f),
                    notesPaint
                )
            }

            // D-day display
            canvas.drawText(
                ddayText,
                cardRect.right - 40f,
                cardRect.centerY() + 18f,
                ddayPaint
            )

            currentY += currentCardHeight + gap
        }

        return try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "ddaycount_shared_events.png")
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareEvents(
        context: Context,
        events: List<Event>,
        settings: CalculationSettings,
        includeBackgroundPhotos: Boolean = true
    ) {
        val file = generateEventSharingImage(context, events, settings, includeBackgroundPhotos) ?: return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserIntent = Intent.createChooser(intent, "Share D-Day Events").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }

    private fun loadBitmapFromUri(context: Context, uriStr: String): Bitmap? {
        return try {
            val uri = Uri.parse(uriStr)
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawBitmapCenterCrop(canvas: Canvas, bitmap: Bitmap, rect: RectF, paint: Paint) {
        val srcWidth = bitmap.width
        val srcHeight = bitmap.height
        val dstWidth = rect.width()
        val dstHeight = rect.height()

        val srcRatio = srcWidth.toFloat() / srcHeight
        val dstRatio = dstWidth / dstHeight

        val srcRect = if (srcRatio > dstRatio) {
            val width = (srcHeight * dstRatio).toInt()
            val offset = (srcWidth - width) / 2
            Rect(offset, 0, offset + width, srcHeight)
        } else {
            val height = (srcWidth / dstRatio).toInt()
            val offset = (srcHeight - height) / 2
            Rect(0, offset, srcWidth, offset + height)
        }
        canvas.drawBitmap(bitmap, srcRect, rect, paint)
    }

    private fun drawRepeatingEffectOnCanvas(canvas: Canvas, rect: RectF, effect: String) {
        val effectPaint = Paint().apply {
            isAntiAlias = true
            color = Color.argb(20, 255, 255, 255)
            style = Paint.Style.FILL
        }

        val stepX = 80f
        val stepY = 80f
        var y = rect.top + 20f
        while (y < rect.bottom) {
            var x = rect.left + 20f
            while (x < rect.right) {
                when (effect.uppercase()) {
                    "HEARTS" -> {
                        val path = Path().apply {
                            moveTo(x, y - 5f)
                            cubicTo(x - 10f, y - 15f, x - 20f, y - 5f, x - 20f, y + 10f)
                            cubicTo(x - 20f, y + 25f, x, y + 35f, x, y + 40f)
                            cubicTo(x, y + 35f, x + 20f, y + 25f, x + 20f, y + 10f)
                            cubicTo(x + 20f, y - 5f, x + 10f, y - 15f, x, y - 5f)
                            close()
                        }
                        canvas.drawPath(path, effectPaint)
                    }
                    "STARS" -> {
                        val path = Path().apply {
                            moveTo(x, y - 10f)
                            lineTo(x + 3f, y - 3f)
                            lineTo(x + 10f, y - 3f)
                            lineTo(x + 5f, y + 2f)
                            lineTo(x + 8f, y + 10f)
                            lineTo(x, y + 5f)
                            lineTo(x - 8f, y + 10f)
                            lineTo(x - 5f, y + 2f)
                            lineTo(x - 10f, y - 3f)
                            lineTo(x - 3f, y - 3f)
                            close()
                        }
                        canvas.drawPath(path, effectPaint)
                    }
                    "CHERRIES" -> {
                        val cherryPaint = Paint().apply {
                            isAntiAlias = true
                            color = Color.argb(15, 255, 255, 255)
                        }
                        canvas.drawCircle(x - 6f, y + 6f, 5f, cherryPaint)
                        canvas.drawCircle(x + 6f, y + 8f, 5f, cherryPaint)
                    }
                    "SPARKLES" -> {
                        val path = Path().apply {
                            moveTo(x, y - 8f)
                            quadTo(x, y, x + 8f, y)
                            quadTo(x, y, x, y + 8f)
                            quadTo(x, y, x - 8f, y)
                            quadTo(x, y, x, y - 8f)
                            close()
                        }
                        canvas.drawPath(path, effectPaint)
                    }
                }
                x += stepX
            }
            y += stepY
        }
    }
}
