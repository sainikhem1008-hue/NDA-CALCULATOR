package com.example.ndacalculator.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object PdfHelper {
	fun writeSimplePdf(file: File, title: String, lines: List<String>) {
		val document = PdfDocument()
		val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size approx at 72dpi
		val page = document.startPage(pageInfo)
		val canvas: Canvas = page.canvas

		val titlePaint = Paint().apply {
			isAntiAlias = true
			textSize = 18f
		}
		val bodyPaint = Paint().apply {
			isAntiAlias = true
			textSize = 14f
		}

		var y = 40f
		canvas.drawText(title, 40f, y, titlePaint)
		y += 24f
		lines.forEach { line ->
			canvas.drawText(line, 40f, y, bodyPaint)
			y += 20f
		}

		document.finishPage(page)
		FileOutputStream(file).use { out ->
			document.writeTo(out)
		}
		document.close()
	}
}