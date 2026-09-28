package com.aula.virtual.ui;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.app.ProgressDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
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

        final EditText etNewPass = DialogUtils.createStyledEditText(requireContext(), "Nueva Contraseña", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNewPass);

        final EditText etConfirmPass = DialogUtils.createStyledEditText(requireContext(), "Confirmar Contraseña", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etConfirmPass);

        builder.setView(layout);
        builder.setPositiveButton("Actualizar", (dialog, which) -> {
            String newPass = etNewPass.getText().toString();
            if (newPass.equals(etConfirmPass.getText().toString()) && !newPass.isEmpty()) {
                viewModel.performOnlineAction(() -> {
                    currentUser.password = newPass;
                    viewModel.updateUser(currentUser);
                    Toast.makeText(getContext(), "Contraseña actualizada", Toast.LENGTH_SHORT).show();
                });
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
