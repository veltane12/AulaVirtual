package com.aula.virtual.ui;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import com.aula.virtual.data.entity.Faculty;
import com.aula.virtual.databinding.FragmentDetailBinding;

public class FacultyDetailFragment extends Fragment {
    private FragmentDetailBinding binding;
    private MainViewModel viewModel;
    private Faculty faculty;

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
        int facultyId = getArguments().getInt("facultyId");

        setupUI();
        observeViewModel();
        
        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        viewModel.fetchFacultyById(facultyId);
    }

    private void setupUI() {
        binding.tvDetailTitle.setText("Detalle Facultad");
        binding.etField1.setHint("Nombre de la Facultad");
        binding.etField2.setHint("Descripción");

        binding.btnSave.setOnClickListener(v -> saveChanges());
        binding.btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        DialogUtils.arrangeGridButtons(binding.layoutActionButtons);
    }

    private void observeViewModel() {
        viewModel.getSelectedFaculty().observe(getViewLifecycleOwner(), fac -> {
            if (fac != null) {
                faculty = fac;
                binding.etField1.setText(faculty.name);
                binding.etField2.setText(faculty.description);

                if (isSystemFaculty(faculty.name)) {
                    binding.etField1.setEnabled(false);
                    binding.etField1.setFocusable(false);
                    binding.btnDelete.setVisibility(View.GONE);
                } else {
                    binding.etField1.setEnabled(true);
                    binding.etField1.setFocusable(true);
                    binding.etField1.setFocusableInTouchMode(true);
                    binding.btnDelete.setVisibility(View.VISIBLE);
                }
                DialogUtils.arrangeGridButtons(binding.layoutActionButtons);
            }
        });
    }

    private boolean isSystemFaculty(String name) {
        return name != null && ("Docencia".equalsIgnoreCase(name.trim()) || "Administrativa".equalsIgnoreCase(name.trim()));
    }

    private void saveChanges() {
        if (faculty != null) {
            if (!isSystemFaculty(faculty.name)) {
                String name = binding.etField1.getText().toString().trim();
                if (name.isEmpty()) {
                    binding.etField1.setError("El nombre es obligatorio");
                    return;
                }
                faculty.name = name;
            }
            
            faculty.description = binding.etField2.getText().toString().trim();
            viewModel.performOnlineAction(() -> {
                viewModel.updateFaculty(faculty);
                Toast.makeText(getContext(), "Facultad actualizada", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).popBackStack();
            });
        }
    }

    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar esta facultad? Esto puede afectar a las materias vinculadas.")
            .setPositiveButton("Eliminar", (dialog, which) -> {
                if (faculty != null) {
                    viewModel.performOnlineAction(() -> {
                        viewModel.deleteFaculty(faculty);
                        Toast.makeText(getContext(), "Facultad eliminada", Toast.LENGTH_SHORT).show();
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
