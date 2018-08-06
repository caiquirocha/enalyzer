package com.github.neone35.enalyzer.ui.main;

import android.animation.ObjectAnimator;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import com.blankj.utilcode.util.ToastUtils;
import com.github.neone35.enalyzer.ui.scan.ScanActivity;
import com.github.neone35.enalyzer.ui.additive.AdditiveActivity;
import com.github.neone35.enalyzer.R;
import com.github.neone35.enalyzer.dummy.DummyContent;
import com.github.neone35.enalyzer.ui.main.codes.CodeCategoryListFragment;
import com.github.neone35.enalyzer.ui.main.codes.CodeDetailListFragment;
import com.github.neone35.enalyzer.ui.main.scans.ScanDetailListFragment;
import com.github.neone35.enalyzer.ui.main.scans.ScanPhotoListFragment;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements
        ScanPhotoListFragment.OnScanPhotoListListener,
        ScanDetailListFragment.OnScanDetailListListener,
        CodeCategoryListFragment.OnCodeCategoryListListener,
        CodeDetailListFragment.OnCodeDetailListListener {

    @BindView(R.id.viewpager)
    ViewPager mViewPager;
    @BindView(R.id.sliding_tabs)
    TabLayout mTabLayout;
    @BindView(R.id.toolbar_appbar_tabs)
    Toolbar mToolbar;
    @BindView(R.id.inc_add_fab)
    FloatingActionButton mAddFab;

    @BindString(R.string.app_name)
    String mAppName;
    @BindString(R.string.photo_fab_transition)
    String photoFabTransitionName;
    @BindString(R.string.additive_image_transition)
    String additiveImageTransitionName;

    public static final String SCANS_DETAIL = "scans_detail";
    public static final String CODES_DETAIL = "codes_detail";
    public static final String KEY_TAB_POSITION = "tab_position";
    public static final String KEY_PAGER_INSTRUCT_MOTION = "pager_motion";
    public static final String KEY_SELECTED_ECODE = "selected_ecode";
    public static final String KEY_TAB_SOURCE = "tab_source";
    public static final String KEY_TRANSITION_NAME = "transition_name";
    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    public static int ID_CAMERA_PERMISSION;
    public static int ID_EXTERNAL_STORAGE_PERMISSION;
    private FirebaseAnalytics mFirebaseAnalytics;
    private SharedPreferences mSettings;
    private FragmentManager mFragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Logger.addLogAdapter(new AndroidLogAdapter());
        ButterKnife.bind(this);

        mSettings = this.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        mFragmentManager = getSupportFragmentManager();

        setSupportActionBar(mToolbar);
        setupFirebaseAnalytics();
        setupTabs(mFragmentManager);
        setupAddFAB(mViewPager, mAddFab);

        // show instructive motion (once per lifetime)
        if (!mSettings.getBoolean(KEY_PAGER_INSTRUCT_MOTION, false))
            showInstructiveMotion(mViewPager);
    }

    private void showInstructiveMotion(ViewPager pager) {
        final Handler handler = new Handler();
        handler.postDelayed(() ->
                        ObjectAnimator.ofInt(pager, "scrollX", pager.getScrollX() + 100).setDuration(1000).start(),
                2000);
        handler.postDelayed(() ->
                        ObjectAnimator.ofInt(pager, "scrollX", pager.getScrollX() - 100).setDuration(1000).start(),
                3000);
        // save key for lifetime (show motion once)
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean(KEY_PAGER_INSTRUCT_MOTION, true);
        editor.apply();
    }

    private void setupAddFAB(ViewPager pager, FloatingActionButton addFab) {
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                long animDuration = 500;
                float visible = 1.0f;
                float invisible = 0.0f;
                int translateOutX = -mAddFab.getWidth();
                int translateToInitPos = 0;
                if (position == 1) {
                    mAddFab.animate()
                            .alpha(invisible)
                            .translationX(translateOutX)
                            .setDuration(animDuration)
                            .withEndAction(() -> mAddFab.setVisibility(View.INVISIBLE));
                } else {
                    mAddFab.animate()
                            .alpha(visible)
                            .translationX(translateToInitPos)
                            .setDuration(animDuration)
                            .withEndAction(() -> mAddFab.setVisibility(View.VISIBLE));
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        addFab.setOnClickListener(v -> {
            checkAndRequestPermissions();
            if (ID_CAMERA_PERMISSION == PackageManager.PERMISSION_GRANTED &&
                    ID_EXTERNAL_STORAGE_PERMISSION == PackageManager.PERMISSION_GRANTED)
                startScanActivity();
        });
    }

    private void setupFirebaseAnalytics() {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.START_DATE, Calendar.getInstance().getTime().toString());
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle);
    }

    private void setupTabs(FragmentManager fragmentManager) {
        // Set ViewPager's PagerAdapter so that it can display items
        mViewPager.setAdapter(new MainFragmentPagerAdapter(fragmentManager, MainActivity.this));
        // Give the TabLayout the ViewPager
        mTabLayout.setupWithViewPager(mViewPager);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_TAB_POSITION, mTabLayout.getSelectedTabPosition());
    }

    private void checkAndRequestPermissions() {
        ID_CAMERA_PERMISSION = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA);
        ID_EXTERNAL_STORAGE_PERMISSION = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (ID_CAMERA_PERMISSION != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.CAMERA);
        }
        if (ID_EXTERNAL_STORAGE_PERMISSION != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray
                    (new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    mAddFab.setEnabled(true);
                    mAddFab.setAlpha(1f);
                    final Handler handler = new Handler();
                    handler.postDelayed(() ->
                                    mAddFab.performClick(),
                            1000);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    ToastUtils.showShort("Permissions denied. Limited functionality.");
                    mAddFab.setEnabled(false);
                    mAddFab.setAlpha(0.5f);
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            mViewPager.setCurrentItem(savedInstanceState.getInt(KEY_TAB_POSITION));
        }
    }

    @Override
    public void onBackPressed() {
        int currentPagePos = mViewPager.getCurrentItem();
        String currentPageTitle = Objects.requireNonNull(Objects.requireNonNull(
                mViewPager.getAdapter()).getPageTitle(currentPagePos)).toString();
        int fragStackNum = mFragmentManager.getBackStackEntryCount();
        String lastEntryName = null;
        if (fragStackNum != 0) {
            lastEntryName = mFragmentManager.getBackStackEntryAt(fragStackNum - 1).getName();
            /*Logger.d(fragStackNum);
            Logger.d(currentPageTitle);
            Logger.d(lastEntryName);*/
        }

        // both scans & codes details fragments are added to back stack
        if (fragStackNum > 1) {
            if (currentPageTitle.equals("Scans")) {
                mFragmentManager.popBackStack(SCANS_DETAIL, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            } else if (currentPageTitle.equals("Codes")) {
                mFragmentManager.popBackStack(CODES_DETAIL, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
            // one (scans or codes) detail fragment is in back stack
        } else if (fragStackNum == 1) {
            // if tab = scans
            if (currentPageTitle.equals("Scans")) {
                // && backstack = scans
                if (lastEntryName.equals(SCANS_DETAIL)) {
                    mFragmentManager.popBackStack(SCANS_DETAIL, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    // && backstack = codes
                } else if (lastEntryName.equals(CODES_DETAIL)) {
                    // exit this activity
                    finish();
                }
                // if tab = codes
            } else if (currentPageTitle.equals("Codes")) {
                // && backstack = codes
                if (lastEntryName.equals(CODES_DETAIL)) {
                    mFragmentManager.popBackStack(CODES_DETAIL, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }
                // && backstack = scans
                else if (lastEntryName.equals(SCANS_DETAIL)) {
                    // switch to 'scans' page
                    mViewPager.setCurrentItem(0, true);
                }
                // if tab = codes && scan detail is shown
            }
            // no fragments found in back stack
        } else if (fragStackNum == 0) {
            if (currentPageTitle.equals("Codes")) {
                // switch to 'scans' page
                mViewPager.setCurrentItem(0, true);
            } else if (currentPageTitle.equals("Scans")) {
                // exit this activity
                finish();
            }
        } else {
            super.onBackPressed();
        }
    }

    private void startScanActivity() {
        Intent scanActivityIntent = new Intent(this, ScanActivity.class);
        // start with transitions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityOptionsCompat options = ActivityOptionsCompat.
                    makeSceneTransitionAnimation(this, mAddFab, photoFabTransitionName);
            startActivity(scanActivityIntent, options.toBundle());
            // start without transitions
        } else {
            startActivity(scanActivityIntent);
        }
    }

    private void startAdditiveActivity(String tabSourceKey, String transitionName) {
        Intent additiveActivityIntent = new Intent(this, AdditiveActivity.class);
        Bundle additiveBundle = new Bundle();
        additiveBundle.putString(KEY_SELECTED_ECODE, "E221");
        additiveBundle.putString(KEY_TAB_SOURCE, tabSourceKey);
        additiveBundle.putString(KEY_TRANSITION_NAME, transitionName);
        additiveActivityIntent.putExtras(additiveBundle);
        // start with transitions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // set shared element source
            ImageView additivePhoto = null;
            if (tabSourceKey.equals(CODES_DETAIL))
                additivePhoto = findViewById(R.id.iv_code_detail_photo);
            else if (tabSourceKey.equals(SCANS_DETAIL)) {
                additivePhoto = findViewById(R.id.iv_scan_detail_photo);
            }
            // transition without shared elements
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this);
            if (additivePhoto != null) {
                // transition with shared elements
                options = ActivityOptions.makeSceneTransitionAnimation(this, additivePhoto, transitionName);
            }
            startActivity(additiveActivityIntent, options.toBundle());
            // start without transitions
        } else {
            startActivity(additiveActivityIntent);
        }
    }

    @Override
    public void onScanListInteraction(DummyContent.DummyItem item) {
        Logger.d(item);
        int currentPage = mViewPager.getCurrentItem();
        // if current page is scans
        if (currentPage == 0) {
            // replace ScanList with ScanDetailList
            mFragmentManager.beginTransaction()
                    .replace(R.id.fl_scan_root, ScanDetailListFragment.newInstance(1))
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    // enable switch back to ScanList with addToBackStack
                    .addToBackStack(SCANS_DETAIL)
                    .commit();
        }
    }

    @Override
    public void onScanDetailListInteraction(String transitionName) {
        startAdditiveActivity(SCANS_DETAIL, transitionName);
    }

    @Override
    public void onCodeCategoryListInteraction(DummyContent.DummyItem item) {
        Logger.d(item);
        int currentPage = mViewPager.getCurrentItem();
        // if current page is codes
        if (currentPage == 1) {
            // replace CodeCategoryList with CodeDetailList
            mFragmentManager.beginTransaction()
                    .replace(R.id.fl_code_root, CodeDetailListFragment.newInstance(3))
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    // enable switch back to CodeCategoryList with addToBackStack
                    .addToBackStack(CODES_DETAIL)
                    .commit();
        }
    }

    @Override
    public void onCodeDetailListInteraction(String transitionName) {
        startAdditiveActivity(CODES_DETAIL, transitionName);
    }
}


