package com.example.yololitertobjectdetection.tracking

import kotlin.math.min

class HungarianAlgorithm {
    fun match(costMatrix: Array<FloatArray>): IntArray {
        val numRows = costMatrix.size
        val numCols = if (costMatrix.isNotEmpty()) costMatrix[0].size else 0

        if (numRows == 0 || numCols == 0) return IntArray(0) { -1 }

        val numAssignments = min(numRows, numCols)
        val assignments = IntArray(numRows) { -1 }
        val assignedCols = BooleanArray(numCols)

        for (row in 0 until numRows) {
            var minCost = Float.MAX_VALUE
            var minCol = -1

            for (col in 0 until numCols) {
                if (!assignedCols[col] && costMatrix[row][col] < minCost) {
                    minCost = costMatrix[row][col]
                    minCol = col
                }
            }

            if (minCol != -1) {
                assignments[row] = minCol
                assignedCols[minCol] = true
            }
        }

        return assignments
    }
}
