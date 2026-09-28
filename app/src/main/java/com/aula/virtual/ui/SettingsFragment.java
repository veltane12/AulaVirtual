package com.aula.virtual.ui;

import androidx.appcompat.app.AlertDialog;

import com.aula.virtual.data.entity.Notification;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.aula.virtual.R;
import com.aula.virtual.data.VirtualAulaRepository;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentSettingsBinding;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private MainViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        
        binding.switchDarkMode.setChecked(ThemeHelper.isDarkMode(requireContext()));
        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ThemeHelper.setDarkMode(requireContext(), isChecked);
            requireActivity().recreate();
        });

        setupAccentColorSelector();
        setupNavbarPositionSelector();
        binding.btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        binding.btnDownloadOfflineData.setOnClickListener(v -> downloadOfflineData());

        User currentUser = viewModel.getCurrentUser().getValue();
        if (currentUser != null && !"ADMIN".equals(currentUser.role)) {
            binding.layoutContactAdminContainer.setVisibility(View.VISIBLE);
            binding.btnContactAdmin.setOnClickListener(v -> showContactAdminDialog(currentUser));
        } else {
            binding.layoutContactAdminContainer.setVisibility(View.GONE);
        }

        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });
    }

    private void setupAccentColorSelector() {
        String currentColor = ThemeHelper.getAccentColorName(requireContext());
        resetColorIcons();
        
        markSelected(currentColor);

        // Light row
        binding.btnBlueLight.setOnClickListener(v -> changeColor("BLUE_LIGHT"));
        binding.btnRedLight.setOnClickListener(v -> changeColor("RED_LIGHT"));
        binding.btnGreenLight.setOnClickListener(v -> changeColor("GREEN_LIGHT"));
        binding.btnPurpleLight.setOnClickListener(v -> changeColor("PURPLE_LIGHT"));
        binding.btnCyanLight.setOnClickListener(v -> changeColor("CYAN_LIGHT"));
        binding.btnYellowLight.setOnClickListener(v -> changeColor("YELLOW_LIGHT"));

        // Normal row
        binding.btnBlue.setOnClickListener(v -> changeColor("BLUE"));
        binding.btnRed.setOnClickListener(v -> changeColor("RED"));
        binding.btnGreen.setOnClickListener(v -> changeColor("GREEN"));
        binding.btnPurple.setOnClickListener(v -> changeColor("PURPLE"));
        binding.btnCyan.setOnClickListener(v -> changeColor("CYAN"));
        binding.btnYellow.setOnClickListener(v -> changeColor("YELLOW"));

        // Dark row
        binding.btnBlueDark.setOnClickListener(v -> changeColor("BLUE_DARK"));
        binding.btnRedDark.setOnClickListener(v -> changeColor("RED_DARK"));
        binding.btnGreenDark.setOnClickListener(v -> changeColor("GREEN_DARK"));
        binding.btnPurpleDark.setOnClickListener(v -> changeColor("PURPLE_DARK"));
        binding.btnCyanDark.setOnClickListener(v -> changeColor("CYAN_DARK"));
        binding.btnYellowDark.setOnClickListener(v -> changeColor("YELLOW_DARK"));
    }

    private void markSelected(String color) {
        MaterialButton btn = null;
        switch (color) {
            case "BLUE_LIGHT": btn = binding.btnBlueLight; break;
            case "BLUE": btn = binding.btnBlue; break;
            case "BLUE_DARK": btn = binding.btnBlueDark; break;
            case "RED_LIGHT": btn = binding.btnRedLight; break;
            case "RED": btn = binding.btnRed; break;
            case "RED_DARK": btn = binding.btnRedDark; break;
            case "GREEN_LIGHT": btn = binding.btnGreenLight; break;
            case "GREEN": btn = binding.btnGreen; break;
            case "GREEN_DARK": btn = binding.btnGreenDark; break;
            case "PURPLE_LIGHT": btn = binding.btnPurpleLight; break;
            case "PURPLE": btn = binding.btnPurple; break;
            case "PURPLE_DARK": btn = binding.btnPurpleDark; break;
            case "CYAN_LIGHT": btn = binding.btnCyanLight; break;
            case "CYAN": btn = binding.btnCyan; break;
            case "CYAN_DARK": btn = binding.btnCyanDark; break;
            case "YELLOW_LIGHT": btn = binding.btnYellowLight; break;
            case "YELLOW": btn = binding.btnYellow; break;
            case "YELLOW_DARK": btn = binding.btnYellowDark; break;
        }
        if (btn != null) {
            btn.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
            btn.setIconResource(R.drawable.ic_check);
            int colorInt = ThemeHelper.getSubjectColor(requireContext(), color);
            int tint = ThemeHelper.isColorLight(colorInt) ? 0xFF000000 : 0xFFFFFFFF;
            btn.setIconTint(ColorStateList.valueOf(tint));
        }
    }

    private void resetColorIcons() {
        binding.btnBlueLight.setIcon(null); binding.btnBlue.setIcon(null); binding.btnBlueDark.setIcon(null);
        binding.btnRedLight.setIcon(null); binding.btnRed.setIcon(null); binding.btnRedDark.setIcon(null);
        binding.btnGreenLight.setIcon(null); binding.btnGreen.setIcon(null); binding.btnGreenDark.setIcon(null);
        binding.btnPurpleLight.setIcon(null); binding.btnPurple.setIcon(null); binding.btnPurpleDark.setIcon(null);
        binding.btnCyanLight.setIcon(null); binding.btnCyan.setIcon(null); binding.btnCyanDark.setIcon(null);
        binding.btnYellowLight.setIcon(null); binding.btnYellow.setIcon(null); binding.btnYellowDark.setIcon(null);
    }

    private void changeColor(String colorName) {
        ThemeHelper.setAccentColor(requireContext(), colorName);
        requireActivity().recreate();
    }

    private void setupNavbarPositionSelector() {
        String currentPos = ThemeHelper.getNavbarPosition(requireContext());
        if (ThemeHelper.NAVBAR_POSITION_BOTTOM.equals(currentPos)) {
            binding.toggleNavbarPosition.check(R.id.btnNavPositionBottom);
        } else {
            binding.toggleNavbarPosition.check(R.id.btnNavPositionTop);
        }

        binding.toggleNavbarPosition.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                String newPos = (checkedId == R.id.btnNavPositionBottom) ? ThemeHelper.NAVBAR_POSITION_BOTTOM : ThemeHelper.NAVBAR_POSITION_TOP;
                if (!newPos.equals(ThemeHelper.getNavbarPosition(requireContext()))) {
                    ThemeHelper.setNavbarPosition(requireContext(), newPos);
                    requireActivity().recreate();
                }
            }
        });
    }

    private void showChangePasswordDialog() {
        User currentUser = viewModel.getCurrentUser().getValue();
        if (currentUser == null) return;

        MaterialAlertDialogBuilder builder = DialogUtils.createMaterialDialog(requireContext(), "Cambiar Contraseña");
        LinearLayout layout = DialogUtils.createDialogContainer(requireContext());

        final EditText etCurrentPass = DialogUtils.createStyledEditText(requireContext(), "Contraseña Actual", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etCurrentPass);

        final EditText etNewPass = DialogUtils.createStyledEditText(requireContext(), "Nueva Contraseña", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNewPass);

        final TextView tvStrength = new TextView(getContext());
        tvStrength.setTextSize(12);
        tvStrength.setVisibility(View.GONE);
        layout.addView(tvStrength);

        etNewPass.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    tvStrength.setVisibility(View.GONE);
                } else {
                    tvStrength.setVisibility(View.VISIBLE);
                    ValidationUtils.PasswordStrength strength = ValidationUtils.getPasswordStrength(s.toString());
                    tvStrength.setText(strength.label);
                    tvStrength.setTextColor(strength.color);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        final EditText etConfirmPass = DialogUtils.createStyledEditText(requireContext(), "Confirmar Nueva Contraseña", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etConfirmPass);

        builder.setView(layout);
        builder.setPositiveButton("Actualizar", null);
        builder.setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String currentPass = etCurrentPass.getText().toString();
            String newPass = etNewPass.getText().toString();
            String confirmPass = etConfirmPass.getText().toString();

            boolean isValid = true;

            if (!currentPass.equals(currentUser.password)) {
                etCurrentPass.setError("Contraseña actual incorrecta");
                isValid = false;
            }

            if (newPass.isEmpty()) {
                etNewPass.setError("Ingrese la nueva contraseña");
                isValid = false;
            } else if (ValidationUtils.getPasswordStrength(newPass) == ValidationUtils.PasswordStrength.WEAK) {
                etNewPass.setError("Contraseña demasiado débil");
                isValid = false;
            }

            if (!newPass.equals(confirmPass)) {
                etConfirmPass.setError("Las contraseñas no coinciden");
                isValid = false;
            }

            if (isValid) {
                viewModel.performOnlineAction(() -> {
                    currentUser.password = newPass;
                    viewModel.updateUser(currentUser);
                    Toast.makeText(getContext(), "Contraseña actualizada exitosamente", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }
        });
    }

    private void downloadOfflineData() {
        if (!viewModel.performOnlineAction(() -> {})) return;
        
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Descargando datos del servidor SQL...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        viewModel.downloadAllDataForOffline(new VirtualAulaRepository.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private static final long TWO_HOURS_MILLIS = 2 * 60 * 60 * 1000L; // 7,200,000 ms

    private void showContactAdminDialog(User user) {
        if (user == null || getContext() == null) return;

        SharedPreferences prefs = requireContext().getSharedPreferences("aula_virtual_prefs", Context.MODE_PRIVATE);
        long lastReportTime = prefs.getLong("KEY_LAST_REPORT_TIME_" + user.id, 0L);
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastReportTime;

        if (elapsed < TWO_HOURS_MILLIS) {
            long remainingMillis = TWO_HOURS_MILLIS - elapsed;
            long remainingMinutes = remainingMillis / (60 * 1000L);
            long hours = remainingMinutes / 60;
            long minutes = remainingMinutes % 60;

            String timeMsg = (hours > 0 ? hours + " hora(s) y " : "") + minutes + " minuto(s)";

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("⏳ Límite de Tiempo de Reporte")
                    .setMessage("Debes esperar " + timeMsg + " antes de enviar otro reporte a los Administradores.")
                    .setPositiveButton("Entendido", null)
                    .show();
            return;
        }

        MaterialAlertDialogBuilder builder = DialogUtils.createMaterialDialog(requireContext(), "Contactar con Administrador");
        LinearLayout layout = DialogUtils.createDialogContainer(requireContext());

        final EditText etTitle = DialogUtils.createStyledEditText(requireContext(), "Asunto / Título del Problema", 0);
        layout.addView(etTitle);

        final EditText etMessage = DialogUtils.createStyledEditText(requireContext(), "Detalles del problema o consulta", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        layout.addView(etMessage);

        builder.setView(layout);
        builder.setPositiveButton("Enviar Reporte", null);
        builder.setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String message = etMessage.getText().toString().trim();

            boolean isValid = true;
            if (title.isEmpty()) {
                etTitle.setError("El asunto es obligatorio");
                isValid = false;
            }
            if (message.isEmpty()) {
                etMessage.setError("El mensaje es obligatorio");
                isValid = false;
            }

            if (isValid) {
                String senderInfo = user.name + " (" + user.carnet + ")";
                Notification notif = new Notification(title, message, "SUPPORT", null, senderInfo, "");

                viewModel.insertNotification(notif, () -> {
                    prefs.edit().putLong("KEY_LAST_REPORT_TIME_" + user.id, System.currentTimeMillis()).apply();
                    Toast.makeText(getContext(), "¡Reporte enviado exitosamente a los Administradores!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
