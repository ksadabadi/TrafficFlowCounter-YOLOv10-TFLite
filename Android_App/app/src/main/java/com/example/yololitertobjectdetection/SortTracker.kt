package com.example.yololitertobjectdetection.tracking

import com.example.yololitertobjectdetection.BoundingBox
import com.example.yololitertobjectdetection.ObjectKalmanFilter
import kotlin.math.abs

class SortTracker {
    private val trackers = mutableListOf<Pair<Int, ObjectKalmanFilter>>() // (ID, Kalman Filter)
    private var nextId = 1

    fun update(detections: List<BoundingBox>): List<BoundingBox> {
        val costMatrix = Array(trackers.size) { FloatArray(detections.size) }
        val hungarian = HungarianAlgorithm()

        for (i in trackers.indices) {
            val (_, tracker) = trackers[i]
            val predicted = tracker.predict()
            val predX = predicted[0]
            val predY = predicted[1]

            for (j in detections.indices) {
                val detection = detections[j]
                val centerX = (detection.x1 + detection.x2) / 2
                val centerY = (detection.y1 + detection.y2) / 2
                costMatrix[i][j] = abs(predX - centerX) + abs(predY - centerY)
            }
        }

        val assignments = hungarian.match(costMatrix)
        val trackedDetections = mutableListOf<BoundingBox>()
        val usedDetections = mutableSetOf<Int>()

        for (i in assignments.indices) {
            val assignedDetection = assignments[i]
            if (assignedDetection != -1) {
                val (id, tracker) = trackers[i]
                val detection = detections[assignedDetection]
                val measurement = floatArrayOf((detection.x1 + detection.x2) / 2, (detection.y1 + detection.y2) / 2)
                tracker.correct(measurement)

                trackedDetections.add(detection.copy(id = id))
                usedDetections.add(assignedDetection)
            }
        }

        detections.forEachIndexed { index, detection ->
            if (!usedDetections.contains(index)) {
                val newTracker = ObjectKalmanFilter()
                val measurement = floatArrayOf((detection.x1 + detection.x2) / 2, (detection.y1 + detection.y2) / 2)
                newTracker.correct(measurement)

                trackers.add(Pair(nextId, newTracker))
                trackedDetections.add(detection.copy(id = nextId))
                nextId++
            }
        }

        return trackedDetections
    }
}
