package com.aula.virtual.ui;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.Faculty;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentDetailBinding;
import java.util.ArrayList;
import java.util.List;

public class AdminDetailFragment extends Fragment {
    private FragmentDetailBinding binding;
    private MainViewModel viewModel;
    private User admin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        int adminId = getArguments().getInt("adminId");

        setupUI();
        observeViewModel();
        
        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        viewModel.fetchUserById(adminId);
    }

    private void setupUI() {
        binding.tvDetailTitle.setText("Detalle"); // Will be updated in observeViewModel
        binding.cardProfileImage.setVisibility(View.VISIBLE);
        
        binding.etField1.setHint("Nombre Completo");
        
        binding.etField2.setHint("Carnet");
        binding.etField2.setInputType(InputType.TYPE_CLASS_TEXT);
        binding.etField2.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15)});

        binding.etField3.setHint("Contraseña");
        binding.etField3.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        binding.etField3.setVisibility(View.VISIBLE);
        binding.layoutCopyToggles.setVisibility(View.VISIBLE);
        binding.btnTogglePassword.setVisibility(View.VISIBLE);
        binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);

        binding.btnTogglePassword.setOnClickListener(v -> {
            int inputType = binding.etField3.getInputType();
            if (inputType == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                binding.etField3.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility);
            } else {
                binding.etField3.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                binding.btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
            }
            binding.etField3.setSelection(binding.etField3.getText().length());
        });

        binding.tvPasswordStrength.setVisibility(View.VISIBLE);
        binding.etField3.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                ValidationUtils.PasswordStrength strength = ValidationUtils.getPasswordStrength(s.toString());
                binding.tvPasswordStrength.setText(strength.label);
                binding.tvPasswordStrength.setTextColor(strength.color);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Extended fields for Professors
        binding.etField4.setHint("Facultad (Click para cambiar)");
        binding.etField4.setFocusable(false);
        binding.etField4.setClickable(true);
        
        binding.btnSave.setOnClickListener(v -> saveChanges());
        binding.btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        binding.btnImpersonate.setOnClickListener(v -> impersonateUser());

        setupCopyButtons();
    }

    private void setupCopyButtons() {
        binding.btnCopyField1.setOnClickListener(v -> ClipboardUtils.copyToClipboard(getContext(), "Nombre", binding.etField1.getText().toString()));
        binding.btnCopyField2.setOnClickListener(v -> {
            String carnetBody = binding.etField2.getText().toString();
            String prefix = "";
            if (binding.tvRolePrefix.getVisibility() == View.VISIBLE) {
                prefix = binding.tvRolePrefix.getText().toString();
            }
            ClipboardUtils.copyToClipboard(getContext(), "Carnet", prefix + carnetBody);
        });
        binding.btnCopyField3.setOnClickListener(v -> ClipboardUtils.copyToClipboard(getContext(), "Contraseña", binding.etField3.getText().toString()));
        binding.btnCopyAddress.setOnClickListener(v -> ClipboardUtils.copyToClipboard(getContext(), "Dirección", binding.etAddress.getText().toString()));
        binding.btnCopyPersonalEmail.setOnClickListener(v -> ClipboardUtils.copyToClipboard(getContext(), "Correo Personal", binding.etPersonalEmail.getText().toString()));
    }

    private void impersonateUser() {
        if (admin != null) {
            viewModel.startImpersonation(admin);
            if ("PROFESSOR".equals(admin.role)) {
                Navigation.findNavController(requireView()).navigate(R.id.action_adminDetailFragment_to_professorHomeFragment);
            } else {
                // If it's a student (though students use StudentDetailFragment)
                Navigation.findNavController(requireView()).navigate(R.id.action_adminDetailFragment_to_studentHomeFragment);
            }
        }
    }

    private void observeViewModel() {
        viewModel.getSelectedUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                admin = user;
                
                // Dynamic Title based on role
                if ("PROFESSOR".equals(admin.role)) {
                    binding.tvDetailTitle.setText("Detalle Prof.");
                } else if ("ADMIN".equals(admin.role)) {
                    binding.tvDetailTitle.setText("Detalle Admin.");
                }

                binding.etField1.setText(admin.name);
                binding.etField2.setText(admin.carnet);
                binding.etField3.setText(admin.password);
                
                // Show extended fields for both Professors and Admins
                if ("PROFESSOR".equals(admin.role) || "ADMIN".equals(admin.role)) {
                    binding.etField4.setVisibility(View.VISIBLE);
                    binding.layoutAddress.setVisibility(View.VISIBLE);
                    binding.layoutPersonalEmail.setVisibility(View.VISIBLE);
                    binding.switchLockPhoto.setVisibility(View.VISIBLE);

                    // Label setup and Input Restrictions
                    binding.tvRolePrefix.setVisibility(View.VISIBLE);
                    if ("PROFESSOR".equals(admin.role)) {
                        binding.tvRolePrefix.setText("PROF");
                        binding.etField2.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
                        if (admin.carnet != null && admin.carnet.startsWith("PROF")) {
                            binding.etField2.setText(admin.carnet.substring(4));
                        } else {
                            binding.etField2.setText(admin.carnet);
                        }
                    } else {
                        binding.tvRolePrefix.setText("ADMIN");
                        binding.etField2.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
                        if (admin.carnet != null && admin.carnet.startsWith("ADMIN")) {
                            binding.etField2.setText(admin.carnet.substring(5));
                        } else {
                            binding.etField2.setText(admin.carnet);
                        }
                    }
                    binding.etField2.setInputType(InputType.TYPE_CLASS_NUMBER);
                    
                    binding.etField4.setText(admin.faculty != null ? admin.faculty : "");
                    binding.etAddress.setText(admin.address != null ? admin.address : "");
                    binding.etPersonalEmail.setText(admin.personal_email != null ? admin.personal_email : "");

                    // Si es Profesor o Administrador, bloquear la selección de facultad (es automática e inmutable)
                    binding.etField4.setEnabled(false);
                    binding.etField4.setFocusable(false);
                    binding.etField4.setClickable(false);
                    
                    boolean locked = admin.can_change_photo != null && admin.can_change_photo == 0;
                    binding.switchLockPhoto.setChecked(locked);

                    // Show copy/toggle buttons for both Admin and Professor management
                    binding.layoutCopyToggles.setVisibility(View.VISIBLE);

                    // Impersonation is only for non-master admins auditing professors
                    if ("PROFESSOR".equals(admin.role)) {
                        binding.btnImpersonate.setVisibility(View.VISIBLE);
                        binding.btnImpersonate.setText("Ver como Profesor");
                    } else {
                        binding.btnImpersonate.setVisibility(View.GONE);
                    }
                } else {
                    binding.tvRolePrefix.setVisibility(View.GONE);
                    binding.etField4.setVisibility(View.GONE);
                    binding.layoutAddress.setVisibility(View.GONE);
                    binding.layoutPersonalEmail.setVisibility(View.GONE);
                    binding.switchLockPhoto.setVisibility(View.GONE);
                    binding.btnImpersonate.setVisibility(View.GONE);
                }

                int paddingPx = (int) (16 * getResources().getDisplayMetrics().density);
                ImageUtils.setProfileImage(binding.ivProfileImageDetail, admin.profile_image, paddingPx);

                // Master Admin Protection
                if ("ADMIN12345".equals(admin.carnet)) {
                    binding.btnDelete.setVisibility(View.GONE);
                    binding.etField2.setEnabled(false); // Carnet cannot be changed
                }
            }
        });

        viewModel.getAllFaculties().observe(getViewLifecycleOwner(), faculties -> {
            if (faculties == null) return;
            // Filtrar para que la gestión de usuarios general NO liste Docencia ni Administrativa si no pertenecen a ese rol
            List<String> filteredNames = new ArrayList<>();
            for (Faculty f : faculties) {
                if (!f.name.equals("Docencia") && !f.name.equals("Administrativa")) {
                    filteredNames.add(f.name);
                }
            }
            String[] names = filteredNames.toArray(new String[0]);
            
            binding.etField4.setOnClickListener(v -> {
                if (admin != null && ("PROFESSOR".equals(admin.role) || "ADMIN".equals(admin.role))) return;
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Seleccionar Facultad")
                    .setItems(names, (dialog, which) -> {
                        binding.etField4.setText(names[which]);
                    }).show();
            });
        });
    }

    private void saveChanges() {
        if (admin != null) {
            String name = binding.etField1.getText().toString();
            String carnet = binding.etField2.getText().toString();
            String pass = binding.etField3.getText().toString();

            boolean isValid = true;
            if (name.isEmpty()) {
                binding.etField1.setError("El nombre es obligatorio");
                isValid = false;
            }
            if (carnet.isEmpty()) {
                binding.etField2.setError("El carnet es obligatorio");
                isValid = false;
            }
            if (ValidationUtils.getPasswordStrength(pass) == ValidationUtils.PasswordStrength.WEAK) {
                binding.tvPasswordStrength.setText("Contraseña muy débil");
                binding.tvPasswordStrength.setTextColor(0xFFFF4444);
                isValid = false;
            }

            if (isValid) {
                // Validation of lengths for the numeric section
                if ("PROFESSOR".equals(admin.role) && carnet.length() != 6) {
                    binding.etField2.setError("Debe ingresar exactamente 6 dígitos");
                    return;
                }
                if ("ADMIN".equals(admin.role) && carnet.length() != 5) {
                    binding.etField2.setError("Debe ingresar exactamente 5 dígitos");
                    return;
                }

                admin.name = name;
                
                // Construct full carnet based on prefix
                if ("PROFESSOR".equals(admin.role)) {
                    admin.carnet = "PROF" + carnet;
                } else if ("ADMIN".equals(admin.role)) {
                    admin.carnet = "ADMIN" + carnet;
                } else {
                    admin.carnet = carnet;
                }
                
                admin.password = pass;
                
                if ("PROFESSOR".equals(admin.role) || "ADMIN".equals(admin.role)) {
                    admin.faculty = binding.etField4.getText().toString();
                    admin.address = binding.etAddress.getText().toString();
                    admin.personal_email = binding.etPersonalEmail.getText().toString();
                    
                    boolean isLocked = binding.switchLockPhoto.isChecked();
                    admin.can_change_photo = isLocked ? 0 : 1;
                    if (isLocked) admin.profile_image = null;
                }

                viewModel.performOnlineAction(() -> {
                    viewModel.updateUser(admin);
                    Toast.makeText(getContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).popBackStack();
                });
            }
        }
    }

    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar este perfil administrativo?")
            .setPositiveButton("Eliminar", (dialog, which) -> {
                if (admin != null) {
                    viewModel.performOnlineAction(() -> {
                        viewModel.deleteUser(admin);
                        Toast.makeText(getContext(), "Administrador eliminado", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).popBackStack();
                    });
                }
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
