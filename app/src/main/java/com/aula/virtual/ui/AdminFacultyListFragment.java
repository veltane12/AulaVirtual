package com.aula.virtual.ui;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import com.aula.virtual.R;
import com.aula.virtual.data.entity.Faculty;
import com.aula.virtual.databinding.FragmentAdminDashboardBinding;

public class AdminFacultyListFragment extends Fragment {
    private FragmentAdminDashboardBinding binding;
    private MainViewModel viewModel;
    private FacultyAdapter adapter;
    private List<Faculty> allFaculties = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        binding.tvTitle.setText("Listado de Facultades");
        adapter = new FacultyAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        setupSearch();

        viewModel.getAllFaculties().observe(getViewLifecycleOwner(), faculties -> {
            allFaculties = faculties;
            applyFilter();
        });

        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        viewModel.refreshData();

        adapter.setOnItemClickListener(faculty -> {
            Bundle args = new Bundle();
            args.putInt("facultyId", faculty.id);
            Navigation.findNavController(view).navigate(R.id.action_adminFacultyListFragment_to_facultyDetailFragment, args);
        });

        binding.btnAdd.setOnClickListener(v -> showAddFacultyDialog());
    }

    private void setupSearch() {
        String[] options = {"Todo", "Nombre"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, options);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFilter.setAdapter(spinnerAdapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.spinnerFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                applyFilter();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void applyFilter() {
        String query = binding.etSearch.getText().toString().toLowerCase().trim();
        String filterType = binding.spinnerFilter.getSelectedItem() != null ? binding.spinnerFilter.getSelectedItem().toString() : "Todo";

        if (query.isEmpty()) {
            adapter.setFaculties(allFaculties);
            binding.tvRecordCount.setText(allFaculties.size() + (allFaculties.size() == 1 ? " registro" : " registros"));
            return;
        }

        List<Faculty> filtered = allFaculties.stream().filter(f -> {
            if (f == null) return false;
            String name = f.name != null ? f.name.toLowerCase() : "";
            String desc = f.description != null ? f.description.toLowerCase() : "";

            if (filterType.equals("Todo")) {
                return name.contains(query) || desc.contains(query);
            } else {
                return name.contains(query);
            }
        }).collect(Collectors.toList());

        adapter.setFaculties(filtered);
        binding.tvRecordCount.setText(filtered.size() + (filtered.size() == 1 ? " registro" : " registros"));
    }

    private void showAddFacultyDialog() {
        MaterialAlertDialogBuilder builder = DialogUtils.createMaterialDialog(requireContext(), "Añadir Facultad");
        LinearLayout layout = DialogUtils.createDialogContainer(requireContext());
        final EditText etName = DialogUtils.createStyledEditText(requireContext(), "Nombre de la Facultad", 0);
        layout.addView(etName);
        final EditText etDesc = DialogUtils.createStyledEditText(requireContext(), "Descripción", 0);
        layout.addView(etDesc);
        builder.setView(layout);
        builder.setPositiveButton("Añadir", null);
        builder.setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v1 -> {
            String name = etName.getText().toString();
            String desc = etDesc.getText().toString();

            if (name.isEmpty()) {
                etName.setError("El nombre es obligatorio");
            } else {
                viewModel.performOnlineAction(() -> {
                    viewModel.insertFaculty(new Faculty(name, desc));
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
