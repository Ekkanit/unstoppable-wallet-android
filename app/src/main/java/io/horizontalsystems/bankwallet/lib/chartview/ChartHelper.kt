package io.horizontalsystems.bankwallet.lib.chartview

import android.graphics.RectF
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartConfig
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartPoint

class ChartHelper(private val shape: RectF, private val config: ChartConfig) {

    fun setCoordinates(points: List<ChartPoint>) {
        val width = shape.width()
        val height = shape.height()
        val bottom = config.valueTop - (config.valueStep * 4)

        val startTimestamp = points.first().timestamp
        val endTimestamp = points.last().timestamp

        val deltaX = (endTimestamp - startTimestamp) / width
        val deltaY = height / (config.valueStep * 4)

        for (point in points) {
            point.x = (point.timestamp - startTimestamp) / deltaX
            point.y = height - deltaY * (point.value - bottom)
        }
    }

    companion object {
        fun convert(points: List<Float>, scaleMinutes: Int, lastTimestamp: Long): List<ChartPoint> {

            val scaleSecs = scaleMinutes * 60
            var timestamp = lastTimestamp

            val chartPoints = mutableListOf<ChartPoint>()

            for (i in (points.size - 1) downTo 0) {
                chartPoints.add(0, ChartPoint(points[i], timestamp))
                timestamp -= scaleSecs
            }

            return chartPoints
        }
    }
}
