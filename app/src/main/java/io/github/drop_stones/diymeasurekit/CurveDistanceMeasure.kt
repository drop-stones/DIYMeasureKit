package io.github.drop_stones.diymeasurekit

import android.os.Bundle
import kotlin.math.sqrt

class CurveDistanceMeasure : DistanceMeasure() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_curve_distance)
        setTitle(R.string.curve_distance_measure)
        super.onCreate(savedInstanceState)
    }

    override fun calculateDistance() {
        distance += sqrt(dx*dx + dy*dy + dz*dz)
        //printDistance()
    }
}