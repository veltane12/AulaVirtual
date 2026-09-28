package com.aula.virtual.ui;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.aula.virtual.R;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;
import com.aula.virtual.data.entity.Faculty;
import com.aula.virtual.data.entity.User;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.aula.virtual.databinding.FragmentAdminDashboardBinding;

public class AdminStudentListFragment extends Fragment {
    private FragmentAdminDashboardBinding binding;
    private MainViewModel viewModel;
    private StudentAdapter adapter;
    private List<User> allStudents = new ArrayList<>();
    private List<Faculty> availableFaculties = new ArrayList<>();

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

        binding.tvTitle.setText("Gestión de Alumnos");
        adapter = new StudentAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        setupSearch();

        viewModel.getAllStudents().observe(getViewLifecycleOwner(), students -> {
            allStudents = students;
            applyFilter();
        });

        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        viewModel.getAllFaculties().observe(getViewLifecycleOwner(), faculties -> {
            if (faculties != null) {
                availableFaculties = faculties;
            }
        });

        viewModel.refreshData(); // Force refresh when entering list

        adapter.setOnItemClickListener(student -> {
            Bundle args = new Bundle();
            args.putInt("studentId", student.id);
            Navigation.findNavController(view).navigate(R.id.action_adminStudentListFragment_to_studentDetailFragment, args);
        });

        binding.btnAdd.setOnClickListener(v -> showAddUserDialog());
    }

    private void setupSearch() {
        String[] options = {"Todo", "Carnet", "Nombre", "Facultad"};
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

        binding.spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilter();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void applyFilter() {
        String query = binding.etSearch.getText().toString().toLowerCase().trim();
        String filterType = binding.spinnerFilter.getSelectedItem().toString();

        if (query.isEmpty()) {
            adapter.setStudents(allStudents);
            return;
        }

        List<User> filtered = allStudents.stream().filter(s -> {
            if (s == null) return false;
            boolean match = false;
            String name = s.name != null ? s.name.toLowerCase() : "";
            String carnet = s.carnet != null ? s.carnet.toLowerCase() : "";
            String faculty = s.faculty != null ? s.faculty.toLowerCase() : "";

            if (filterType.equals("Todo")) {
                match = name.contains(query) || 
                        carnet.contains(query) || 
                        faculty.contains(query);
            } else if (filterType.equals("Carnet")) {
                match = carnet.contains(query);
            } else if (filterType.equals("Nombre")) {
                match = name.contains(query);
            } else if (filterType.equals("Facultad")) {
                match = faculty.contains(query);
            }
            return match;
        }).collect(Collectors.toList());

        adapter.setStudents(filtered);
    }

    private void showAddUserDialog() {
        if (availableFaculties == null || availableFaculties.isEmpty()) {
            Toast.makeText(getContext(), "Cree una facultad primero", Toast.LENGTH_SHORT).show();
            return;
        }
        showAddUserAlertDialog(availableFaculties);
    }

    private void showAddUserAlertDialog(List<Faculty> faculties) {
        MaterialAlertDialogBuilder builder = DialogUtils.createMaterialDialog(requireContext(), "Añadir Estudiante");
        LinearLayout layout = DialogUtils.createDialogContainer(requireContext());

        final EditText etName = DialogUtils.createStyledEditText(requireContext(), "Nombre Completo", 0);
        layout.addView(etName);

        final EditText etCarnet = DialogUtils.createStyledEditText(requireContext(), "Carnet (7 dígitos)", InputType.TYPE_CLASS_NUMBER);
        etCarnet.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
        layout.addView(etCarnet);

        final EditText etPass = DialogUtils.createStyledEditText(requireContext(), "Contraseña", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etPass);

        final TextView tvStrength = new TextView(getContext());
        tvStrength.setTextSize(12);
        layout.addView(tvStrength);

        etPass.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                ValidationUtils.PasswordStrength strength = ValidationUtils.getPasswordStrength(s.toString());
                tvStrength.setText(strength.label);
                tvStrength.setTextColor(strength.color);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        final TextView tvFaculty = new TextView(getContext());
        tvFaculty.setText("Seleccionar Facultad (Click aquí)");
        tvFaculty.setPadding(0, 20, 0, 20);
        tvFaculty.setTextColor(0xFF007BFF);
        layout.addView(tvFaculty);

        final String[] facultyNames;
        List<String> filteredNames = new ArrayList<>();
        for (Faculty f : faculties) {
            if (!f.name.equals("Docencia") && !f.name.equals("Administrativa")) {
                filteredNames.add(f.name);
            }
        }
        facultyNames = filteredNames.toArray(new String[0]);
        final String[] selectedFaculty = {null};

        tvFaculty.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Facultades")
                .setItems(facultyNames, (dialog, which) -> {
                    selectedFaculty[0] = facultyNames[which];
                    tvFaculty.setText("Facultad: " + selectedFaculty[0]);
                }).show();
        });

        builder.setView(layout);
        builder.setPositiveButton("Añadir", null);
        builder.setNegativeButton("Cancelar", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v1 -> {
            String name = etName.getText().toString();
            String carnet = etCarnet.getText().toString();
            String pass = etPass.getText().toString();
            
            boolean isValid = true;

            if (name.isEmpty()) {
                etName.setError("El nombre es obligatorio");
                isValid = false;
            }
            if (!carnet.matches("\\d{7}")) {
                etCarnet.setError("Carnet inválido (7 dígitos numéricos)");
                isValid = false;
            }
            if (ValidationUtils.getPasswordStrength(pass) == ValidationUtils.PasswordStrength.WEAK) {
                etPass.setError("Contraseña muy débil");
                isValid = false;
            }
            if (selectedFaculty[0] == null) {
                Toast.makeText(getContext(), "Seleccione una facultad", Toast.LENGTH_SHORT).show();
                isValid = false;
            }

            if (isValid) {
                String fullCarnet = "EST" + carnet;
                viewModel.performOnlineAction(() -> {
                    viewModel.insertUser(new User(fullCarnet, name, pass, "STUDENT", selectedFaculty[0]));
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
