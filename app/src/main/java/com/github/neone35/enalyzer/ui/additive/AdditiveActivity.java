package com.github.neone35.enalyzer.ui.additive;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.github.neone35.enalyzer.R;
import com.github.neone35.enalyzer.ui.main.MainActivity;
import com.orhanobut.logger.Logger;

import butterknife.ButterKnife;

public class AdditiveActivity extends AppCompatActivity implements
        AdditiveFragment.OnAdditiveFragmentListener {

    private FragmentManager mFragmentManager;
    public static final String KEY_PREVIOUS = "previous_click";
    public static final String KEY_NEXT = "next_click";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_additive);
        ButterKnife.bind(this);
        mFragmentManager = getSupportFragmentManager();

        setupActionBar();

        // check if intent bundle received successfully and setup view
        Bundle mainExtrasBundle = getIntent().getExtras();
        if (mainExtrasBundle != null) {
            String selectedEcode = mainExtrasBundle.getString(MainActivity.KEY_SELECTED_ECODE);
            String tabSource = mainExtrasBundle.getString(MainActivity.KEY_TAB_SOURCE);
            String photoTransitionName = mainExtrasBundle.getString(MainActivity.KEY_PHOTO_TRANSITION_VIEW);
            String ecodeTransitionName = mainExtrasBundle.getString(MainActivity.KEY_ECODE_TRANSITION_VIEW);
            // only create fragment if there was no configuration change
            if (savedInstanceState == null) {
                // TODO: Send scanID or ecodeID with tabSource
                AdditiveFragment additiveFragment =
                        AdditiveFragment.newInstance(selectedEcode, tabSource, photoTransitionName, ecodeTransitionName);
                mFragmentManager.beginTransaction()
                        .add(R.id.frag_additive, additiveFragment)
                        .commit();
            }
        }
    }

    public void setupActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    public void switchAdditive(int scanID, int additiveID) {
//        AdditiveFragment stepsListFragment = AdditiveFragment.newInstance(scanID, additiveID);
//        mFragmentManager.beginTransaction()
//                .replace(R.id.frag_additive, stepsListFragment)
//                .commit();

    }

    @Override
    public void onAdditiveFragmentInteraction(String replaceWithData, int currentAdditiveID, String whichBtn) {
        Logger.d("Additive switch button is clicked");
//        int nextStepID = -1;
//        // additive ids begin at 0
//        int currentRecipeStepsNum = currentRecipe.getSteps().size() - 1;
//        // calc next step ID depending on button click
//        if (whichBtn.equals(KEY_PREVIOUS)) {
//            if (currentStepID > 0)
//                nextStepID = currentStepID - 1;
//        } else if (whichBtn.equals(KEY_NEXT)) {
//            if (currentStepID < currentRecipeStepsNum)
//                nextStepID = currentStepID + 1;
//        }
//
//        int currentRecipeID = currentRecipe.getId();
//        // if next step exists, switch step
//        if (nextStepID != -1)
//            switchStep(currentRecipeID, nextStepID);
    }
}
