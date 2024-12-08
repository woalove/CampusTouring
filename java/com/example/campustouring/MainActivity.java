package com.example.campustouring;


import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.campustouring.databinding.ActivityMainBinding;
import com.opencsv.CSVReader;

import android.view.Menu;
import android.view.MenuItem;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.example.campustouring.CustomMarkerContract;


public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private boolean isARFragmentShown = false;

    public List<CustomMarkerContract.MarkerEntryObj> masterList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        // Load CSV files and store in the local variable
        loadCSVFiles();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(R.id.IntroductionFragment, R.id.MapFragment, R.id.ARFragment, R.id.MarkerInfoFragment).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setVisibility(View.GONE);

        // Add destination listener to observe navigation changes
        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController navController, @NonNull NavDestination navDestination, @Nullable Bundle bundle) {
                if (navDestination.getId() == R.id.MapFragment || navDestination.getId() == R.id.ARFragment) {
                    binding.fab.setVisibility(View.VISIBLE);
                } else {
                    binding.fab.setVisibility(View.GONE);
                }
            }
        });

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isARFragmentShown) {
                    navController.navigate(R.id.action_MapFragment_to_ARFragment);
                    isARFragmentShown = true;
                    binding.fab.setImageResource(android.R.drawable.ic_dialog_map);
                  } else {
                    navController.popBackStack();
                    isARFragmentShown = false;
                    binding.fab.setImageResource(android.R.drawable.ic_menu_camera);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.SettingsFragment);
            return true;
        }

        if (id == R.id.action_all) {
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.allMarkersFragment);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Load CSV files and store in the database
    private void loadCSVFiles() {
        masterList = new ArrayList<CustomMarkerContract.MarkerEntryObj>();
        // Loading All Default Points From CSV
        boolean firstLine = true;
        try (CSVReader reader = new CSVReader(new InputStreamReader(getResources().openRawResource(R.raw.master)), '~')) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                // Convert CSV data to MarkerEntryObj
                CustomMarkerContract.MarkerEntryObj marker = new CustomMarkerContract.MarkerEntryObj(
                        Long.parseLong(line[0]), line[1], line[2], line[3], Double.parseDouble(line[4]), Double.parseDouble(line[5]), Integer.parseInt(line[6])
                );
                // Save marker to the database using the contract
                this.masterList.add(marker);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
    public void arToDetails(int entry) {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        Bundle args = new Bundle();
        args.putString("snippet", Integer.toString(entry));
        navController.navigate(
                R.id.action_ARFragment_to_MarkerInfoFragment,
                args
        );
    }
}