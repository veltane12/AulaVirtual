package com.aula.virtual.ui;

import android.app.ProgressDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.aula.virtual.R;
import com.aula.virtual.data.RetrofitClient;
import com.aula.virtual.data.VirtualAulaRepository;
import com.aula.virtual.data.entity.Notification;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentSettingsAdvancedBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsAdvancedFragment extends Fragment {
    private FragmentSettingsAdvancedBinding binding;
    private MainViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsAdvancedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        setupDataModeSelector();
        setupBiometricAutofillSwitch();
        binding.btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        binding.btnDownloadOfflineData.setOnClickListener(v -> downloadOfflineData());
        binding.btnUploadOfflineData.setOnClickListener(v -> confirmUploadOfflineData());
        binding.btnClearOfflineData.setOnClickListener(v -> confirmClearOfflineData());
        binding.btnConfigureServerUrl.setOnClickListener(v -> showServerUrlDialog());

        User currentUser = viewModel.getCurrentUser().getValue();
        if (currentUser != null && !"ADMIN".equals(currentUser.role)) {
            RetrofitClient.setCustomUrl(requireContext(), RetrofitClient.BASE_URL);
        }

        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });
    }

    private void updateVisibilityForMode(String currentMode) {
        User currentUser = viewModel.getCurrentUser().getValue();
        boolean isAdmin = currentUser != null && "ADMIN".equals(currentUser.role);

        if (ThemeHelper.MODE_LOCAL.equals(currentMode)) {
            if (isAdmin) {
                binding.btnDownloadOfflineData.setVisibility(View.VISIBLE);
                binding.btnUploadOfflineData.setVisibility(View.VISIBLE);
                binding.btnClearOfflineData.setVisibility(View.VISIBLE);
            } else {
                binding.btnDownloadOfflineData.setVisibility(View.GONE);
                binding.btnUploadOfflineData.setVisibility(View.GONE);
                binding.btnClearOfflineData.setVisibility(View.GONE);
            }
            binding.btnConfigureServerUrl.setVisibility(View.GONE);
        } else {
            binding.btnDownloadOfflineData.setVisibility(View.GONE);
            binding.btnUploadOfflineData.setVisibility(View.GONE);
            binding.btnClearOfflineData.setVisibility(View.GONE);
            if (isAdmin) {
                binding.btnConfigureServerUrl.setVisibility(View.VISIBLE);
            } else {
                binding.btnConfigureServerUrl.setVisibility(View.GONE);
            }
        }

        if (currentUser != null && !"ADMIN".equals(currentUser.role)) {
            binding.layoutContactAdminContainer.setVisibility(View.VISIBLE);
            binding.btnContactAdmin.setOnClickListener(v -> showContactAdminDialog(currentUser));
        } else {
            binding.layoutContactAdminContainer.setVisibility(View.GONE);
        }
    }

    private void setupDataModeSelector() {
        String currentMode = ThemeHelper.getDataMode(requireContext());
        updateVisibilityForMode(currentMode);

        if (ThemeHelper.MODE_LOCAL.equals(currentMode)) {
            binding.toggleDataMode.check(R.id.btnModeLocal);
            binding.tvDataModeDescription.setText("Modo Local Activo: Operando con los datos almacenados dentro del dispositivo móvil sin depender del servidor.");
        } else {
            binding.toggleDataMode.check(R.id.btnModeServer);
            binding.tvDataModeDescription.setText("Modo Servidor Activo: Conexión obligatoria con el servidor para la consulta y el registro de datos.");
        }

        binding.toggleDataMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                String newMode = (checkedId == R.id.btnModeLocal) ? ThemeHelper.MODE_LOCAL : ThemeHelper.MODE_SERVER;
                if (!newMode.equals(ThemeHelper.getDataMode(requireContext()))) {
                    ThemeHelper.setDataMode(requireContext(), newMode);
                    if (ThemeHelper.MODE_LOCAL.equals(newMode)) {
                        Toast.makeText(getContext(), "Cambiado a Modo Local (Sin servidor)", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Cambiado a Modo Servidor. Verificando conexión...", Toast.LENGTH_SHORT).show();
                        RetrofitClient.resetClient();
                        viewModel.refreshData();
                    }
                    requireActivity().recreate();
                }
            }
        });
    }

    private void setupBiometricAutofillSwitch() {
        boolean isEnabled = ThemeHelper.isBiometricAutofillEnabled(requireContext());
        binding.switchBiometricAutofill.setChecked(isEnabled);
        binding.switchBiometricAutofill.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ThemeHelper.setBiometricAutofillEnabled(requireContext(), isChecked);
            String message = isChecked ? "Autocompletado de contraseñas activado" : "Autocompletado de contraseñas desactivado";
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });
    }

    private void showServerUrlDialog() {
        MaterialAlertDialogBuilder builder = DialogUtils.createMaterialDialog(requireContext(), "Configurar Servidor");
        LinearLayout layout = DialogUtils.createDialogContainer(requireContext());

        String currentUrl = RetrofitClient.getActiveUrl(requireContext());

        TextView tvHeader = new TextView(requireContext());
        tvHeader.setText("Selecciona el tipo de conexión:");
        tvHeader.setTextSize(14f);
        tvHeader.setTypeface(null, Typeface.BOLD);
        tvHeader.setPadding(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));
        layout.addView(tvHeader);

        // Input field for URL (masked for security)
        final EditText etUrl = DialogUtils.createStyledEditText(requireContext(), "Dirección del Servidor", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etUrl.setText(currentUrl);
        etUrl.setTransformationMethod(PasswordTransformationMethod.getInstance());

        // Option 1: Cloudflare Tunnel
        TextView btnPresetCloudflare = DialogUtils.createDialogOptionButton(requireContext(), 
            "🌐 Cloudflare Tunnel (Servidor Nube)");
        btnPresetCloudflare.setOnClickListener(v -> etUrl.setText(RetrofitClient.BASE_URL));
        layout.addView(btnPresetCloudflare);

        // Option 2: Android Emulator
        TextView btnPresetEmulator = DialogUtils.createDialogOptionButton(requireContext(), 
            "📱 Emulador de Android Studio");
        btnPresetEmulator.setOnClickListener(v -> etUrl.setText(RetrofitClient.EMULATOR_URL));
        layout.addView(btnPresetEmulator);

        // Option 3: Local Network
        TextView btnPresetLocalNet = DialogUtils.createDialogOptionButton(requireContext(), 
            "💻 Red Local Wi-Fi");
        btnPresetLocalNet.setOnClickListener(v -> {
            if (!etUrl.getText().toString().contains("192.168.")) {
                etUrl.setText("http://192.168.1.100:8000/");
            }
        });
        layout.addView(btnPresetLocalNet);

        TextView tvLabelEdit = new TextView(requireContext());
        tvLabelEdit.setText("Dirección del servidor:");
        tvLabelEdit.setTextSize(14f);
        tvLabelEdit.setTypeface(null, Typeface.BOLD);
        tvLabelEdit.setPadding(0, (int) (12 * getResources().getDisplayMetrics().density), 0, (int) (4 * getResources().getDisplayMetrics().density));
        layout.addView(tvLabelEdit);
        layout.addView(etUrl);

        TextView btnToggleMask = new TextView(requireContext());
        btnToggleMask.setText("👁️ Mostrar / Ocultar Dirección");
        btnToggleMask.setTextSize(12f);
        btnToggleMask.setTextColor(ContextCompat.getColor(requireContext(), R.color.bs_secondary));
        btnToggleMask.setPadding(0, (int) (4 * getResources().getDisplayMetrics().density), 0, (int) (8 * getResources().getDisplayMetrics().density));
        btnToggleMask.setOnClickListener(v -> {
            if (etUrl.getTransformationMethod() instanceof PasswordTransformationMethod) {
                etUrl.setTransformationMethod(null);
            } else {
                etUrl.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            if (etUrl.getText() != null) etUrl.setSelection(etUrl.getText().length());
        });
        layout.addView(btnToggleMask);

        builder.setView(layout);
        builder.setPositiveButton("Probar y Guardar", (dialog, which) -> {
            String newUrl = etUrl.getText().toString().trim();
            if (!newUrl.isEmpty()) {
                RetrofitClient.setCustomUrl(requireContext(), newUrl);
                Toast.makeText(getContext(), "Configuración guardada. Probando conexión con el servidor...", Toast.LENGTH_SHORT).show();
                viewModel.refreshData();
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
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
        tvStrength.setTextSize(12f);
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
        User currentUser = viewModel.getCurrentUser().getValue();
        if (ThemeHelper.isLocalMode(requireContext()) && currentUser != null && !"ADMIN".equals(currentUser.role)) {
            Toast.makeText(getContext(), "Acción no permitida: Los Profesores y Estudiantes no pueden descargar datos del servidor en Modo Local.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!viewModel.performOnlineAction(() -> {})) return;
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("⚠️ Confirmar Sincronización")
                .setMessage("ADVERTENCIA: Al descargar los datos del servidor, se BORRARÁN PRIMERO todos los datos locales actuales para evitar duplicados o incongruencias, y se establecerán únicamente los datos actualizados del servidor.\n\n¿Deseas continuar?")
                .setPositiveButton("Sí, Borrar y Sincronizar", (dialog, which) -> {
                    ProgressDialog progressDialog = new ProgressDialog(getContext());
                    progressDialog.setMessage("Sincronizando y descargando datos del servidor...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    viewModel.downloadAllDataForOffline(new VirtualAulaRepository.SyncCallback() {
                        @Override
                        public void onSuccess(String message) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                                    viewModel.refreshData();
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
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmClearOfflineData() {
        User currentUser = viewModel.getCurrentUser().getValue();
        if (ThemeHelper.isLocalMode(requireContext()) && currentUser != null && !"ADMIN".equals(currentUser.role)) {
            Toast.makeText(getContext(), "Acción no permitida: Los Profesores y Estudiantes no pueden eliminar datos de la aplicación en Modo Local.", Toast.LENGTH_LONG).show();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Eliminar Datos Descargados")
                .setMessage("¿Estás seguro de que deseas eliminar del dispositivo todos los datos descargados previamente del servidor?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    viewModel.clearAllOfflineData(new VirtualAulaRepository.SyncCallback() {
                        @Override
                        public void onSuccess(String message) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                                    viewModel.refreshData();
                                });
                            }
                        }

                        @Override
                        public void onError(String error) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                                });
                            }
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmUploadOfflineData() {
        User currentUser = viewModel.getCurrentUser().getValue();
        if (ThemeHelper.isLocalMode(requireContext()) && currentUser != null && !"ADMIN".equals(currentUser.role)) {
            Toast.makeText(getContext(), "Acción no permitida: Los Profesores y Estudiantes no pueden subir datos al servidor en Modo Local.", Toast.LENGTH_LONG).show();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("📤 Subir Registros Locales al Servidor")
                .setMessage("⚠️ PRECAUCIÓN Y ADVERTENCIA:\n\nTodos los registros actuales almacenados en el dispositivo (Modo Offline) se subirán y se SUMARÁN a los registros actuales del Servidor SQL en línea como NUEVOS REGISTROS.\n\nEsta acción registrará tus materias, facultades, instalaciones, usuarios, foros y notificaciones locales directamente en el servidor como entradas completamente nuevas. ¿Deseas continuar?")
                .setPositiveButton("Sí, Subir al Servidor", (dialog, which) -> uploadOfflineData())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void uploadOfflineData() {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Subiendo registros locales al servidor...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        viewModel.uploadAllOfflineDataToOnline(new VirtualAulaRepository.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                        viewModel.refreshData();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Error al subir registros: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void showContactAdminDialog(User user) {
        if (user == null || getContext() == null) return;

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

                viewModel.performOnlineAction(() -> {
                    viewModel.insertNotification(notif, () -> {
                        Toast.makeText(getContext(), "¡Reporte enviado exitosamente a los Administradores!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
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
