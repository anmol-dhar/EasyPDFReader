package com.fusionstudios.easypdfreader.Ads;

import android.app.Activity;
import android.content.Context;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.fusionstudios.easypdfreader.Constants.EasyPDFConstants;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class AdServices {

    private static InterstitialAd mInterstitialAd;

    public static void loadBannerAds(LinearLayout container, Context context){
        AdView adView = new AdView(context);
        adView.setAdUnitId(EasyPDFConstants.BANNER_AD_ID);
        adView.setAdSize(AdSize.BANNER);

        container.removeAllViews();
        container.addView(adView);

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    public static void loadFullscreenAds(Context context){

        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(context,EasyPDFConstants.INTERSTITIAL_AD_ID, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mInterstitialAd = null;
                        loadFullscreenAds(context);
                    }
                });
    }

    public static void showFullscreenAds(Context context){
        if (mInterstitialAd != null) {
            mInterstitialAd.show((Activity) context);
        }
    }
}
