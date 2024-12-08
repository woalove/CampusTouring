package com.example.campustouring;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.campustouring.databinding.FragmentLocationInputBinding;
import com.google.android.gms.maps.model.LatLng;

import java.util.UUID;

public class LocationInputFragment extends Fragment {
    private FragmentLocationInputBinding binding;
    private EditText editNewTitleText;
    private EditText editNewSubTitleText;
    private EditText editNewDescriptionText;
    private LatLng locationCoordinates;
    private Button saveButton;
    private CustomMarkerContract markerContract;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLocationInputBinding.inflate(inflater, container, false);

        editNewTitleText = binding.editNewTitleText;
        editNewSubTitleText = binding.editNewSubTitleText;
        editNewDescriptionText = binding.editNewDescriptionText;
        saveButton = binding.saveButton;

        Bundle args = getArguments();
        if (args != null) {
            locationCoordinates = args.getParcelable("locationCoordinates");
        }

        markerContract = new CustomMarkerContract(getContext());

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get user input from the form
                String name = editNewTitleText.getText().toString();
                String shortName = editNewSubTitleText.getText().toString();
                String description = editNewDescriptionText.getText().toString();
                double latitude = locationCoordinates.latitude;
                double longitude = locationCoordinates.longitude;

                // Save location details to the database
                CustomMarkerContract.MarkerEntryObj newMarker = new CustomMarkerContract.MarkerEntryObj(
                        name, shortName, description, latitude, longitude, 0
                );
                markerContract.saveNewMarker(newMarker);
                Toast.makeText(requireContext(), "Location point created successfully!", Toast.LENGTH_SHORT).show();

                // Navigate back to MapFragment
                getParentFragmentManager().popBackStack();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
