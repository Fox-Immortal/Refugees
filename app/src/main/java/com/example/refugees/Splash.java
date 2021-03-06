package com.example.refugees;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ViewTreeObserver;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Splash extends AppCompatActivity {
    Context context = this;
    ImageView top;
    ImageView bottom_dark;
    ImageView bottom_light;
    ImageView logo;
    Interpolator interpolator = new FastOutSlowInInterpolator();

    int duration = 500;
    float ScreenWidth;
    float ScreenHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        final ConstraintLayout layout = (ConstraintLayout) findViewById(R.id.background);
        ViewTreeObserver vto = layout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                ScreenWidth = displayMetrics.widthPixels;
                ScreenHeight = displayMetrics.heightPixels;

                setup();
            }
        });
        top = findViewById(R.id.top);
        bottom_dark = findViewById(R.id.bottom_dark);
        bottom_light = findViewById(R.id.bottom_light);
        logo = findViewById(R.id.logo);


    }

    public void setup() {
        findViewById(R.id.anime_mid).setAlpha(0);
        logo.setY(ScreenHeight);
        bottom_dark.setY(ScreenHeight - bottom_dark.getHeight());
        bottom_light.setY(ScreenHeight - bottom_light.getHeight());
        bottom_dark.setY(ScreenHeight);
        bottom_light.setY(ScreenHeight);
        top.setY(top.getHeight() * -1);
        animate();
    }

    public void animate() {
        logo();
    }

    public void logo() {
        logo.animate().setDuration(duration * 2).translationYBy((ScreenHeight * 0.85f) * -1).setInterpolator(interpolator).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                top();
            }
        });
    }

    public void top() {
        logo.animate().setDuration(duration).translationYBy((top.getHeight())).setInterpolator(interpolator).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
            }
        });
        top.animate().setDuration(duration).translationYBy(top.getHeight()).setInterpolator(interpolator).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                bottom();
            }
        });
    }

    public void bottom() {
        logo.animate().setDuration(duration).translationYBy((bottom_dark.getHeight()) / 4f * -1).setInterpolator(interpolator);
        bottom_dark.animate().setDuration(duration).translationYBy(bottom_dark.getHeight() * -1f).setInterpolator(interpolator);
        bottom_light.animate().setDuration(duration).translationYBy(bottom_light.getHeight() * -1f).setInterpolator(interpolator).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                SharedPreferences lang = getSharedPreferences("LANGUAGE_PREFERENCE", Context.MODE_PRIVATE);
                String lng = lang.getString("lang", "null");
                if (lng.equals("null")) {
                    findViewById(R.id.anime).animate().setDuration(100).alpha(1);
                    Intent intent = new Intent(context, Language.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                    finish();
                } else {
                    FirebaseAuth mAuth = FirebaseAuth.getInstance();
                    FirebaseUser user = mAuth.getCurrentUser();
                    if(user != null && user.isEmailVerified() && isConnect()) {
//                if(true) {
                        findViewById(R.id.anime_mid).animate().setDuration(100).alpha(1);
                        animate_main();
                    }
                    else {
                        findViewById(R.id.anime).animate().setDuration(100).alpha(1);
                        Intent intent = new Intent(context, LogOptions.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivity(intent);
                        finish();
                    }
                }
            }
        });
    }
    public void animate_main() {
        bottom_light.animate().setListener(null);
        bottom_dark.animate().setListener(null);
        logo.animate().setListener(null);
        top.animate().setListener(null);
        findViewById(R.id.anime_mid).animate().setDuration(100).alpha(1);
        bottom_light.setPivotY(bottom_light.getHeight());
        bottom_dark.setPivotY(bottom_light.getHeight());
        top.setPivotY(0);
        bottom_light.animate().setDuration(duration - 200).scaleY(0f);
        bottom_dark.animate().setDuration(duration - 200).scaleY(0f);
        top.animate().setDuration(duration - 200).scaleY(0f);
        logo.animate().setDuration(duration + 100).translationXBy(logo.getWidth() * -1).setInterpolator(interpolator).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                Intent intent = new Intent(context, MainScreenActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                finish();
            }
        });
    }
    @Override
    public void onPause() {
        super.onPause();
        overridePendingTransition(0, 0);
    }

    public boolean isConnect() {
        ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMgr.getActiveNetworkInfo();

        if(netInfo == null || !netInfo.isConnected() || !netInfo.isAvailable()){
            Toast.makeText(context, "No Internet Connection!", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }
}