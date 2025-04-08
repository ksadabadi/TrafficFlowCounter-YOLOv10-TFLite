package com.example.yololitertobjectdetection.ui

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.yololitertobjectdetection.BoundingBox
import com.example.yololitertobjectdetection.Constants.LABELS_PATH
import com.example.yololitertobjectdetection.Constants.MODEL_PATH
import com.example.yololitertobjectdetection.Detector
import com.example.yololitertobjectdetection.Direction
import com.example.yololitertobjectdetection.tracking.SortTracker
import com.example.yololitertobjectdetection.databinding.FragmentCameraBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.random.Random
import android.location.Location
import com.example.yololitertobjectdetection.HomeActivity
import com.example.yololitertobjectdetection.R
import com.google.android.gms.location.*

class CameraFragment : Fragment(), Detector.DetectorListener {
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private var isCounting = false
    private var startTime: String = ""
    private var startDate: String = ""
    private var stopTime: String = ""
    private var location: String = "Unknown"
    private var linePosition: Float = 0f
    private var isVerticalLine: Boolean = true
    private var objectCount = 0
    private var ttcount = 0
    private lateinit var selectedDirection: String
    private val classCounts = mutableMapOf<String, Int>()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val tracker = SortTracker()
    private val objectPreviousPositions = mutableMapOf<Int, Pair<Float, Float>>()
    private val colorMap = mutableMapOf<Int, Int>() // Stores object ID → Color mapping
    private val countedObjects = mutableMapOf<Int, String>()
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var detector: Detector? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var countTextView: TextView


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        getLastKnownLocation()

        selectedDirection = arguments?.getString("DIRECTION") ?: "LEFT_TO_RIGHT"

