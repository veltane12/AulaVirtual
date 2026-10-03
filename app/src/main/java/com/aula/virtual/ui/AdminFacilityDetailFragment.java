package com.aula.virtual.ui;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.Facility;
import com.aula.virtual.databinding.FragmentDetailBinding;

public class AdminFacilityDetailFragment extends Fragment {
    private FragmentDetailBinding binding;
    private MainViewModel viewModel;
    private int facilityId;
    private Facility facility;

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
        facilityId = getArguments().getInt("facilityId");

        setupUI();
        observeViewModel();
        
        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        DialogUtils.showLoadingOverlay(binding.getRoot(), requireContext());
        viewModel.refreshData(); // Ensure we have the latest facilities
    }

    private void setupUI() {
        binding.tvDetailTitle.setText("Detalle Instal.");
        binding.etField1.setHint("Nombre (Ej: Aula 01)");
        binding.etField2.setHint("Tipo (Ej: Edificio, Laboratorio)");
        
        binding.etField3.setHint("Descripción");
        binding.etField3.setVisibility(View.VISIBLE);
        binding.etField3.setInputType(InputType.TYPE_CLASS_TEXT);
        binding.btnTogglePassword.setVisibility(View.GONE);

        binding.btnSave.setEnabled(false);
        binding.btnDelete.setEnabled(false);
        binding.btnManageGrades.setEnabled(false);

        binding.btnSave.setOnClickListener(v -> saveChanges());
        binding.btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        
        binding.btnManageGrades.setVisibility(View.GONE);

        DialogUtils.arrangeGridButtons(binding.layoutActionButtons);
    }

    private void observeViewModel() {
        viewModel.getAllFacilities().observe(getViewLifecycleOwner(), facilities -> {
            for (Facility f : facilities) {
                if (f.id == facilityId) {
                    facility = f;
                    binding.etField1.setText(f.name);
                    binding.etField2.setText(f.type);
                    binding.etField3.setText(f.description);
                    
                    binding.btnSave.setEnabled(true);
                    binding.btnDelete.setEnabled(true);
                    binding.btnManageGrades.setEnabled(true);

                    DialogUtils.hideLoadingOverlay(binding.getRoot());
                    break;
                }
            }
        });
    }

    private void saveChanges() {
        if (facility != null) {
            String name = binding.etField1.getText().toString();
            if (name.isEmpty()) {
                binding.etField1.setError("El nombre es obligatorio");
                return;
            }
            
            facility.name = name;
            facility.type = binding.etField2.getText().toString();
            facility.description = binding.etField3.getText().toString();
            
            viewModel.performOnlineAction(() -> {
                viewModel.updateFacility(facility);
                Toast.makeText(getContext(), "Instalación actualizada", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).popBackStack();
            });
        }
    }

    private void popBackToFacilityManagement() {
        NavController navController = Navigation.findNavController(requireView());
        if (navController.popBackStack(R.id.adminFacilityListFragment, false)) {
            return;
        }
        navController.popBackStack();
    }

    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar esta instalación? Se borrarán todos los horarios asociados.")
            .setPositiveButton("Eliminar todo", (dialog, which) -> {
                if (facility != null) {
                    viewModel.performOnlineAction(() -> {
                        viewModel.deleteFacility(facility.id);
                        Toast.makeText(getContext(), "Instalación eliminada", Toast.LENGTH_SHORT).show();
                        popBackToFacilityManagement();
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
