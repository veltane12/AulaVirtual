package com.aula.virtual;

import androidx.appcompat.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.aula.virtual.data.entity.Faculty;
import com.aula.virtual.data.entity.Facility;
import com.aula.virtual.data.entity.Notification;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.ui.DialogUtils;
import com.aula.virtual.ui.MainViewModel;
import com.aula.virtual.ui.ThemeHelper;
import com.aula.virtual.ui.ToastUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private NavController navController;
    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        applyNavbarPosition();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationIcon(null);
        }
        
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.loginFragment, R.id.adminHomeFragment, R.id.studentHomeFragment, R.id.professorHomeFragment)
                .build();
        
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        View layoutNavbar = findViewById(R.id.layoutNavbar);
        ImageButton btnHome = findViewById(R.id.btnNavHome);
        ImageButton btnBack = findViewById(R.id.btnNavBack);
        TextView tvNavTitle = findViewById(R.id.tvNavTitle);
        ImageButton btnRefresh = findViewById(R.id.btnNavRefresh);
        ImageButton btnNotification = findViewById(R.id.btnNavNotification);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setHomeButtonEnabled(false);
        }

        btnHome.setOnClickListener(v -> navigateToHome());
        btnBack.setOnClickListener(v -> {
            if (isCurrentDestinationHome()) {
                // User requested "no utility" on home screens
                return;
            }
            if (!navController.popBackStack()) {
                showExitConfirmationDialog();
            }
        });
        btnRefresh.setOnClickListener(v -> {
            // Visual feedback
            Animation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(500);
            v.startAnimation(rotate);
            
            viewModel.refreshData();
            refreshCurrentDestination();
            ToastUtils.showToast(this, "Actualizando...");
        });
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                hideKeyboard();
                if (navController != null && (navController.getCurrentDestination() == null || navController.getCurrentDestination().getId() != R.id.notificationListFragment)) {
                    navController.navigate(R.id.notificationListFragment);
                }
            });
        }

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            }
            if (toolbar != null) {
                toolbar.setNavigationIcon(null);
            }
            if (id == R.id.loginFragment) {
                layoutNavbar.setVisibility(View.VISIBLE);
                tvNavTitle.setText("Loguin");
                btnBack.setEnabled(false);
                btnBack.setAlpha(0.3f);
                btnHome.setEnabled(false);
                btnHome.setAlpha(0.3f);
            } else {
                layoutNavbar.setVisibility(View.VISIBLE);
                tvNavTitle.setText(destination.getLabel());
                btnBack.setEnabled(true);
                btnBack.setAlpha(1.0f);
                btnHome.setEnabled(true);
                btnHome.setAlpha(1.0f);
                
                boolean isHome = id == R.id.adminHomeFragment || 
                                id == R.id.studentHomeFragment || 
                                id == R.id.professorHomeFragment;
                btnBack.setAlpha(isHome ? 0.5f : 1.0f);
                if (isHome) {
                    btnBack.setEnabled(false);
                }
            }
        });

        TextView tvStatus = findViewById(R.id.tvConnectionStatus);
        if (ThemeHelper.isLocalMode(this)) {
            tvStatus.setVisibility(View.GONE);
        }
        
        viewModel.isServerConnected().observe(this, connected -> {
            if (ThemeHelper.isLocalMode(this) || Boolean.TRUE.equals(connected)) {
                tvStatus.setVisibility(View.GONE);
            } else {
                tvStatus.setVisibility(View.VISIBLE);
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isCurrentDestinationHome()) {
                    showExitConfirmationDialog();
                } else {
                    if (!navController.popBackStack()) {
                        showExitConfirmationDialog();
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.startKeepAlive();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (viewModel != null) {
            viewModel.stopKeepAlive();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    private void showExitConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Salir")
                .setMessage("¿Estás seguro de que deseas salir de la aplicación?")
                .setPositiveButton("Sí", (dialog, which) -> finish())
                .setNegativeButton("No", null)
                .show();
    }

    private void hideKeyboard(View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus != null) {
            hideKeyboard(focus);
        }
    }

    private void navigateToHome() {
        if (viewModel.getCurrentUser().getValue() == null) return;
        String role = viewModel.getCurrentUser().getValue().role;
        int homeId = -1;
        if ("ADMIN".equals(role)) homeId = R.id.adminHomeFragment;
        else if ("PROFESSOR".equals(role)) homeId = R.id.professorHomeFragment;
        else if ("STUDENT".equals(role)) homeId = R.id.studentHomeFragment;
        
        if (homeId != -1 && navController.getCurrentDestination() != null && 
            navController.getCurrentDestination().getId() != homeId) {
            navController.popBackStack(homeId, false);
        }
    }

    private void refreshCurrentDestination() {
        if (navController.getCurrentDestination() == null) return;
        int destId = navController.getCurrentDestination().getId();
        Bundle args = navController.getCurrentBackStackEntry() != null ? 
                     navController.getCurrentBackStackEntry().getArguments() : null;
        
        navController.navigate(destId, args, new NavOptions.Builder()
                .setPopUpTo(destId, true)
                .build());
    }

    private boolean isCurrentDestinationHome() {
        if (navController.getCurrentDestination() == null) return false;
        int id = navController.getCurrentDestination().getId();
        return id == R.id.adminHomeFragment || 
               id == R.id.studentHomeFragment || 
               id == R.id.professorHomeFragment;
    }

    private void applyNavbarPosition() {
        LinearLayout rootLayout = findViewById(R.id.mainRootLayout);
        View appBarLayout = findViewById(R.id.appBarLayout);
        View navHostFragment = findViewById(R.id.nav_host_fragment);

        if (rootLayout != null && appBarLayout != null && navHostFragment != null) {
            String position = ThemeHelper.getNavbarPosition(this);
            rootLayout.removeView(appBarLayout);
            rootLayout.removeView(navHostFragment);

            if (ThemeHelper.NAVBAR_POSITION_BOTTOM.equals(position)) {
                rootLayout.addView(navHostFragment);
                rootLayout.addView(appBarLayout);
            } else {
                rootLayout.addView(appBarLayout);
                rootLayout.addView(navHostFragment);
            }
        }
    }
}
