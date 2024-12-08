package com.example.campustouring;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.campustouring.databinding.FragmentMapBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;

import com.google.android.gms.maps.model.MapStyleOptions;


public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private FragmentMapBinding binding;
    private GoogleMap gMap;
    CustomMarkerContract contract;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    gMap.setMyLocationEnabled(true);
                }
            } else {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });

        binding = FragmentMapBinding.inflate(inflater, container, false);
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getChildFragmentManager().beginTransaction().replace(R.id.id_map, mapFragment).commit();
        mapFragment.getMapAsync(this);

        return binding.getRoot();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        LatLng location = new LatLng(35.3071, -80.7352);
        LatLng southWest = new LatLng(35.3040, -80.7400);
        LatLng northEast = new LatLng(35.3100, -80.7300);
        LatLngBounds bounds = new LatLngBounds(southWest, northEast);
        googleMap.setLatLngBoundsForCameraTarget(bounds);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 16));
        googleMap.clear();
        contract = new CustomMarkerContract(this.getContext());

        // Place points from Database
        placePoints(googleMap);
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // Pass the clicked location coordinates to the LocationInputFragment
                Bundle args = new Bundle();
                args.putParcelable("locationCoordinates", latLng);
                Navigation.findNavController(requireView()).navigate(
                        R.id.action_MapFragment_to_LocationInputFragment,
                        args
                );
            }
        });

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.empty_map_style));
        googleMap.setInfoWindowAdapter(new CustomInfoWindow(getContext()));
        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                marker.hideInfoWindow();
            }
        });
        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

            @Override
            public void onInfoWindowClick(@NonNull Marker marker) {
                Bundle args = new Bundle();
                args.putString("title", marker.getTitle());
                args.putString("snippet", marker.getSnippet());
                Log.d("Marker from Contract", "Title: " + marker.getTitle() +
                        ", Snippet: " + marker.getSnippet());
                Navigation.findNavController(requireView()).navigate(
                        R.id.action_MapFragment_to_MarkerInfoFragment,
                        args
                );
            }
        });
        this.gMap = googleMap;
    }

    public class CustomInfoWindow implements GoogleMap.InfoWindowAdapter {

        View CustomView;

        public CustomInfoWindow(Context context) {
            CustomView = LayoutInflater.from(context).inflate(R.layout.custom_marker_view, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            TextView customTitle = CustomView.findViewById(R.id.customTitleTextView);
            customTitle.setText(marker.getTitle());
            return CustomView;
        }

        @Override
        public View getInfoContents(Marker marker) {
            return null;
        }
    }

    private void printMarkersFromContract() {
        List<CustomMarkerContract.MarkerEntryObj> markerList = contract.readAllFromDb();
        for (CustomMarkerContract.MarkerEntryObj marker : markerList) {
            Log.d("Marker from Contract", "Name: " + marker.name +
                    ", Latitude: " + marker.latitude +
                    ", Longitude: " + marker.longitude);
        }
    }

    public void placePoints(GoogleMap googleMap) {
        List<CustomMarkerContract.MarkerEntryObj> customList = contract.readAllFromDb();
        CustomMarkerContract.MarkerEntryObj entry = contract.readSingleFromDb(1);
        // Clear existing markers
        googleMap.clear();

        for (CustomMarkerContract.MarkerEntryObj row : customList) {
            String name = row.name;
            String snippet = String.valueOf(row._id);
            double lat = row.latitude;
            double lng = row.longitude;

            LatLng coords = new LatLng(lat, lng);
            googleMap.addMarker(new MarkerOptions()
                    .position(coords)
                    .title(name)
                    .snippet(snippet));
        }
        for (CustomMarkerContract.MarkerEntryObj row : ((MainActivity)this.getActivity()).masterList) {
            String name = row.name;
            String snippet = String.valueOf(row._id);
            double lat = row.latitude;
            double lng = row.longitude;
            LatLng coords = new LatLng(lat, lng);

            googleMap.addMarker(new MarkerOptions()
                    .position(coords)
                    .title(name)
                    .snippet(snippet));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}