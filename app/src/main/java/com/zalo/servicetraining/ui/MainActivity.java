package com.zalo.servicetraining.ui;


import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.zalo.servicetraining.R;
import com.zalo.servicetraining.model.Item;
import com.zalo.servicetraining.ui.contentprovider.ContentProviderDemoActivity;
import com.zalo.servicetraining.ui.network.DemoNetwork;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements MenuAdapter.OnItemClickListener {
    public static final String TAG = "MainActivity";

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout mSwipeRefresh;

    MenuAdapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        ButterKnife.bind(this);
        mAdapter = new MenuAdapter();
        mAdapter.setListener(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this,2,RecyclerView.VERTICAL,false));
        refreshData();
        mSwipeRefresh.setOnRefreshListener(this::refreshData);
    }

    ArrayList<Item> mList = new ArrayList<>();

    private void refreshData() {
       mList.clear();
       mList.add(new Item().setTitle("Service").setDescription("Create a simple foreground service."));
       mList.add(new Item().setTitle("Content Provider").setDescription("A note app using SQLite to store data"));
       mList.add(new Item().setTitle("Network").setDescription("A weather app fetching data from internet"));

       mAdapter.setData(mList);
       mSwipeRefresh.setRefreshing(false);
    }

    @Override
    public void onEventItemClick(Item item) {
        switch (item.getTitle()) {
            case "Service" :
                startActivity(new Intent(this, ServiceDemoActivity.class));
                break;
            case "Content Provider" :
                startActivity(new Intent(this, ContentProviderDemoActivity.class));
                break;
            case "Network":
                startActivity(new Intent(this, DemoNetwork.class));
                break;
        }
    }

}
