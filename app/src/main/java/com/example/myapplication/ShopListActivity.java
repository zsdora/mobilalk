package com.example.myapplication;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.KeyEventDispatcher;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.Collection;

public class ShopListActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private static final String LOG_TAG = ShopListActivity.class.getName();

    private int gridNumber = 1;
    private RecyclerView mRecyclerView;
    private ArrayList<ShoppingItem> mItemsData;
    private ShoppingItemAdapter mAdapter;

    private FirebaseFirestore mFirestore;
    private CollectionReference mItems;
    private SharedPreferences preferences;
    private FrameLayout redCircle;
    private TextView contentTextView;
    private boolean viewRow = true;
    private int cartItems = 0;
    private Integer itemLimit = 10;
    private NotificationHandler mNotificationHandler;
    private AlarmManager mAlarmManager;
    private JobScheduler mJobScheduler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_list);
        //mAuth = FirebaseAuth.getInstance();
        // mAuth.signOut();
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
            Log.d(LOG_TAG,"Autentikált felhasználó!");
        }else {
            Log.d(LOG_TAG,"Nem autentikált felhasználó!");
            finish();
        }

        // preferences = getSharedPreferences(PREF_KEY, MODE_PRIVATE);
        // if(preferences != null) {
        //     cartItems = preferences.getInt("cartItems", 0);
        //     gridNumber = preferences.getInt("gridNum", 1);
        // }

        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, gridNumber));
        mItemsData = new ArrayList<>();

        mAdapter = new ShoppingItemAdapter(this, mItemsData);
        mRecyclerView.setAdapter(mAdapter);

        mFirestore = FirebaseFirestore.getInstance();
        mItems = mFirestore.collection("Items");
        queryData();

        mNotificationHandler = new NotificationHandler(this);
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mJobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        //setAlarmManager();
        setJobScheduler();
    }

    private void setJobScheduler() {
        int networkType = JobInfo.NETWORK_TYPE_UNMETERED;
        int hardDeadLine = 5000;
        ComponentName name = new ComponentName(getPackageName(), NotificationJobService.class.getName());
        JobInfo.Builder builder = new JobInfo.Builder(0, name)
                .setRequiredNetworkType(networkType)
                .setOverrideDeadline(hardDeadLine);
        mJobScheduler.schedule(builder.build());
        //mJobScheduler.cancel(0);
    }

    private void queryData() {
        mItemsData.clear();

        mItems.orderBy("cartedCount", Query.Direction.DESCENDING).limit(itemLimit).get()
               .addOnSuccessListener(queryDocumentSnapshots ->  {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots){
                ShoppingItem item = document.toObject(ShoppingItem.class);
                item.setId(document.getId());
                mItemsData.add(item);
            }

            if (mItemsData.size() == 0){
                initializeData();
                queryData();
            }
            mAdapter.notifyDataSetChanged();
        });
    }

    public void deleteItem(ShoppingItem item){
        DocumentReference ref = mItems.document(item._getId());

        ref.delete().addOnSuccessListener(success ->{
            Log.d(LOG_TAG, "Termék sikeresen törölve: "+ item._getId());
        })
        .addOnFailureListener(failure -> {
            Toast.makeText(this, "A "+ item._getId()+ " termék törlése sikertelen.", Toast.LENGTH_SHORT).show();
        });

        queryData();
        mNotificationHandler.cancel();
    }

    public void updateAlertIcon(ShoppingItem item){
        cartItems = (cartItems + 1);
        if (0 < cartItems){
            contentTextView.setText(String.valueOf(cartItems));
        }else {
            contentTextView.setText("");
        }

        redCircle.setVisibility((cartItems > 0) ? VISIBLE : GONE);

        mItems.document(item._getId()).update("cartedCount", item.getCartedCount() +1)
           .addOnFailureListener(failure -> {
               Toast.makeText(this, "A "+ item._getId()+ " termék megváltoztatása sikertelen.", Toast.LENGTH_SHORT).show();
           });

        mNotificationHandler.send(item.getName());
        queryData();
    }

    private void initializeData() {
        String[] itemList = getResources().getStringArray(R.array.shopping_item_names);
        String[] itemInfo = getResources().getStringArray(R.array.shopping_item_desc);
        String[] itemPrice = getResources().getStringArray(R.array.shopping_item_price);
        TypedArray itemsImageResources = getResources().obtainTypedArray(R.array.shopping_item_images);
        TypedArray itemsRate = getResources().obtainTypedArray(R.array.shopping_item_rates);


        for (int i = 0; i < itemList.length; i++){
            mItems.add(new ShoppingItem(
                    itemList[i],
                    itemInfo[i],
                    itemPrice[i],
                    itemsRate.getFloat(i, 0),
                    itemsImageResources.getResourceId(i, 0),
                    0));
        }

        itemsImageResources.recycle();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.shop_list_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.search_bar);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {return false;}

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(LOG_TAG, s);
                mAdapter.getFilter().filter(s);
                return false;
            }
        });


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.log_out_button:
                Log.d(LOG_TAG, "Kijelentkezés megnyomva.");
                FirebaseAuth.getInstance().signOut();
                finish();
                return true;
            case R.id.setting_button:
                Log.d(LOG_TAG, "Beállítások megnyomva.");
                return true;
            case R.id.cart:
                Log.d(LOG_TAG, "Kosár megnyomva.");
                return true;
            case R.id.view_selector:
                Log.d(LOG_TAG, "Nézet kiválasztó megnyomva.");
                if (viewRow){
                    changeSpanCount(item, R.drawable.ic_view_grid, 1);
                }else {
                    changeSpanCount(item, R.drawable.ic_view_row, 2);
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void changeSpanCount(MenuItem item, int drawableId, int spanCount) {
        viewRow = !viewRow;
        item.setIcon(drawableId);
        GridLayoutManager layoutManager = (GridLayoutManager) mRecyclerView.getLayoutManager();
        layoutManager.setSpanCount(spanCount);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){

        final MenuItem alertMenuItem = menu.findItem(R.id.cart);
        FrameLayout rootView = (FrameLayout) alertMenuItem.getActionView();

        redCircle = (FrameLayout) rootView.findViewById(R.id.view_alert_red_circle);
        contentTextView = (TextView) rootView.findViewById(R.id.view_alert_count_textview);

        rootView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                onOptionsItemSelected(alertMenuItem);
            }
        });

        return super.onPrepareOptionsMenu(menu);
    }

    private void setAlarmManager(){
        long repeatInterval = 60*1000; //AlarmManager.INTERVAL_FIFTEEN_MINUTES;
        long triggerTime = SystemClock.elapsedRealtime() + repeatInterval;
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE);

        mAlarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTime,
                repeatInterval,
                pendingIntent);

        //mAlarmManager.cancel(pendingIntent);
    }
}