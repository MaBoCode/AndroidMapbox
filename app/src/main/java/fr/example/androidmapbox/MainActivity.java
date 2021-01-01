package fr.example.androidmapbox;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.mapbox.mapboxsdk.Mapbox;

import fr.example.androidmapbox.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    protected ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(getApplicationContext(), MapboxMapView.getMapboxAccessToken(this));

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}