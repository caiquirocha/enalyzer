package com.github.neone35.enalyzer.main;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.github.neone35.enalyzer.R;
import com.github.neone35.enalyzer.dummy.DummyContent;
import com.github.neone35.enalyzer.main.codes.CodeCategoryListFragment;
import com.github.neone35.enalyzer.main.scans.ScanDetailListFragment;
import com.github.neone35.enalyzer.main.scans.ScanPhotoListFragment;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;

import java.util.Calendar;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements
        ScanPhotoListFragment.OnScanPhotoListListener,
        ScanDetailListFragment.OnScanDetailListListener,
        CodeCategoryListFragment.OnCodeCategoryListListener {

    @BindView(R.id.viewpager)
    ViewPager mViewPager;
    @BindView(R.id.sliding_tabs)
    TabLayout mTabLayout;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    public static final String SCANS_DETAIL = "scans_page";

    @BindString(R.string.app_name)
    String mAppName;

    private FirebaseAnalytics mFirebaseAnalytics;
    public static String TAB_POSITION = "tab_position";
    public static String PAGER_INSTRUCT_MOTION = "pager_motion";
    @BindView(R.id.inc_add_fab)
    FloatingActionButton mAddFab;
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
        setupViewPager(mViewPager);

        // show instructive motion (once per lifetime)
        if (!mSettings.getBoolean(PAGER_INSTRUCT_MOTION, false))
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
        editor.putBoolean(PAGER_INSTRUCT_MOTION, true);
        editor.apply();
    }

    private void setupViewPager(ViewPager pager) {
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Logger.d("Page is being selected");
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
        outState.putInt(TAB_POSITION, mTabLayout.getSelectedTabPosition());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            mViewPager.setCurrentItem(savedInstanceState.getInt(TAB_POSITION));
        }
    }

    @Override
    public void onBackPressed() {
        int currentPage = mViewPager.getCurrentItem();
        int fragStackNum = mFragmentManager.getBackStackEntryCount();
        // if tab = scans && scans detail is shown
        if (currentPage == 0 && fragStackNum != 0) {
            mFragmentManager.popBackStack(SCANS_DETAIL, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            // if tab = codes && codes detail is shown
        } else if (currentPage == 1 && fragStackNum != 0) {
            // TODO: replace with another pop call to CODES_PAGE
            Logger.d("Must be replaced by another fragment stack to pop off");
        } else {
            super.onBackPressed();
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
    public void onScanDetailListInteraction(DummyContent.DummyItem item) {
        // TODO: Open AdditiveActivity here
    }

    @Override
    public void onCodeCategoryListInteraction(DummyContent.DummyItem item) {
        // TODO: Begin fragment transaction here
    }

}

