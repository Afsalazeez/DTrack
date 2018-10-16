package com.example.user.d_track;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.awt.font.TextAttribute;

public class ResultActivity extends AppCompatActivity {

    TextView distanceTravelledTextView;

    TextView timeElapsedTextView;

    TextView averageSpeedTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        distanceTravelledTextView = (TextView) findViewById(R.id.distance_travelled_text_view);

        timeElapsedTextView = (TextView) findViewById(R.id.time_taken_text_view);

        averageSpeedTextView = (TextView) findViewById(R.id.average_speed_text_view);

        Intent intent = getIntent();
        if (intent != null) {

            Bundle bundle = intent.getExtras();
            if (bundle != null) {

                String distanceTravelled = bundle.getString(MainActivity.DISTANCE_IN_METERS);
                String timeElapsed = bundle.getString(MainActivity.TIME_IN_MILLISECONDS);
                String averageSpeed = bundle.getString(MainActivity.AVERAGE_SPEED);

                distanceTravelledTextView.setText(new StringBuilder().append(distanceTravelled).append(" meter(s)").toString());

                timeElapsedTextView.setText(timeElapsed);

                averageSpeedTextView.setText(new StringBuilder().append(averageSpeed).append(" m/s").toString());
            }
        }
    }
}
