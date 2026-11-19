
package com.example.rikua;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rikua.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {

    private static final long DURASI_TAMPIK = 2400L;
    private static final long DURASI_FADE = 600L;
    private static final long JEDA_BERTINGKAT = 160L;

    private ActivitySplashBinding binding;
    private Handler handler;
    private SessionManager sessionManager;

    private final Runnable prosesNavigasi = this::bukaHalamanTujuan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        handler = new Handler(Looper.getMainLooper());
        sessionManager = new SessionManager(this);

        View[] unsur = {
                binding.splashLogo,
                binding.splashTitle,
                binding.splashRule,
                binding.splashTagline
        };

        boolean animasiAktif = durasiAnimasiNormal();

        if (animasiAktif) {
            jalankanFadeIn(unsur);
        } else {
            for (View v : unsur) {
                v.setAlpha(1f);
            }
        }

        handler.postDelayed(prosesNavigasi, DURASI_TAMPIK);
    }

    private void jalankanFadeIn(View[] unsur) {
        AnimatorSet set = new AnimatorSet();

        ObjectAnimator[] animator = new ObjectAnimator[unsur.length];

        for (int i = 0; i < unsur.length; i++) {
            unsur[i].setAlpha(0f);

            animator[i] = ObjectAnimator.ofFloat(
                    unsur[i],
                    View.ALPHA,
                    0f,
                    1f
            );

            animator[i].setDuration(DURASI_FADE);
            animator[i].setStartDelay(i * JEDA_BERTINGKAT);
            animator[i].setInterpolator(
                    new AccelerateDecelerateInterpolator()
            );
        }

        set.playTogether(animator);
        set.start();
    }

    private boolean durasiAnimasiNormal() {
        try {
            float skala = Settings.Global.getFloat(
                    getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f
            );

            return skala > 0f;

        } catch (Exception e) {
            return true;
        }
    }

    private void bukaHalamanTujuan() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        Intent intent;

        if (sessionManager.isLoggedIn()) {
            // Pengguna sudah memiliki token sesi.
            intent = new Intent(this, MainActivity.class);
        } else {
            // Belum memiliki token sesi.
            intent = new Intent(this, LoginActivity.class);
        }

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (handler != null) {
            handler.removeCallbacks(prosesNavigasi);
        }

        binding = null;
        super.onDestroy();
    }
}
