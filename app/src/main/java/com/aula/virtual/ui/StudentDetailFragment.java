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
import android.widget.TextView;
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

public class StudentDetailFragment extends Fragment {
    private FragmentDetailBinding binding;
    private MainViewModel viewModel;
    private User student;

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

        int studentId = getArguments().getInt("studentId");
        
        setupUI();
        observeViewModel();
        
        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        DialogUtils.showLoadingOverlay((ViewGroup) binding.getRoot(), requireContext());
        viewModel.fetchUserById(studentId);
    }

    private void setupUI() {
        binding.tvDetailTitle.setText("Detalle Est.");
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

        binding.etField4.setHint("Facultad (Click para cambiar)");
        binding.etField4.setFocusable(false);
        binding.etField4.setClickable(true);
        binding.etField4.setVisibility(View.VISIBLE);

        binding.layoutAddress.setVisibility(View.VISIBLE);
        binding.layoutPersonalEmail.setVisibility(View.VISIBLE);
        binding.switchLockPhoto.setVisibility(View.VISIBLE);
        
        binding.btnManageGrades.setVisibility(View.VISIBLE);
        binding.btnImpersonate.setVisibility(View.GONE);

        binding.btnSave.setEnabled(false);
        binding.btnDelete.setEnabled(false);
        binding.btnManageGrades.setEnabled(false);

        binding.btnSave.setOnClickListener(v -> saveChanges());
        binding.btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        binding.btnManageGrades.setOnClickListener(v -> navigateToGrades());
        binding.cardProfileImage.setOnClickListener(v -> showImpersonateConfirmationDialog());

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

    private void observeViewModel() {
        viewModel.getSelectedUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null && binding != null) {
                student = user;
                binding.etField1.setText(student.name != null ? student.name : "");
                
                binding.tvRolePrefix.setVisibility(View.VISIBLE);
                binding.tvRolePrefix.setText("EST");
                binding.etField2.setInputType(InputType.TYPE_CLASS_NUMBER);
                binding.etField2.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
                
                if (student.carnet != null && student.carnet.startsWith("EST")) {
                    binding.etField2.setText(student.carnet.substring(3));
                } else {
                    binding.etField2.setText(student.carnet != null ? student.carnet : "");
                }
                
                binding.etField3.setText(student.password != null ? student.password : "");
                binding.etField4.setText(student.faculty != null ? student.faculty : "");
                binding.etAddress.setText(student.address != null ? student.address : "");
                binding.etPersonalEmail.setText(student.personal_email != null ? student.personal_email : "");
                
                boolean locked = student.can_change_photo != null && student.can_change_photo == 0;
                binding.switchLockPhoto.setChecked(locked);

                binding.layoutProfileHeader.setVisibility(View.VISIBLE);
                binding.cardImpersonateIndicator.setVisibility(View.VISIBLE);

                int paddingPx = (int) (16 * getResources().getDisplayMetrics().density);
                ImageUtils.setProfileImage(binding.ivProfileImageDetail, student.profile_image, paddingPx);

                binding.btnSave.setEnabled(true);
                binding.btnDelete.setEnabled(true);
                binding.btnManageGrades.setEnabled(true);

                DialogUtils.arrangeGridButtons(binding.layoutActionButtons);
                DialogUtils.hideLoadingOverlay(binding.getRoot());
            }
        });

        viewModel.getAllFaculties().observe(getViewLifecycleOwner(), faculties -> {
            if (faculties == null) return;
            List<String> filteredNames = new ArrayList<>();
            for (Faculty f : faculties) {
                if (!f.name.equals("Docencia") && !f.name.equals("Administrativa")) {
                    filteredNames.add(f.name);
                }
            }
            String[] names = filteredNames.toArray(new String[0]);
            
            binding.etField4.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Seleccionar Facultad")
                    .setItems(names, (dialog, which) -> {
                        binding.etField4.setText(names[which]);
                    }).show();
            });
        });
    }

    private void saveChanges() {
        if (student != null) {
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
                if (carnet.length() != 7) {
                    binding.etField2.setError("Debe ingresar exactamente 7 dígitos");
                    return;
                }
                student.name = name;
                student.carnet = "EST" + carnet;
                student.password = pass;
                student.faculty = binding.etField4.getText().toString();
                student.address = binding.etAddress.getText().toString();
                student.personal_email = binding.etPersonalEmail.getText().toString();
                
                boolean isLocked = binding.switchLockPhoto.isChecked();
                student.can_change_photo = isLocked ? 0 : 1;
                
                // Automatically remove photo if locking for inappropriate content
                if (isLocked) {
                    student.profile_image = null;
                }
                
                viewModel.performOnlineAction(() -> {
                    viewModel.updateUser(student);
                    Toast.makeText(getContext(), "Cambios guardados", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).popBackStack();
                });
            }
        }
    }

    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar a este estudiante?")
            .setPositiveButton("Eliminar", (dialog, which) -> {
                if (student != null) {
                    viewModel.performOnlineAction(() -> {
                        viewModel.deleteUser(student);
                        Toast.makeText(getContext(), "Estudiante eliminado", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).popBackStack();
                    });
                }
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void navigateToGrades() {
        if (student == null) return;
        Bundle args = new Bundle();
        args.putInt("studentId", student.id);
        Navigation.findNavController(requireView()).navigate(R.id.action_studentDetailFragment_to_adminEnrollmentListFragment, args);
    }

    private void showImpersonateConfirmationDialog() {
        if (student == null) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Acceder como Estudiante")
                .setMessage("¿Deseas ingresar a la aplicación utilizando el perfil y menú de " + student.name + "?")
                .setPositiveButton("Acceder", (dialog, which) -> impersonateStudent())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void impersonateStudent() {
        if (student != null) {
            viewModel.startImpersonation(student);
            Navigation.findNavController(requireView()).navigate(R.id.action_studentDetailFragment_to_studentHomeFragment);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
