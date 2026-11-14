package com.example.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.example.MultiFloatApp;
import com.example.R;
import com.example.util.EdgeToEdgeHelper;
import com.example.util.PermissionHelper;

public class OnboardingActivity extends AppCompatActivity {

    private static class Slide {
        final int imageRes;
        final int titleRes;
        final int descRes;
        Slide(int imageRes, int titleRes, int descRes) {
            this.imageRes = imageRes;
            this.titleRes = titleRes;
            this.descRes = descRes;
        }
    }

    private final Slide[] slides = new Slide[]{
            new Slide(R.drawable.img_onboarding_windows_1794636603479, R.string.onboarding_title_1, R.string.onboarding_desc_1),
            new Slide(R.drawable.img_onboarding_tools_1794636603720, R.string.onboarding_title_2, R.string.onboarding_desc_2),
            new Slide(R.drawable.img_onboarding_sidebar_1794636604938, R.string.onboarding_title_3, R.string.onboarding_desc_3)
    };

    private ViewPager2 viewPager;
    private LinearLayout dotsContainer;
    private Button btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        EdgeToEdgeHelper.setup(this, findViewById(R.id.onboarding_root));

        viewPager = findViewById(R.id.vp_onboarding);
        dotsContainer = findViewById(R.id.layout_onboarding_dots);
        btnNext = findViewById(R.id.btn_onboarding_next);

        viewPager.setAdapter(new SlideAdapter());
        updateDots(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
                btnNext.setText(position == slides.length - 1 ? R.string.action_get_started : R.string.action_next);
            }
        });

        findViewById(R.id.btn_onboarding_skip).setOnClickListener(v -> finishOnboarding());
        btnNext.setOnClickListener(v -> {
            int pos = viewPager.getCurrentItem();
            if (pos < slides.length - 1) {
                viewPager.setCurrentItem(pos + 1, true);
            } else {
                finishOnboarding();
            }
        });
    }

    private void updateDots(int current) {
        dotsContainer.removeAllViews();
        int size = EdgeToEdgeHelper.dpToPx(this, 8);
        int activeW = EdgeToEdgeHelper.dpToPx(this, 22);
        for (int i = 0; i < slides.length; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(i == current ? activeW : size, size);
            lp.setMargins(6, 0, 6, 0);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(i == current ? R.drawable.bg_button_electric : R.drawable.bg_pill_chip);
            dotsContainer.addView(dot);
        }
    }

    private void finishOnboarding() {
        MultiFloatApp.get().getPreferences().setOnboardingCompleted(true);
        Intent next = new Intent(this, MultitaskingSetupCenterActivity.class);
        next.putExtra(MultitaskingSetupCenterActivity.EXTRA_FROM_ONBOARDING, true);
        startActivity(next);
        finish();
    }

    private class SlideAdapter extends RecyclerView.Adapter<SlideAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding_slide, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Slide s = slides[position];
            holder.img.setImageResource(s.imageRes);
            holder.title.setText(s.titleRes);
            holder.desc.setText(s.descRes);
        }

        @Override
        public int getItemCount() { return slides.length; }

        class VH extends RecyclerView.ViewHolder {
            final ImageView img;
            final TextView title, desc;
            VH(View itemView) {
                super(itemView);
                img = itemView.findViewById(R.id.iv_slide_art);
                title = itemView.findViewById(R.id.tv_slide_title);
                desc = itemView.findViewById(R.id.tv_slide_desc);
            }
        }
    }
}
