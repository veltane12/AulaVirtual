package com.aula.virtual.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.Faculty;
import com.aula.virtual.data.entity.Student;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentAdminDashboardBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminEncargadoDetailFragment extends Fragment {
    private FragmentAdminDashboardBinding binding;
    private MainViewModel viewModel;
    private User encargado;
    private StudentRecordAdapter adapter;
    private List<Student> encargadoStudents = new ArrayList<>();
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

        int encargadoId = getArguments() != null ? getArguments().getInt("encargadoId", -1) : -1;

        binding.tvTitle.setText("Alumnos encargados");
        adapter = new StudentRecordAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        setupSearch();

        viewModel.getAllFaculties().observe(getViewLifecycleOwner(), faculties -> {
            if (faculties != null) availableFaculties = faculties;
        });

        if (encargadoId != -1) {
            viewModel.fetchUserById(encargadoId);
            viewModel.getSelectedUser().observe(getViewLifecycleOwner(), user -> {
                if (user != null) {
                    encargado = user;
                    binding.tvTitle.setText("Alumnos encargados");
                    binding.tvSubtitle.setText("Encargado: " + user.name);
                    binding.tvSubtitle.setVisibility(View.VISIBLE);
                    loadEncargadoStudents(user.id);

                    binding.btnHeaderDetail.setVisibility(View.VISIBLE);
                    binding.btnHeaderDetail.setIconResource(R.drawable.ic_carnet);
                    binding.btnHeaderDetail.setOnClickListener(v -> {
                        Bundle args = new Bundle();
                        args.putInt("adminId", user.id);
                        Navigation.findNavController(requireView()).navigate(R.id.action_adminEncargadoDetailFragment_to_adminDetailFragment, args);
                    });
                }
            });
        }

        binding.btnAdd.setOnClickListener(v -> {
            if (encargado != null) {
                showAddStudentDialog(encargado);
            }
        });

        adapter.setOnItemClickListener(student -> {
            Bundle args = new Bundle();
            args.putInt("studentId", student.id);
            Navigation.findNavController(requireView()).navigate(R.id.action_adminEncargadoDetailFragment_to_adminEnrollmentListFragment, args);
        });
    }

    private void setupSearch() {
        String[] options = {"Todo", "Carnet", "Nombre", "Facultad"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, options);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFilter.setAdapter(spinnerAdapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { applyFilter(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadEncargadoStudents(int encargadoId) {
        viewModel.fetchStudentsForEncargado(encargadoId);
        viewModel.getMyStudents().observe(getViewLifecycleOwner(), students -> {
            encargadoStudents = students != null ? students : new ArrayList<>();
            applyFilter();
        });
    }

    private void applyFilter() {
        String query = binding.etSearch.getText().toString().toLowerCase().trim();
        String filterType = binding.spinnerFilter.getSelectedItem() != null ? binding.spinnerFilter.getSelectedItem().toString() : "Todo";

        if (query.isEmpty()) {
            adapter.setStudents(encargadoStudents);
            binding.tvRecordCount.setText(encargadoStudents.size() + (encargadoStudents.size() == 1 ? " alumno" : " alumnos"));
            return;
        }

        List<Student> filtered = encargadoStudents.stream().filter(s -> {
            if (s == null) return false;
            String name = s.name != null ? s.name.toLowerCase() : "";
            String carnet = s.carnet != null ? s.carnet.toLowerCase() : "";
            String faculty = s.faculty != null ? s.faculty.toLowerCase() : "";

            if (filterType.equals("Todo")) {
                return name.contains(query) || carnet.contains(query) || faculty.contains(query);
            } else if (filterType.equals("Carnet")) {
                return carnet.contains(query);
            } else if (filterType.equals("Nombre")) {
                return name.contains(query);
            } else if (filterType.equals("Facultad")) {
                return faculty.contains(query);
            }
            return false;
        }).collect(Collectors.toList());

        adapter.setStudents(filtered);
        binding.tvRecordCount.setText(filtered.size() + (filtered.size() == 1 ? " alumno" : " alumnos"));
    }

    private void showAddStudentDialog(User targetEncargado) {
        MaterialAlertDialogBuilder builder = DialogUtils.createMaterialDialog(requireContext(), "Añadir Alumno a Cargo");
        LinearLayout layout = DialogUtils.createDialogContainer(requireContext());

        final TextView tvEncargadoFixed = DialogUtils.createDialogOptionButton(requireContext(), "Encargado Obligatorio: " + targetEncargado.name, false);
        tvEncargadoFixed.setEnabled(false);
        layout.addView(tvEncargadoFixed);

        final EditText etName = DialogUtils.createStyledEditText(requireContext(), "Nombre Completo del Alumno", 0);
        layout.addView(etName);

        final EditText etCarnet = DialogUtils.createStyledEditText(requireContext(), "Carnet del Alumno (7 dígitos)", InputType.TYPE_CLASS_NUMBER);
        etCarnet.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
        layout.addView(etCarnet);

        final String[] gradeOptions = {
            "1º Primaria", "2º Primaria", "3º Primaria", "4º Primaria",
            "5º Primaria", "6º Primaria", "7º Primaria", "8º Primaria", "9º Primaria",
            "1º Bachillerato", "2º Bachillerato", "3º Bachillerato"
        };
        final String[] selectedGrade = {null};
        final TextView tvGrade = DialogUtils.createDialogOptionButton(requireContext(), "Seleccionar Grado y Nivel (Primaria 1-9 / Bachiller 1-3)...", true);
        tvGrade.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Seleccionar Grado y Nivel")
                    .setItems(gradeOptions, (dialog, which) -> {
                        selectedGrade[0] = gradeOptions[which];
                        tvGrade.setError(null);
                        DialogUtils.setOptionState(tvGrade, "Grado: " + selectedGrade[0], false, requireContext());
                    }).show();
        });
        layout.addView(tvGrade);

        final TextView tvFaculty = DialogUtils.createDialogOptionButton(requireContext(), "Seleccionar Escuela/Facultad...", true);
        layout.addView(tvFaculty);

        final String[] facultyNames;
        List<String> filteredNames = new ArrayList<>();
        for (Faculty f : availableFaculties) {
            if (!f.name.equals("Docencia") && !f.name.equals("Administrativa") && !f.name.equals("Encargados de Estudiantes")) {
                filteredNames.add(f.name);
            }
        }
        facultyNames = filteredNames.toArray(new String[0]);
        final String[] selectedFaculty = {null};

        tvFaculty.setOnClickListener(v -> {
            if (facultyNames.length == 0) {
                Toast.makeText(getContext(), "No hay facultades académicas disponibles", Toast.LENGTH_SHORT).show();
                return;
            }
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Facultades/Escuelas")
                .setItems(facultyNames, (dialog, which) -> {
                    selectedFaculty[0] = facultyNames[which];
                    tvFaculty.setError(null);
                    DialogUtils.setOptionState(tvFaculty, "Escuela: " + selectedFaculty[0], false, requireContext());
                }).show();
        });

        builder.setView(layout);
        builder.setPositiveButton("Añadir", null);
        builder.setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v1 -> {
            String name = etName.getText().toString().trim();
            String carnet = etCarnet.getText().toString().trim();

            boolean isValid = true;

            if (name.isEmpty()) {
                etName.setError("El nombre es obligatorio");
                isValid = false;
            }
            if (!carnet.matches("\\d{7}")) {
                etCarnet.setError("Carnet inválido (7 dígitos numéricos)");
                isValid = false;
            }
            if (selectedGrade[0] == null) {
                tvGrade.setError("Seleccione un grado y nivel");
                isValid = false;
            }
            if (selectedFaculty[0] == null) {
                tvFaculty.setError("Seleccione una facultad o escuela");
                isValid = false;
            }

            if (isValid) {
                String fullCarnet = "EST" + carnet;
                String grade = selectedGrade[0];
                Student newStudent = new Student(fullCarnet, name, grade, selectedFaculty[0], targetEncargado.id);
                viewModel.performOnlineAction(() -> {
                    viewModel.insertStudent(newStudent, new retrofit2.Callback<Student>() {
                        @Override
                        public void onResponse(retrofit2.Call<Student> call, retrofit2.Response<Student> response) {
                            Toast.makeText(getContext(), "Estudiante registrado exitosamente", Toast.LENGTH_SHORT).show();
                            loadEncargadoStudents(targetEncargado.id);
                            dialog.dismiss();
                        }

                        @Override
                        public void onFailure(retrofit2.Call<Student> call, Throwable t) {
                            Toast.makeText(getContext(), "Error al guardar estudiante", Toast.LENGTH_SHORT).show();
                        }
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
