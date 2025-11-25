package com.example.ispk_projektas;

import android.Manifest;
import android.content.DialogInterface; // ADDED
import android.content.Intent; // ADDED
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView; // ADDED
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog; // ADDED
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth; // ADDED
import com.google.maps.android.data.Feature;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonPolygon;
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private GoogleMap mMap;
    private GeoJsonLayer countiesLayer;
    private GeoJsonFeature selectedFeature;
    private Polygon selectedPolygon;
    private Button logoutButton;
    private FirebaseAuth auth;
    private FloatingActionButton btnF;
    private FloatingActionButton userInfoFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // NOTE: activity_map XML needs adjustment to include TextView
        setContentView(R.layout.activity_map);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        String role = getIntent().getStringExtra("role");
        String nickname = getIntent().getStringExtra("nickname");
        String email = getIntent().getStringExtra("email");

        // --- DISPLAY USER INFO ---
        userInfoFab = findViewById(R.id.userInfoFab);
//        if (userInfoTextView != null) {
//            userInfoTextView.setText("Logged in as: " + nickname + " (" + role + ")");
//        }

        userInfoFab.setOnClickListener(v -> {
            Intent intent = new Intent(MapActivity.this, ProfileActivity.class);

            // OPTIONAL — pass data if you want to show nickname, role, email
            intent.putExtra("nickname", nickname);
            intent.putExtra("role", role);

            startActivity(intent);
        });
        // Initial toast removed for a persistent display
        logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> performLogout());

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        btnF = findViewById(R.id.btn_f);

        btnF.setOnClickListener(v -> openForum());

    }

    public void openForum()
    {
        Intent intent = new Intent(MapActivity.this, Forum.class);
        startActivity(intent);

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        applyMapStyleBasedOnSystemTheme();

        // Location permission (optional, like in your MainActivity)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Center on Lithuania
        LatLng lithuaniaCenter = new LatLng(55.1694, 23.8813);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lithuaniaCenter, 7));

        // Load and paint polygons like in your PKP MainActivity
        loadAndStyleCounties();
    }

    // --- LOGOUT AND BACK PRESS LOGIC ---

    /**
     * Override the back button press to prompt for logout.
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        showLogoutConfirmationDialog();
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out and return to the login screen?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }


    private void performLogout() {
        auth.signOut(); // Firebase sign out
        Toast.makeText(this, "Successfully logged out.", Toast.LENGTH_SHORT).show();

        // Redirect to LoginActivity and clear the back stack
        Intent intent = new Intent(MapActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // --- EXISTING METHODS BELOW ---

    private void applyMapStyleBasedOnSystemTheme() {
        int currentNightMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        boolean isNightModeActive = currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private void loadAndStyleCounties() {
        try {
            countiesLayer = new GeoJsonLayer(mMap, R.raw.counties, getApplicationContext());

            // 1) give each feature a default style (like in MainActivity)
            for (Feature feature : countiesLayer.getFeatures()) {
                if (feature instanceof GeoJsonFeature) {
                    GeoJsonFeature geoJsonFeature = (GeoJsonFeature) feature;
                    GeoJsonPolygonStyle polygonStyle = new GeoJsonPolygonStyle();
                    polygonStyle.setStrokeColor(0xFF0000FF);     // blue outline
                    polygonStyle.setStrokeWidth(2f);
                    polygonStyle.setFillColor(0x5500FF00);       // semi-transparent green
                    geoJsonFeature.setPolygonStyle(polygonStyle);
                }
            }

            // 2) click listener → highlight polygon using GoogleMap Polygon (same idea)
            countiesLayer.setOnFeatureClickListener(feature -> {
                if (feature instanceof GeoJsonFeature) {
                    GeoJsonFeature geoJsonFeature = (GeoJsonFeature) feature;
                    selectedFeature = geoJsonFeature;

                    String countyName =
                            geoJsonFeature.getProperty("name:lt") != null ?
                                    geoJsonFeature.getProperty("name:lt") :
                                    geoJsonFeature.getProperty("name") != null ?
                                            geoJsonFeature.getProperty("name") :
                                            "Unknown county";

                    // remove previous highlight
                    if (selectedPolygon != null) {
                        selectedPolygon.remove();
                        selectedPolygon = null;
                    }

                    if (geoJsonFeature.getGeometry() instanceof GeoJsonPolygon) {
                        GeoJsonPolygon polygon =
                                (GeoJsonPolygon) geoJsonFeature.getGeometry();
                        List<LatLng> outerBoundary = polygon.getCoordinates().get(0);

                        PolygonOptions polygonOptions = new PolygonOptions()
                                .addAll(outerBoundary)
                                .strokeColor(0xFF0000FF)
                                .strokeWidth(3f)
                                .fillColor(0x88006400);

                        selectedPolygon = mMap.addPolygon(polygonOptions);
                    }

                    Toast.makeText(
                            MapActivity.this,
                            "Selected: " + countyName,
                            Toast.LENGTH_SHORT
                    ).show();
                    Intent intent = new Intent(MapActivity.this, CountyNewsActivity.class);
                    intent.putExtra("countyName", countyName);
                    startActivity(intent);
                }
            });

            // 3) finally add the layer to the map
            countiesLayer.addLayerToMap();

        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error loading counties GeoJSON", e);
            Toast.makeText(this, "Error loading counties boundaries", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mMap != null && ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            }
        }
    }
}