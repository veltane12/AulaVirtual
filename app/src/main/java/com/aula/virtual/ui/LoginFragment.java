package com.aula.virtual.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentLoginBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;
import java.util.concurrent.Executor;

public class LoginFragment extends Fragment {
    private FragmentLoginBinding binding;
    private MainViewModel viewModel;
    private CredentialsManager credentialsManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        viewModel.logout();
        credentialsManager = new CredentialsManager(requireContext());

        binding.btnLogin.setOnClickListener(v -> {
            String carnet = binding.etCarnet.getText().toString();
            String password = binding.etPassword.getText().toString();

            if (carnet.isEmpty()) {
                Toast.makeText(getContext(), "El carné es obligatorio", Toast.LENGTH_SHORT).show();
                return;
            }

            performLogin(carnet, password, binding.cbRemember.isChecked());
        });

        binding.btnSavedAccounts.setOnClickListener(v -> showSavedAccountsDialog());
    }

    private void performLogin(String carnet, String password, boolean remember) {
        viewModel.logout();
        viewModel.login(carnet, password, new MainViewModel.LoginCallback() {
            @Override
            public void onSuccess(User user) {
                if (remember) {
                    credentialsManager.saveCredentials(carnet, password);
                }
                
                if (getActivity() == null || getView() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    if (getView() == null || Navigation.findNavController(getView()).getCurrentDestination() == null || 
                        Navigation.findNavController(getView()).getCurrentDestination().getId() != R.id.loginFragment) {
                        return;
                    }
                    if ("ADMIN".equals(user.role)) {
                        Navigation.findNavController(getView()).navigate(R.id.action_loginFragment_to_adminHomeFragment);
                    } else if ("PROFESSOR".equals(user.role)) {
                        Navigation.findNavController(getView()).navigate(R.id.action_loginFragment_to_professorHomeFragment);
                    } else {
                        Navigation.findNavController(getView()).navigate(R.id.action_loginFragment_to_studentHomeFragment);
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (getContext() != null) Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showSavedAccountsDialog() {
        List<String> accounts = credentialsManager.getRememberedAccounts();
        if (accounts.isEmpty()) {
            Toast.makeText(getContext(), "No hay cuentas guardadas", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = accounts.toArray(new String[0]);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Seleccionar cuenta")
                .setItems(items, (dialog, which) -> {
                    String selectedCarnet = items[which];
                    authenticateBiometrically(selectedCarnet);
                })
                .show();
    }

    private void authenticateBiometrically(String carnet) {
        Executor executor = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                String password = credentialsManager.getPassword(carnet);
                if (password != null) {
                    performLogin(carnet, password, false);
                }
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getContext(), "Autenticación fallida: " + errString, Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Inicio de sesión biométrico")
                .setSubtitle("Usa tu huella o rostro para entrar a " + carnet)
                .setNegativeButtonText("Cancelar")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
