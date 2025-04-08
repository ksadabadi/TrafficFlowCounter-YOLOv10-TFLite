package com.example.yololitertobjectdetection

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.example.yololitertobjectdetection.MetaData.extractNamesFromLabelFile
import com.example.yololitertobjectdetection.MetaData.extractNamesFromMetadata
import com.example.yololitertobjectdetection.tracking.SortTracker
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class Detector(
    private val context: Context,
    private val modelPath: String,
    private val labelPath: String?,
    private val detectorListener: DetectorListener,
    private val message: (String) -> Unit,
    private val tracker: SortTracker = SortTracker(),
    private var selectedDirection: Direction // ✅ Stores the selected direction
) {
    private var interpreter: Interpreter
    private var labels = mutableListOf<String>()

    private var tensorWidth = 0
    private var tensorHeight = 0
    private var numChannel = 0
    private var numElements = 0

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    private val previousPositions = mutableMapOf<Int, Pair<Float, Float>>()
    private var count = 0 // ✅ Only one count for the selected direction
    private var isVertical = false
    private var countingLinePosition = 0.5f

    init {
        val options = Interpreter.Options().apply {
            this.setNumThreads(4)
        }

        val model = FileUtil.loadMappedFile(context, modelPath)
        interpreter = Interpreter(model, options)

        labels.addAll(extractNamesFromMetadata(model))
        if (labels.isEmpty()) {
            if (labelPath == null) {
                message("Model does not contain metadata, provide LABELS_PATH in Constants.kt")
                labels.addAll(MetaData.TEMP_CLASSES)
            } else {
                labels.addAll(extractNamesFromLabelFile(context, labelPath))
            }
        }

        val inputShape = interpreter.getInputTensor(0)?.shape()
        val outputShape = interpreter.getOutputTensor(0)?.shape()

        if (inputShape != null) {
            tensorWidth = inputShape[1]
            tensorHeight = inputShape[2]

            if (inputShape[1] == 3) {
                tensorWidth = inputShape[2]
                tensorHeight = inputShape[3]
            }
        }

        if (outputShape != null) {
            numElements = outputShape[1]
            numChannel = outputShape[2]
        }
    }

    fun restart(isGpu: Boolean) {
        interpreter.close()

        val options = if (isGpu) {
            val compatList = CompatibilityList()
            Interpreter.Options().apply {
                if (compatList.isDelegateSupportedOnThisDevice) {
                    val delegateOptions = compatList.bestOptionsForThisDevice
                    this.addDelegate(GpuDelegate(delegateOptions))
                } else {
                    this.setNumThreads(4)
                }
            }
        } else {
            Interpreter.Options().apply {
                this.setNumThreads(4)
            }
        }

        val model = FileUtil.loadMappedFile(context, modelPath)
        interpreter = Interpreter(model, options)
    }

    fun close() {
        interpreter.close()
    }

    fun detect(frame: Bitmap) {
        if (tensorWidth == 0 || tensorHeight == 0 || numChannel == 0 || numElements == 0) return

        var inferenceTime = SystemClock.uptimeMillis()

        val resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false)

        val tensorImage = TensorImage(INPUT_IMAGE_TYPE)
        tensorImage.load(resizedBitmap)
        val processedImage = imageProcessor.process(tensorImage)
        val imageBuffer = processedImage.buffer

        val output = TensorBuffer.createFixedSize(intArrayOf(1, numChannel, numElements), OUTPUT_IMAGE_TYPE)
        interpreter.run(imageBuffer, output.buffer)

        val bestBoxes = bestBox(output.floatArray)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        if (bestBoxes.isEmpty()) {
            detectorListener.onEmptyDetect()
            return
        }

        bestBoxes.forEach { bbox ->
            val objectCenterX = (bbox.x1 + bbox.x2) / 2
            val objectCenterY = (bbox.y1 + bbox.y2) / 2
            val objectId = bbox.id ?: return@forEach

            previousPositions[objectId]?.let { previousCenter ->
                val previousCenterX = previousCenter.first
                val previousCenterY = previousCenter.second

                when (selectedDirection) {
                    Direction.LEFT_TO_RIGHT -> {
                        if (previousCenterX < countingLinePosition && objectCenterX >= countingLinePosition) {
                            count++
                        }
                    }
                    Direction.RIGHT_TO_LEFT -> {
                        if (previousCenterX > countingLinePosition && objectCenterX <= countingLinePosition) {
                            count++
                        }
                    }
                    Direction.TOP_TO_BOTTOM -> {
                        if (previousCenterY < countingLinePosition && objectCenterY >= countingLinePosition) {
                            count++
                        }
                    }
                    Direction.BOTTOM_TO_TOP -> {
                        if (previousCenterY > countingLinePosition && objectCenterY <= countingLinePosition) {
                            count++
                        }
                    }
                }
            }

            previousPositions[objectId] = Pair(objectCenterX, objectCenterY)
        }

        detectorListener.onDetect(bestBoxes, inferenceTime, count)
    }

    private fun bestBox(array: FloatArray): List<BoundingBox> {
        val validClasses = setOf("person", "bicycle", "car", "motorcycle", "bus", "truck")

        val boundingBoxes = mutableListOf<BoundingBox>()
        for (r in 0 until numElements) {
            val cnf = array[r * numChannel + 4]
            if (cnf > CONFIDENCE_THRESHOLD) {
                val x1 = array[r * numChannel]
                val y1 = array[r * numChannel + 1]
                val x2 = array[r * numChannel + 2]
                val y2 = array[r * numChannel + 3]
                val cls = array[r * numChannel + 5].toInt()
                val clsName = labels[cls]
                if (clsName in validClasses) {
                    boundingBoxes.add(
                        BoundingBox(
                            x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                            cnf = cnf, cls = cls, clsName = clsName
                        )
                    )

                }
            }
        }
        return boundingBoxes
    }

    fun getCount(): Int = count // ✅ Returns only the selected direction count

    fun setCountingLine(position: Float, vertical: Boolean, direction: Direction) {
        countingLinePosition = position
        isVertical = vertical
        selectedDirection = direction // ✅ Updates selected direction
        count = 0 // ✅ Resets count when changing direction
    }

    interface DetectorListener {
        fun onEmptyDetect()
        fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long, count: Int)
    }

    companion object {
        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
        private const val CONFIDENCE_THRESHOLD = 0.3F
    }
}

enum class Direction {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    TOP_TO_BOTTOM,
    BOTTOM_TO_TOP
}