        binding.viewFinder.viewTreeObserver.addOnGlobalLayoutListener {
            when (selectedDirection) {
                "LEFT_TO_RIGHT", "RIGHT_TO_LEFT" -> {
                    isVerticalLine = true
                    linePosition = 0.5f
                }
                "TOP_TO_BOTTOM", "BOTTOM_TO_TOP" -> {
                    isVerticalLine = false
                    linePosition = 0.5f
                }
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        detector = Detector(requireContext(), MODEL_PATH, LABELS_PATH, this, { message ->
            toast(message)
        }, tracker, Direction.valueOf(selectedDirection))

        binding.viewFinder.post {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                ActivityCompat.requestPermissions(requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            }
        }
        binding.btnStartCounting.setOnClickListener {
            isCounting = true
            objectCount = 0 // Reset count
            countedObjects.clear() // Clear counted objects
            val now = java.util.Calendar.getInstance()
            val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            startTime = sdf.format(now.time)
            val dateFormat = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
            startDate = dateFormat.format(now.time)
            toast("Counting Started")
        }

        // 🎯 Stop Counting Button
        binding.btnStopCounting.setOnClickListener {
            isCounting = false
            val now = java.util.Calendar.getInstance()
            val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            stopTime = sdf.format(now.time)
            showCountPopup()
        }

    }
    private fun getLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                location = if (loc != null) {
                    "${loc.latitude}, ${loc.longitude}"
                } else {
                    "Unknown"
                }
                Log.d("LOCATION_DEBUG", "Location: $location")
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                Log.d(TAG, "Camera provider initialized")
                bindCameraUseCases()
            } catch (exc: Exception) {
                Log.e(TAG, "Camera initialization failed: ${exc.message}", exc)
                toast("Camera initialization failed!")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        val binding = _binding ?: return
        val cameraProvider = cameraProvider ?: return

        val rotation = binding.viewFinder.display.rotation

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            val bitmapBuffer = Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )

            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
            imageProxy.close()

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                matrix, true
            )

            detector?.detect(rotatedBitmap)
        }

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            Log.d(TAG, "Camera use cases bound successfully.")
        } catch (exc: Exception) {
            Log.e(TAG, "Error binding use cases", exc)
            toast("Camera initialization failed!")
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long, count: Int) {
        Log.d(TAG, "Detected objects: ${boundingBoxes.size}, Count: $count")
        requireActivity().runOnUiThread {
            val trackedObjects = tracker.update(boundingBoxes)
            Log.d("SORT_DEBUG", "Tracked Objects: ${trackedObjects.map { it.id }}")

            trackedObjects.forEach { bbox ->
                val objectId = bbox.id ?: return@forEach
                val className = bbox.clsName ?: "Unknown"
                val currentPosition = Pair((bbox.x1 + bbox.x2) / 2, (bbox.y1 + bbox.y2) / 2)
                Log.d("TRACKING_DEBUG", "Object ${bbox.id} at (${bbox.x1}, ${bbox.y1})")
                // Set all bounding boxes to red
                bbox.color = Color.RED

                val previousPosition = objectPreviousPositions[objectId]
                if (previousPosition != null && isCounting) {
                    val (prevX, prevY) = previousPosition
                    val (currX, currY) = currentPosition

                    when (selectedDirection) {
                        "LEFT_TO_RIGHT" -> if (prevX < linePosition && currX >= linePosition) incrementCount(objectId, className)
                        "RIGHT_TO_LEFT" -> if (prevX > linePosition && currX <= linePosition) incrementCount(objectId, className)
                        "TOP_TO_BOTTOM" -> if (prevY < linePosition && currY >= linePosition) incrementCount(objectId, className)
                        "BOTTOM_TO_TOP" -> if (prevY > linePosition && currY <= linePosition) incrementCount(objectId, className)
                    }

                }

                objectPreviousPositions[objectId] = currentPosition
            }


            binding.overlay.setResults(trackedObjects)
            binding.overlay.setCountingDirection(selectedDirection, linePosition)
            binding.overlay.invalidate()
        }
    }


    override fun onEmptyDetect() {
        requireActivity().runOnUiThread {
            binding.overlay.clear()
            binding.overlay.setCountingDirection(selectedDirection, linePosition)
            binding.overlay.invalidate()

        }
    }
    private fun showCountPopup() {
        val finalCount = objectCount
        val filename = "${startTime.replace(":", "-")}_${startDate}.txt"
        val fileContent = buildString {
            append("Location: $location\n")
            append("Date: $startDate\n")
            append("Start Time: $startTime\n")
            append("Stop Time: $stopTime\n\n")
            append("Total Counted Objects: $finalCount\n\n")
            append("Detailed Breakdown:\n")
            for ((cls, count) in classCounts) {
                append("$cls: $count\n")
            }
        }

        writeToFile(filename, fileContent)

        requireActivity().runOnUiThread {
            val dialogView = layoutInflater.inflate(R.layout.dialog_count_summary, null)
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create()

            val textViewSummary = dialogView.findViewById<TextView>(R.id.textViewSummary)
            val buttonOk = dialogView.findViewById<View>(R.id.buttonOk)

            textViewSummary.text = fileContent

            buttonOk.setOnClickListener {
                dialog.dismiss()
                val intent = Intent(requireContext(), HomeActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                requireActivity().finish()
            }

            dialog.show()
        }
    }



    private fun writeToFile(filename: String, content: String) {
        try {
            val file = File(requireContext().getExternalFilesDir(null), filename)
            file.writeText(content)

            Log.d("FILE_DEBUG", "File saved at: ${file.absolutePath}")

            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), "File saved at: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("FILE_ERROR", "Error saving file", e)
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), "Failed to save file", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun incrementCount(objectId: Int, className: String) {
        if (!countedObjects.contains(objectId)) {
            countedObjects[objectId] = className
            objectCount++
            Log.d("COUNT_UPDATE", "Counted Object $objectId ($className) → New Total: $objectCount")
            classCounts[className] = classCounts.getOrDefault(className, 0) + 1
            Log.d("COUNT_UPDATE", "Counted Object $objectId ($className) → New Total: $objectCount")

        }
    }

    private fun toast(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "Camera"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
