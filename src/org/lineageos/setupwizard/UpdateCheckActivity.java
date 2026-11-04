/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import static com.google.android.setupcompat.util.ResultCodes.RESULT_SKIP;

import static org.lineageos.setupwizard.SetupWizardApp.OPEN_UPDATER_AFTER_SETUP;
import static org.lineageos.setupwizard.util.SetupWizardUtils.UPDATER_PACKAGE;

import android.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

public class UpdateCheckActivity extends BaseSetupWizardActivity {

    private static final String ACTION_SETUP_UPDATE_CHECK =
            "org.lineageos.updater.action.SETUP_UPDATE_CHECK";

    // Backstop in case the updater does not answer; it limits its own check to 8 seconds.
    private static final long CHECK_TIMEOUT_MILLIS = 10000;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mTimeout = () -> onCheckFinished(false);
    private boolean mCheckFinished;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getGlifLayout().setDescriptionText(getString(R.string.setup_update_checking));
        getGlifLayout().setProgressBarShown(true);
        hideNextButton();
        getNavigationBar().getSkipButton().setVisibility(View.INVISIBLE);
        startCheck();
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacks(mTimeout);
        super.onDestroy();
    }

    private void startCheck() {
        Intent intent = new Intent(ACTION_SETUP_UPDATE_CHECK)
                .setPackage(UPDATER_PACKAGE)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND
                        | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        sendOrderedBroadcast(intent, null, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent resultIntent) {
                onCheckFinished(getResultCode() == RESULT_OK);
            }
        }, mHandler, RESULT_CANCELED, null, null);
        mHandler.postDelayed(mTimeout, CHECK_TIMEOUT_MILLIS);
    }

    private void onCheckFinished(boolean updateAvailable) {
        if (mCheckFinished || isFinishing() || isDestroyed()) {
            return;
        }
        mCheckFinished = true;
        mHandler.removeCallbacks(mTimeout);

        if (!updateAvailable) {
            // See SimMissingActivity: a subactivity before us must see this as a backward
            // navigation if the user later returns to it.
            finishAction(RESULT_SKIP, new Intent().putExtra("onBackPressed", true));
            return;
        }

        getGlifLayout().setProgressBarShown(false);
        getGlifLayout().setHeaderText(R.string.setup_update_available);
        getGlifLayout().setDescriptionText(getString(R.string.setup_update_available_summary));
        setNextText(R.string.setup_update_now);
        setSkipText(R.string.setup_update_later);
        getNextButton().setVisibility(View.VISIBLE);
        getNavigationBar().getSkipButton().setVisibility(View.VISIBLE);
    }

    @Override
    protected void onNextPressed() {
        SetupWizardApp.getSettingsBundle().putBoolean(OPEN_UPDATER_AFTER_SETUP, true);
        super.onNextPressed();
    }

    @Override
    protected void onSkipPressed() {
        SetupWizardApp.getSettingsBundle().putBoolean(OPEN_UPDATER_AFTER_SETUP, false);
        super.onSkipPressed();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.update_check_page;
    }

    @Override
    protected int getTitleResId() {
        return R.string.setup_update_title;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.ic_system_update;
    }
}
