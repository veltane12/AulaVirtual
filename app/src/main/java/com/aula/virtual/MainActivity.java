package com.aula.virtual;

import androidx.appcompat.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
    private AlertDialog activeNotificationsDialog = null;
    private AlertDialog activeCreateNotificationDialog = null;

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
            Toast.makeText(this, "Actualizando...", Toast.LENGTH_SHORT).show();
        });
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> showNotificationsDialog());
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
        
        viewModel.isServerConnected().observe(this, connected -> {
            if (connected) {
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
        if (activeCreateNotificationDialog != null && activeCreateNotificationDialog.getWindow() != null) {
            hideKeyboard(activeCreateNotificationDialog.getWindow().getDecorView());
        }
        if (activeNotificationsDialog != null && activeNotificationsDialog.getWindow() != null) {
            hideKeyboard(activeNotificationsDialog.getWindow().getDecorView());
        }
    }

    private void dismissActiveNotificationsDialog() {
        if (activeNotificationsDialog != null) {
            try {
                if (activeNotificationsDialog.isShowing()) {
                    activeNotificationsDialog.dismiss();
                }
            } catch (Exception ignored) {}
            activeNotificationsDialog = null;
        }
    }

    private void showNotificationsDialog() {
        hideKeyboard();
        dismissActiveNotificationsDialog();

        User currentUser = viewModel.getCurrentUser().getValue();
        if (currentUser == null) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle("🔔 Notificaciones y Avisos");

            LinearLayout container = DialogUtils.createDialogContainer(this);

            TextView tvServerStatus = new TextView(this);
            tvServerStatus.setTextSize(13);
            tvServerStatus.setPadding(0, 0, 0, 12);
            boolean isConnected = Boolean.TRUE.equals(viewModel.isServerConnected().getValue());
            if (isConnected) {
                tvServerStatus.setText("🟢 Servidor en línea (Sincronizado con MySQL)");
                tvServerStatus.setTextColor(0xFF2E7D32);
            } else {
                tvServerStatus.setText("⚠️ Servidor fuera de línea (Modo Offline)");
                tvServerStatus.setTextColor(0xFFD32F2F);
            }
            container.addView(tvServerStatus);

            TextView tvNotice = new TextView(this);
            tvNotice.setText("\n🔒 Debe iniciar sesión para poder ver las notificaciones y avisos de su cuenta.\n");
            tvNotice.setTextSize(14);
            tvNotice.setGravity(Gravity.CENTER);
            tvNotice.setTextColor(0xFF757575);
            tvNotice.setPadding(16, 24, 16, 24);
            container.addView(tvNotice);

            builder.setView(container);
            builder.setPositiveButton("Entendido", null);

            activeNotificationsDialog = builder.create();
            activeNotificationsDialog.setOnDismissListener(dialog -> activeNotificationsDialog = null);
            activeNotificationsDialog.show();
            return;
        }

        viewModel.fetchNotifications();

        observeOnce(viewModel.getAllNotifications(), allNotifs -> {
            dismissActiveNotificationsDialog();

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle("🔔 Notificaciones y Avisos");

            LinearLayout container = DialogUtils.createDialogContainer(this);

            // Server Status Header
            TextView tvServerStatus = new TextView(this);
            tvServerStatus.setTextSize(13);
            tvServerStatus.setPadding(0, 0, 0, 12);
            boolean isConnected = Boolean.TRUE.equals(viewModel.isServerConnected().getValue());
            if (isConnected) {
                tvServerStatus.setText("🟢 Servidor en línea (Sincronizado con MySQL)");
                tvServerStatus.setTextColor(0xFF2E7D32);
            } else {
                tvServerStatus.setText("⚠️ Servidor fuera de línea (Modo Offline)");
                tvServerStatus.setTextColor(0xFFD32F2F);
            }
            container.addView(tvServerStatus);

            // If Admin: Add button to send new notification
            if (currentUser != null && "ADMIN".equals(currentUser.role)) {
                MaterialButton btnCreate = new MaterialButton(this);
                btnCreate.setText("➕ Crear y Enviar Notificación");
                btnCreate.setAllCaps(false);
                btnCreate.setOnClickListener(v -> {
                    dismissActiveNotificationsDialog();
                    showCreateNotificationDialog(null);
                });
                container.addView(btnCreate);
            }

            // List of Notifications
            List<Notification> userNotifs = filterNotificationsForUser(allNotifs, currentUser);

            if (userNotifs == null || userNotifs.isEmpty()) {
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText("\nNo hay notificaciones recientes.\n");
                tvEmpty.setGravity(Gravity.CENTER);
                tvEmpty.setTextColor(0xFF757575);
                container.addView(tvEmpty);
            } else {
                for (Notification n : userNotifs) {
                    MaterialCardView card = new MaterialCardView(this);
                    card.setCardElevation(2f);
                    card.setRadius(12f);
                    card.setStrokeWidth(1);
                    card.setStrokeColor(0xFFCCCCCC);
                    
                    LinearLayout cardLayout = new LinearLayout(this);
                    cardLayout.setOrientation(LinearLayout.VERTICAL);
                    cardLayout.setPadding(16, 16, 16, 16);

                    TextView tvTitle = new TextView(this);
                    tvTitle.setText(n.title != null ? n.title : "Notificación");
                    tvTitle.setTextSize(15);
                    tvTitle.setTypeface(null, Typeface.BOLD);
                    tvTitle.setTextColor(0xFF111111);

                    TextView tvTargetTag = new TextView(this);
                    tvTargetTag.setTextSize(11);
                    tvTargetTag.setTextColor(ThemeHelper.getSubjectColor(this, ThemeHelper.getAccentColorName(this)));
                    tvTargetTag.setText(getNotificationTargetLabel(n));

                    TextView tvMsg = new TextView(this);
                    tvMsg.setText(n.message != null ? n.message : "");
                    tvMsg.setTextSize(13);
                    tvMsg.setPadding(0, 6, 0, 6);

                    TextView tvSender = new TextView(this);
                    String senderStr = (n.senderName != null ? "Enviado por: " + n.senderName : "") + 
                                       (n.timestamp != null ? " • " + n.timestamp : "");
                    tvSender.setText(senderStr);
                    tvSender.setTextSize(11);
                    tvSender.setTextColor(0xFF666666);

                    cardLayout.addView(tvTitle);
                    cardLayout.addView(tvTargetTag);
                    cardLayout.addView(tvMsg);
                    cardLayout.addView(tvSender);

                    // If Admin: Add Edit & Delete action buttons
                    if (currentUser != null && "ADMIN".equals(currentUser.role)) {
                        LinearLayout actionLayout = new LinearLayout(this);
                        actionLayout.setOrientation(LinearLayout.HORIZONTAL);
                        actionLayout.setGravity(Gravity.END);
                        actionLayout.setPadding(0, 8, 0, 0);

                        MaterialButton btnEdit = new MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle);
                        btnEdit.setText("✏️ Editar");
                        btnEdit.setTextSize(12);
                        btnEdit.setAllCaps(false);
                        btnEdit.setOnClickListener(v -> {
                            dismissActiveNotificationsDialog();
                            showCreateNotificationDialog(n);
                        });

                        MaterialButton btnDelete = new MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle);
                        btnDelete.setText("🗑️ Eliminar");
                        btnDelete.setTextColor(0xFFD32F2F);
                        btnDelete.setTextSize(12);
                        btnDelete.setAllCaps(false);
                        btnDelete.setOnClickListener(v -> {
                            dismissActiveNotificationsDialog();
                            showDeleteNotificationConfirmation(n);
                        });

                        actionLayout.addView(btnEdit);
                        actionLayout.addView(btnDelete);
                        cardLayout.addView(actionLayout);
                    }

                    card.addView(cardLayout);

                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, 10, 0, 10);
                    card.setLayoutParams(lp);

                    container.addView(card);
                }
            }

            ScrollView scrollView = new ScrollView(this);
            scrollView.addView(container);

            builder.setView(scrollView);
            builder.setPositiveButton("Cerrar", null);

            activeNotificationsDialog = builder.create();
            activeNotificationsDialog.setOnDismissListener(dialog -> activeNotificationsDialog = null);
            activeNotificationsDialog.show();
        });
    }

    private void showDeleteNotificationConfirmation(Notification n) {
        if (n == null) return;
        dismissActiveNotificationsDialog();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar Notificación")
                .setMessage("¿Estás seguro de que deseas eliminar esta notificación?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    viewModel.deleteNotification(n.id, () -> {
                        Toast.makeText(this, "Notificación eliminada", Toast.LENGTH_SHORT).show();
                        showNotificationsDialog();
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private List<Notification> filterNotificationsForUser(List<Notification> all, User user) {
        List<Notification> result = new ArrayList<>();
        if (all == null) return result;

        for (Notification n : all) {
            if (n == null || n.targetType == null) {
                result.add(n);
                continue;
            }

            String type = n.targetType.toUpperCase();
            String val = n.targetValue != null ? n.targetValue.trim() : "";

            if ("ALL".equals(type)) {
                result.add(n);
            } else if (user != null) {
                if ("ROLE_STUDENTS".equals(type) && "STUDENT".equals(user.role)) {
                    result.add(n);
                } else if ("ROLE_PROFESSORS".equals(type) && "PROFESSOR".equals(user.role)) {
                    result.add(n);
                } else if ("ROLE_ADMINS".equals(type) && "ADMIN".equals(user.role)) {
                    result.add(n);
                } else if ("FACULTY".equals(type) && user.faculty != null && user.faculty.equalsIgnoreCase(val)) {
                    result.add(n);
                } else if ("SUBJECT".equals(type) || "FACILITY".equals(type)) {
                    result.add(n);
                }
            }
        }
        return result;
    }

    private String getNotificationTargetLabel(Notification n) {
        if (n == null || n.targetType == null) return "[📢 General]";
        String type = n.targetType.toUpperCase();
        String val = n.targetValue != null ? n.targetValue : "";

        switch (type) {
            case "ROLE_STUDENTS": return "[👨‍🎓 Todos los Estudiantes]";
            case "ROLE_PROFESSORS": return "[👨‍🏫 Todos los Profesores]";
            case "ROLE_ADMINS": return "[⚙️ Todos los Administradores]";
            case "FACULTY": return "[🏫 Facultad: " + val + "]";
            case "SUBJECT": return "[📚 Materia: " + val + "]";
            case "FACILITY": return "[🏛️ Instalación: " + val + "]";
            default: return "[📢 General (Todos los usuarios)]";
        }
    }

    private <T> void observeOnce(LiveData<T> liveData, Observer<T> observer) {
        liveData.observe(this, new Observer<T>() {
            @Override
            public void onChanged(T t) {
                if (t != null) {
                    liveData.removeObserver(this);
                    observer.onChanged(t);
                }
            }
        });
    }

    private void showCreateNotificationDialog(Notification existingNotif) {
        if (activeCreateNotificationDialog != null && activeCreateNotificationDialog.isShowing()) {
            activeCreateNotificationDialog.dismiss();
            activeCreateNotificationDialog = null;
        }

        User currentUser = viewModel.getCurrentUser().getValue();
        if (currentUser == null) return;

        boolean isEditing = existingNotif != null;
        String dialogTitle = isEditing ? "Editar Notificación" : "Crear Notificación";

        MaterialAlertDialogBuilder builder = DialogUtils.createMaterialDialog(this, dialogTitle);
        LinearLayout layout = DialogUtils.createDialogContainer(this);

        final EditText etTitle = DialogUtils.createStyledEditText(this, "Título de la Notificación", 0);
        if (isEditing && existingNotif.title != null) {
            etTitle.setText(existingNotif.title);
        }
        layout.addView(etTitle);

        final EditText etMessage = DialogUtils.createStyledEditText(this, "Mensaje / Contenido de la Notificación", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        if (isEditing && existingNotif.message != null) {
            etMessage.setText(existingNotif.message);
        }
        layout.addView(etMessage);

        final String[] targetType = {isEditing && existingNotif.targetType != null ? existingNotif.targetType : "ALL"};
        final String[] targetValue = {isEditing ? existingNotif.targetValue : null};

        String initialTargetLabel = "Destinatarios: " + getNotificationTargetLabel(existingNotif);
        final TextView tvTarget = DialogUtils.createDialogOptionButton(this, initialTargetLabel, false);
        layout.addView(tvTarget);

        tvTarget.setOnClickListener(v -> {
            String[] options = {
                "📢 General (Todos los usuarios)",
                "👨‍🎓 Todos los Estudiantes",
                "👨‍🏫 Todos los Profesores",
                "⚙️ Todos los Administradores",
                "🏫 Integrantes de una Facultad...",
                "📚 Integrantes de una Materia...",
                "🏛️ Integrantes de una Instalación..."
            };

            new MaterialAlertDialogBuilder(this)
                .setTitle("Seleccionar Destinatarios")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        targetType[0] = "ALL";
                        targetValue[0] = null;
                        DialogUtils.setOptionState(tvTarget, "Destinatarios: 📢 General (Todos)", false, this);
                    } else if (which == 1) {
                        targetType[0] = "ROLE_STUDENTS";
                        targetValue[0] = null;
                        DialogUtils.setOptionState(tvTarget, "Destinatarios: 👨‍🎓 Todos los Estudiantes", false, this);
                    } else if (which == 2) {
                        targetType[0] = "ROLE_PROFESSORS";
                        targetValue[0] = null;
                        DialogUtils.setOptionState(tvTarget, "Destinatarios: 👨‍🏫 Todos los Profesores", false, this);
                    } else if (which == 3) {
                        targetType[0] = "ROLE_ADMINS";
                        targetValue[0] = null;
                        DialogUtils.setOptionState(tvTarget, "Destinatarios: ⚙️ Todos los Administradores", false, this);
                    } else if (which == 4) {
                        observeOnce(viewModel.getAllFaculties(), faculties -> {
                            if (faculties != null && !faculties.isEmpty()) {
                                String[] facNames = faculties.stream().map(f -> f.name).toArray(String[]::new);
                                new MaterialAlertDialogBuilder(this)
                                    .setTitle("Seleccionar Facultad")
                                    .setItems(facNames, (d2, w2) -> {
                                        targetType[0] = "FACULTY";
                                        targetValue[0] = facNames[w2];
                                        DialogUtils.setOptionState(tvTarget, "Facultad: " + facNames[w2], false, this);
                                    })
                                    .setNegativeButton("Atrás", (d2, w2) -> tvTarget.performClick())
                                    .setOnCancelListener(d2 -> tvTarget.performClick())
                                    .show();
                            } else {
                                Toast.makeText(this, "No hay facultades registradas", Toast.LENGTH_SHORT).show();
                                tvTarget.performClick();
                            }
                        });
                    } else if (which == 5) {
                        observeOnce(viewModel.getAllSubjects(), subjects -> {
                            if (subjects != null && !subjects.isEmpty()) {
                                String[] subNames = subjects.stream().map(s -> s.name).toArray(String[]::new);
                                new MaterialAlertDialogBuilder(this)
                                    .setTitle("Seleccionar Materia")
                                    .setItems(subNames, (d2, w2) -> {
                                        targetType[0] = "SUBJECT";
                                        targetValue[0] = subNames[w2];
                                        DialogUtils.setOptionState(tvTarget, "Materia: " + subNames[w2], false, this);
                                    })
                                    .setNegativeButton("Atrás", (d2, w2) -> tvTarget.performClick())
                                    .setOnCancelListener(d2 -> tvTarget.performClick())
                                    .show();
                            } else {
                                Toast.makeText(this, "No hay materias registradas", Toast.LENGTH_SHORT).show();
                                tvTarget.performClick();
                            }
                        });
                    } else if (which == 6) {
                        observeOnce(viewModel.getAllFacilities(), facilities -> {
                            if (facilities != null && !facilities.isEmpty()) {
                                String[] facNames = facilities.stream().map(f -> f.name).toArray(String[]::new);
                                new MaterialAlertDialogBuilder(this)
                                    .setTitle("Seleccionar Instalación")
                                    .setItems(facNames, (d2, w2) -> {
                                        targetType[0] = "FACILITY";
                                        targetValue[0] = facNames[w2];
                                        DialogUtils.setOptionState(tvTarget, "Instalación: " + facNames[w2], false, this);
                                    })
                                    .setNegativeButton("Atrás", (d2, w2) -> tvTarget.performClick())
                                    .setOnCancelListener(d2 -> tvTarget.performClick())
                                    .show();
                            } else {
                                Toast.makeText(this, "No hay instalaciones registradas", Toast.LENGTH_SHORT).show();
                                tvTarget.performClick();
                            }
                        });
                    }
                }).show();
        });

        builder.setView(layout);
        builder.setPositiveButton(isEditing ? "Guardar" : "Enviar", null);
        builder.setNegativeButton("Cancelar", null);

        activeCreateNotificationDialog = builder.create();
        activeCreateNotificationDialog.setOnDismissListener(d -> activeCreateNotificationDialog = null);
        activeCreateNotificationDialog.show();

        activeCreateNotificationDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            hideKeyboard(etTitle);
            hideKeyboard(etMessage);
            hideKeyboard();
            String title = etTitle.getText().toString().trim();
            String message = etMessage.getText().toString().trim();

            boolean isValid = true;
            if (title.isEmpty()) {
                etTitle.setError("El título es obligatorio");
                isValid = false;
            }
            if (message.isEmpty()) {
                etMessage.setError("El mensaje es obligatorio");
                isValid = false;
            }

            if (isValid) {
                if (isEditing) {
                    existingNotif.title = title;
                    existingNotif.message = message;
                    existingNotif.targetType = targetType[0];
                    existingNotif.targetValue = targetValue[0];
                    viewModel.updateNotification(existingNotif, () -> {
                        Toast.makeText(this, "¡Notificación actualizada con éxito!", Toast.LENGTH_SHORT).show();
                        if (activeCreateNotificationDialog != null) {
                            activeCreateNotificationDialog.dismiss();
                        }
                        showNotificationsDialog();
                    });
                } else {
                    String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
                    Notification notif = new Notification(title, message, targetType[0], targetValue[0], currentUser.name, timestamp);

                    viewModel.insertNotification(notif, () -> {
                        Toast.makeText(this, "¡Notificación enviada con éxito!", Toast.LENGTH_SHORT).show();
                        if (activeCreateNotificationDialog != null) {
                            activeCreateNotificationDialog.dismiss();
                        }
                        showNotificationsDialog();
                    });
                }
            }
        });
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
