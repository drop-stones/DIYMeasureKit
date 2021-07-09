package io.github.drop_stones.diymeasurekit

import android.os.Bundle
import kotlin.math.sqrt

class DirectDistanceMeasure : DistanceMeasure() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_direct_distance)
        setTitle(R.string.direct_distance_measure)
        super.onCreate(savedInstanceState)
    }

    override fun calculateDistance() {
        distance = sqrt(x*x + y*y + z*z)
        //printDistance()
    }
}