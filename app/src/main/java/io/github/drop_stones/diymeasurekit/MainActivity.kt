package io.github.drop_stones.diymeasurekit

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*
        val curveDistanceMeasure: Button = findViewById(R.id.curve_distance_measure)
        curveDistanceMeasure.setOnClickListener {
            val intent: Intent = Intent(this, CurveDistanceMeasure::class.java)
            startActivity(intent)
        }

        val directDistanceButton: Button = findViewById(R.id.direct_distance_measure)
        directDistanceButton.setOnClickListener {
            val intent: Intent = Intent(this, DirectDistanceMeasure::class.java)
            startActivity(intent)
        }
         */

        val distanceMeasure: Button = findViewById(R.id.distance_measure)
        distanceMeasure.setOnClickListener {
            val intent: Intent = Intent(this, CurveDistanceMeasure::class.java)
            startActivity(intent)
        }

        val elevationAngleButton: Button = findViewById(R.id.elevation_angle_measure)
        elevationAngleButton.setOnClickListener {
            val intent: Intent = Intent(this, ElevationAngleMeasure::class.java)
            startActivity(intent)
        }
    }
}