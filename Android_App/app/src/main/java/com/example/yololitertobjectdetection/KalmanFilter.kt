package com.example.yololitertobjectdetection

import kotlin.math.pow

class ObjectKalmanFilter {
    private val state = FloatArray(4) // [x, y, dx, dy]
    private val covariance = Array(4) { FloatArray(4) }
    private val transitionMatrix = arrayOf(
        floatArrayOf(1f, 0f, 1f, 0f),
        floatArrayOf(0f, 1f, 0f, 1f),
        floatArrayOf(0f, 0f, 1f, 0f),
        floatArrayOf(0f, 0f, 0f, 1f)
    )

    private val measurementMatrix = arrayOf(
        floatArrayOf(1f, 0f, 0f, 0f),
        floatArrayOf(0f, 1f, 0f, 0f)
    )

    private val processNoise = 1e-4f
    private val measurementNoise = 1e-1f

    init {
        for (i in 0..3) covariance[i][i] = 1f // Initializing covariance as identity
    }

    fun predict(): FloatArray {
        val predictedState = multiplyMatrix(transitionMatrix, state)
        state.indices.forEach { state[it] = predictedState[it] }
        return floatArrayOf(state[0], state[1]) // Return predicted (x, y)
    }

    fun correct(measurement: FloatArray) {
        val gain = computeKalmanGain()
        val innovation = floatArrayOf(measurement[0] - state[0], measurement[1] - state[1])

        for (i in state.indices) {
            state[i] += gain[i] * innovation[i % 2]
        }
    }

    private fun computeKalmanGain(): FloatArray {
        return floatArrayOf(0.5f, 0.5f, 0.1f, 0.1f) // Simple approximation
    }

    private fun multiplyMatrix(matrix: Array<FloatArray>, vector: FloatArray): FloatArray {
        return FloatArray(matrix.size) { i -> matrix[i].indices.sumOf { matrix[i][it] * vector[it].toDouble() }.toFloat() }

    }
}
