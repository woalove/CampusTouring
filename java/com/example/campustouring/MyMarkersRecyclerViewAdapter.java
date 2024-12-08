package com.example.campustouring;

import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.campustouring.databinding.FragmentAllMarkersBinding;

import java.util.List;

public class MyMarkersRecyclerViewAdapter extends RecyclerView.Adapter<MyMarkersRecyclerViewAdapter.ViewHolder> {

    private final List<CustomMarkerContract.MarkerEntryObj> mValues;

    public MyMarkersRecyclerViewAdapter(List<CustomMarkerContract.MarkerEntryObj> items) {
        mValues = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_all_markers_list, parent, false);
        return new ViewHolder(FragmentAllMarkersBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.mItem = mValues.get(position);
        holder.mContentView.setText(mValues.get(position).name);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to MarkerInfoFragment
                Bundle args = new Bundle();
                args.putString("snippet", String.valueOf(mValues.get(position)._id));
                Navigation.findNavController(holder.itemView).navigate(
                        R.id.action_AllMarkersFragment_to_MarkerInfoFragment,
                        args
                );
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mContentView;
        public CustomMarkerContract.MarkerEntryObj mItem;

        public ViewHolder(FragmentAllMarkersBinding binding) {
            super(binding.getRoot());
            mContentView = binding.content;
        }
    }
}