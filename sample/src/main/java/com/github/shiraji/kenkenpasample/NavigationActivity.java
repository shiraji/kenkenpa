package com.github.shiraji.kenkenpasample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.shiraji.kenkenpasample.loading.LoadingActivity;
import com.github.shiraji.kenkenpasample.switchfragments.SwitchViewsActivity;

import java.util.ArrayList;
import java.util.List;

public class NavigationActivity extends ActionBarActivity {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private List<String> mDataset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        mRecyclerView = (RecyclerView)findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mDataset = new ArrayList<>();
        mDataset.add(getString(R.string.title_activity_loading));
        mDataset.add(getString(R.string.title_activity_switch));
        mAdapter = new MyAdapter(mDataset, getApplicationContext());
        mRecyclerView.setAdapter(mAdapter);
    }

    private void goToMainActivity() {
        Intent intent = LoadingActivity.createIntent(getApplicationContext());
        startActivity(intent);
    }

    private void goToSwitchFragmentActivity() {
        Intent intent = SwitchViewsActivity.createIntent(getApplicationContext());
        startActivity(intent);
    }

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ItemViewHolder> {
        List<String> mDataset;
        Context mContext;

        public class ItemViewHolder extends RecyclerView.ViewHolder {
            public TextView mTextView;

            public ItemViewHolder(View v) {
                super(v);
                mTextView = (TextView)v.findViewById(R.id.textView1);
            }
        }

        public MyAdapter(List<String> dataset, Context context) {
            mDataset = dataset;
            mContext = context;
        }

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.navigation_row, parent, false);
            return new ItemViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ItemViewHolder holder, final int position) {
            holder.mTextView.setText(mDataset.get(position));
            holder.mTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (position) {
                        case 0:
                            goToMainActivity();
                            return;
                        case 1:
                            goToSwitchFragmentActivity();
                            return;
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mDataset.size();
        }
    }
}