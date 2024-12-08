package com.example.campustouring;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.campustouring.databinding.FragmentMarkerInfoBinding;
import java.util.HashMap;


public class MarkerInfoFragment extends Fragment {

    private FragmentMarkerInfoBinding binding;
    private Button editButton;
    private Button deleteButton;
    private boolean isEditMode = false;
    private int locationIndex;
    private CustomMarkerContract markerContract;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMarkerInfoBinding.inflate(inflater, container, false);
        editButton = binding.editButton;
        deleteButton = binding.deleteButton;
        updateButtonState();

        markerContract = new CustomMarkerContract(getContext());

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEditMode) {
                    updateMarkerInfoInDatabase();
                }
                isEditMode = !isEditMode;
                updateButtonState();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteMarkerFromDatabase(locationIndex);
                getParentFragmentManager().popBackStack();
            }
        });

        return binding.getRoot();
    }

    private void updateButtonState() {
        if (isEditMode) {
            editButton.setText(R.string.saveText);
            editButton.setBackgroundColor(Color.parseColor("#005035"));

            binding.editTitleText.setText(binding.titleTextView.getText().toString());
            binding.editTitleText.setVisibility(View.VISIBLE);
            binding.editSubTitleText.setText(binding.subTitleTextView.getText().toString());
            binding.editSubTitleText.setVisibility(View.VISIBLE);
            binding.editDescriptionText.setText(binding.descriptionTextView.getText().toString());
            binding.editDescriptionText.setVisibility(View.VISIBLE);
            binding.scrollEditDescriptionText.setVisibility(View.VISIBLE);

            binding.titleTextView.setVisibility(View.INVISIBLE);
            binding.subTitleTextView.setVisibility(View.INVISIBLE);
            binding.descriptionTextView.setVisibility(View.INVISIBLE);
            binding.scrollDescriptionTextView.setVisibility(View.INVISIBLE);
        } else {
            editButton.setText(R.string.editText);
            editButton.setBackgroundColor(Color.parseColor("#A49665"));

            binding.editTitleText.setVisibility(View.INVISIBLE);
            binding.editSubTitleText.setVisibility(View.INVISIBLE);
            binding.editDescriptionText.setVisibility(View.INVISIBLE);
            binding.scrollEditDescriptionText.setVisibility(View.VISIBLE);

            binding.titleTextView.setVisibility(View.VISIBLE);
            binding.subTitleTextView.setVisibility(View.VISIBLE);
            binding.descriptionTextView.setVisibility(View.VISIBLE);
            binding.scrollDescriptionTextView.setVisibility(View.VISIBLE);
        }
    }

    private void updateMarkerInfoInDatabase() {
        String title = binding.editTitleText.getText().toString();
        String subtitle = binding.editSubTitleText.getText().toString();
        String description = binding.editDescriptionText.getText().toString();

        // Retrieve existing marker information
        CustomMarkerContract.MarkerEntryObj existingMarker = markerContract.readSingleFromDb(locationIndex);

        // Ensure existing latitude and longitude are preserved
        // Create updated marker object
        CustomMarkerContract.MarkerEntryObj updatedMarker = new CustomMarkerContract.MarkerEntryObj(
                title,
                subtitle,
                description,
                existingMarker.latitude,
                existingMarker.longitude,
                existingMarker.isDefaultMarker
        );
        updatedMarker._id = existingMarker._id;

        // Update marker information in the database
        markerContract.updateMarker(updatedMarker);

        // Update UI with the new information
        binding.titleTextView.setText(title);
        binding.subTitleTextView.setText(subtitle);
        binding.descriptionTextView.setText(description);

        Toast.makeText(getContext(), "Marker info updated successfully!", Toast.LENGTH_SHORT).show();
    }

    private void deleteMarkerFromDatabase(int localIndex) {
        markerContract.deleteOneFromDb(localIndex);
        Toast.makeText(getContext(), "Marker deleted successfully!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Retrieve marker details from arguments
        Bundle args = getArguments();
        if (args != null) {
            String snippet = args.getString("snippet", "");
            locationIndex = Integer.parseInt(snippet);
            CustomMarkerContract.MarkerEntryObj marker = markerContract.readSingleFromDb(locationIndex);

            if (!(marker == null)) {
                // Print the contents of the marker HashMap
                Log.d("Marker HashMap", "Marker HashMap: " + marker);

                binding.titleTextView.setText(marker.name);
                binding.subTitleTextView.setText(marker.shortName);
                binding.descriptionTextView.setText(marker.link);

                int isDefaultMarker = marker.isDefaultMarker;
                if (isDefaultMarker == 0) {
                    editButton.setVisibility(View.VISIBLE);
                    deleteButton.setVisibility(View.VISIBLE);
                }
            } else {
                for (CustomMarkerContract.MarkerEntryObj entry : ((MainActivity) getActivity()).masterList) {
                    if (entry._id == locationIndex) {
                        binding.titleTextView.setText(entry.name);
                        binding.subTitleTextView.setText(entry.shortName);
                        binding.descriptionTextView.setText(entry.link);

                        int isDefaultMarker = entry.isDefaultMarker;
                        if (isDefaultMarker == 0) {
                            editButton.setVisibility(View.VISIBLE);
                            deleteButton.setVisibility(View.VISIBLE);
                        }

                    }

                }
            }
        }
    }
}

