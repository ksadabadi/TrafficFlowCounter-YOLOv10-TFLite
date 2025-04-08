package com.example.yololitertobjectdetection.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.example.yololitertobjectdetection.BoundingBox

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var detectedObjects = listOf<BoundingBox>()
    private val boxPaint = Paint()
    private val textPaint = Paint()
    private val linePaint = Paint()
    private val previousPositions = mutableMapOf<Int, Pair<Float, Float>>() // Stores (x1, y1) for each ID
    private val classCounts = mutableMapOf<String, Int>()

    private var count = 0
    private var countedObjects = mutableMapOf<Int, String>() // ✅ Stores only counted object IDs
    var onCountUpdated: ((Int) -> Unit)? = null

    private var countDirection: String = "TOP_TO_BOTTOM" // ✅ Default direction
    private var countLinePosition: Float = 0.5f // ✅ Default: Middle of screen

    init {
        initPaints()
        viewTreeObserver.addOnGlobalLayoutListener {
            if (width > 0 && height > 0) {
                postInvalidate() // Forces a redraw when dimensions are available
            }
        }
    }

    private fun initPaints() {
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = 6f

        textPaint.color = Color.WHITE
        textPaint.textSize = 42f
        textPaint.style = Paint.Style.FILL

        linePaint.color = Color.RED
        linePaint.strokeWidth = 6f
    }

    fun setResults(boundingBoxes: List<BoundingBox>) {
        detectedObjects = boundingBoxes
        invalidate()
    }

    fun setCountingDirection(direction: String, linePos: Float) {
        countDirection = direction
        countLinePosition = linePos
        Log.d("DIRECTION_DEBUG", "Set direction=$countDirection, linePos=${countLinePosition * 100}%")
        invalidate()
    }
    fun getCount(): Int {
        return count
    }
    fun getDetailedCounts(): Map<String, Int> {
        return classCounts.also { detailedCounts ->
            Log.d("DETAILED_COUNTS", "Class breakdown: $detailedCounts")
        }
    }

    fun clear() {
        detectedObjects = listOf()
        countedObjects.clear()

        invalidate()
    }

    override fun draw(canvas: Canvas) {
        if (width ==0 || height == 0)return
        super.draw(canvas)

        // ✅ Draw only the selected counting line
        drawCountingLine(canvas)

        for (box in detectedObjects) {
            boxPaint.color = Color.RED

            val left = box.x1 * width
            val top = box.y1 * height
            val right = box.x2 * width
            val bottom = box.y2 * height

            // ✅ Draw bounding box
            canvas.drawRoundRect(left, top, right, bottom, 16f, 16f, boxPaint)

            val label = "${box.clsName} ${String.format("%.2f", box.cnf)}"
            canvas.drawText(label, left, top - 10, textPaint)

            // ✅ Only count objects in the selected direction
            if (box.id != null) {
                Log.d("POSITION_DEBUG", "Object ${box.id}: Prev=${previousPositions[box.id]}, Curr=(${box.x1 * width}, ${box.y1 * height})")
                val hasCrossed = hasCrossedLine(box)
                val alreadyCounted = countedObjects.contains(box.id)

                Log.d("COUNT_CHECK", "Object ${box.id}: hasCrossed=$hasCrossed, alreadyCounted=$alreadyCounted")

                if (hasCrossed && !countedObjects.containsKey(box.id)) {
                    countedObjects[box.id!!] = box.clsName ?: "Unknown"
                    count++
                    val className = box.clsName ?: "Unknown"
                    classCounts[className] = classCounts.getOrDefault(className, 0) + 1

                    Log.d("COUNT_UPDATE", "Object ${box.id} counted! New count: $count")
                    Log.d("CLASS_COUNT_UPDATE", "Class ${className} count: ${classCounts[className]}")
                    Log.d("COUNT_UPDATE", "Object ${box.id} counted! New count: $count")
                }
            }

        }

        // ✅ Show only the selected count
        canvas.drawText("Count: $count", 50f, 100f, textPaint)
        android.util.Log.d("COUNT_DEBUG", "Displayed Count: $count")
    }

    private fun drawCountingLine(canvas: Canvas) {
        if (width == 0 || height == 0) return
        when (countDirection) {
            "LEFT_TO_RIGHT", "RIGHT_TO_LEFT" -> {
                val x = width * countLinePosition
                canvas.drawLine(x, 0f, x, height.toFloat(), linePaint)
            }
            "TOP_TO_BOTTOM", "BOTTOM_TO_TOP" -> {
                val y = height * countLinePosition
                canvas.drawLine(0f, y, width.toFloat(), y, linePaint)
            }
        }
    }

    private fun hasCrossedLine(box: BoundingBox): Boolean {
        val previous = previousPositions[box.id]
        val current = Pair(box.x1 * width, box.y1 * height) // Convert to screen coordinates

        previousPositions[box.id!!] = current // Update position

        if (previous == null) {
            Log.d("CROSS_DEBUG", "Object ${box.id}: First detection, skipping")
            return false // If first detection, don’t count
        }

        val crossed = when (countDirection) {
            "LEFT_TO_RIGHT" -> previous.first < width * countLinePosition && current.first > width * countLinePosition
            "RIGHT_TO_LEFT" -> previous.first > width * countLinePosition && current.first < width * countLinePosition
            "TOP_TO_BOTTOM" -> previous.second < height * countLinePosition && current.second > height * countLinePosition
            "BOTTOM_TO_TOP" -> previous.second > height * countLinePosition && current.second < height * countLinePosition
            else -> false
        }

        if (crossed) {
            Log.d("CROSS_DEBUG", "Object ${box.id} crossed the line at ${countLinePosition * 100}%")
        } else {
            Log.d("CROSS_DEBUG", "Object ${box.id} did NOT cross. Prev: $previous, Curr: $current, Line: ${countLinePosition * 100}%")
        }

        return crossed
    }




}
