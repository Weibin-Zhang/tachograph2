package com.esp.tachograph2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

//import com.esp.tachograph2.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

//    private ActivityMainBinding binding;
    private BottomNavigationView bottomNavigationView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        checkNeedPermissions();
//        binding = ActivityMainBinding.inflate(getLayoutInflater());
//        setContentView(binding.getRoot());

        // Example of a call to a native method
//        TextView tv = findViewById(R.id.sample_text);
//        tv.setText(stringFromJNI());
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(navListener);
//        FrameLayout fragement_container = findViewById(R.id.fragment_container);

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HttpLiveBroadcastFragment()).commit();
    }
    private void checkNeedPermissions(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //多个权限一起申请
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 1);
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;
                    switch (item.getItemId()){
//                        case 0:
                        case R.id.navigation_live_broadcast:
                            selectedFragment = new HttpLiveBroadcastFragment();
//                            selectedFragment = new LiveBroadcastFragment();
                            break;
//                        case 1:
                        case R.id.navigation_playback:
                            selectedFragment = new PlaybackFragment();
                            break;
//                        case 2:
                        case R.id.navigation_home:
                            selectedFragment = new HomeFragment();
                            break;
                    }
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                            selectedFragment).commit();
//                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
                    return true;
                }
            };

    /**
     * A native method that is implemented by the 'tachograph' native library,
     * which is packaged with this application.
     */
//    public native String stringFromJNI();
//
//    // Used to load the 'tachograph' library on application startup.
//    static {
//        System.loadLibrary("tachograph");
//    }
}