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
import com.aula.virtual.data.entity.Student;
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
    private StudentRecordAdapter studentRecordAdapter;
    private List<User> allEncargados = new ArrayList<>();
    private List<Student> allStudentRecords = new ArrayList<>();
    private List<Faculty> availableFaculties = new ArrayList<>();
    private boolean isEncargadoMode = true;

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

        String filterRole = (getArguments() != null) ? getArguments().getString("filterRole", "ENCARGADO") : "ENCARGADO";
        isEncargadoMode = "ENCARGADO".equals(filterRole);

        adapter = new StudentAdapter();
        studentRecordAdapter = new StudentRecordAdapter();

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        if (isEncargadoMode) {
            binding.recyclerView.setAdapter(adapter);
            binding.tvTitle.setText("Listado de Encargados");
            if (getActivity() != null) {
                TextView tvNavTitle = getActivity().findViewById(R.id.tvNavTitle);
                if (tvNavTitle != null) tvNavTitle.setText("Listado de Encargados");
            }
        } else {
            binding.recyclerView.setAdapter(studentRecordAdapter);
            binding.tvTitle.setText("Listado de Alumnos");
            if (getActivity() != null) {
                TextView tvNavTitle = getActivity().findViewById(R.id.tvNavTitle);
                if (tvNavTitle != null) tvNavTitle.setText("Listado de Alumnos");
            }
        }

        setupSearch();

        if (isEncargadoMode) {
            viewModel.getAllEncargados().observe(getViewLifecycleOwner(), users -> {
                allEncargados = users != null ? users : new ArrayList<>();
                applyFilter();
            });
        } else {
            viewModel.getAllStudentRecords().observe(getViewLifecycleOwner(), students -> {
                allStudentRecords = students != null ? students : new ArrayList<>();
                applyFilter();
            });
        }

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

        adapter.setOnItemClickListener(userItem -> {
            Bundle args = new Bundle();
            args.putInt("encargadoId", userItem.id);
            Navigation.findNavController(view).navigate(R.id.action_adminStudentListFragment_to_adminEncargadoDetailFragment, args);
        });

        studentRecordAdapter.setOnItemClickListener(studentItem -> {
            Bundle args = new Bundle();
            args.putInt("studentId", studentItem.id);
            Navigation.findNavController(view).navigate(R.id.action_adminStudentListFragment_to_adminEnrollmentListFragment, args);
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
        String filterType = binding.spinnerFilter.getSelectedItem() != null ? binding.spinnerFilter.getSelectedItem().toString() : "Todo";

        if (isEncargadoMode) {
            if (query.isEmpty()) {
                adapter.setStudents(allEncargados);
                binding.tvRecordCount.setText(allEncargados.size() + (allEncargados.size() == 1 ? " registro" : " registros"));
                return;
            }

            List<User> filtered = allEncargados.stream().filter(s -> {
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
            binding.tvRecordCount.setText(filtered.size() + (filtered.size() == 1 ? " registro" : " registros"));
        } else {
            if (query.isEmpty()) {
                studentRecordAdapter.setStudents(allStudentRecords);
                binding.tvRecordCount.setText(allStudentRecords.size() + (allStudentRecords.size() == 1 ? " registro" : " registros"));
                return;
            }

            List<Student> filtered = allStudentRecords.stream().filter(s -> {
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

            studentRecordAdapter.setStudents(filtered);
            binding.tvRecordCount.setText(filtered.size() + (filtered.size() == 1 ? " registro" : " registros"));
        }
    }

    private void showAddUserDialog() {
        if (availableFaculties == null || availableFaculties.isEmpty()) {
            Toast.makeText(getContext(), "Cree una facultad primero", Toast.LENGTH_SHORT).show();
            return;
        }
        showAddUserAlertDialog(availableFaculties);
    }

    private void showAddUserAlertDialog(List<Faculty> faculties) {
        String filterRole = (getArguments() != null) ? getArguments().getString("filterRole", "ENCARGADO") : "ENCARGADO";
        boolean isEncargado = "ENCARGADO".equals(filterRole);
        String dialogTitle = isEncargado ? "Añadir Encargado del Estudiante" : "Añadir Alumno";

        MaterialAlertDialogBuilder builder = DialogUtils.createMaterialDialog(requireContext(), dialogTitle);
        LinearLayout layout = DialogUtils.createDialogContainer(requireContext());

        final EditText etName = DialogUtils.createStyledEditText(requireContext(), "Nombre Completo", 0);
        layout.addView(etName);

        final EditText etCarnet = DialogUtils.createStyledEditText(requireContext(), "Carnet (7 dígitos)", InputType.TYPE_CLASS_NUMBER);
        etCarnet.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
        layout.addView(etCarnet);

        final EditText etPass = DialogUtils.createStyledEditText(requireContext(), "Contraseña", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        final TextView tvStrength = new TextView(getContext());

        if (isEncargado) {
            layout.addView(etPass);
            tvStrength.setTextSize(12);
            tvStrength.setVisibility(View.GONE);
            layout.addView(tvStrength);

            etPass.addTextChangedListener(new TextWatcher() {
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
        }

        final User[] selectedEncargado = {null};
        final TextView tvEncargado = DialogUtils.createDialogOptionButton(requireContext(), "Seleccionar Encargado (Obligatorio)...", true);
        if (!isEncargado) {
            layout.addView(tvEncargado);

            tvEncargado.setOnClickListener(v -> {
                List<User> list = viewModel.getAllEncargados().getValue();
                if (list == null || list.isEmpty()) {
                    Toast.makeText(getContext(), "No existen encargados registrados. Registre uno primero.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String[] names = list.stream().map(u -> u.name + " (" + u.carnet + ")").toArray(String[]::new);
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Encargado Obligatorio")
                        .setItems(names, (dialog, which) -> {
                            selectedEncargado[0] = list.get(which);
                            tvEncargado.setError(null);
                            DialogUtils.setOptionState(tvEncargado, "Encargado: " + selectedEncargado[0].name, false, requireContext());
                        }).show();
            });
        }

        final String[] selectedFaculty = {isEncargado ? "Encargados de Estudiantes" : null};
        final TextView tvFaculty = DialogUtils.createDialogOptionButton(requireContext(), isEncargado ? "Facultad: Encargados de Estudiantes" : "Seleccionar Facultad/Escuela...", !isEncargado);
        if (isEncargado) {
            tvFaculty.setEnabled(false);
        }
        layout.addView(tvFaculty);

        final String[] selectedGrade = {null};
        final TextView tvGrade = DialogUtils.createDialogOptionButton(requireContext(), "Seleccionar Grado y Nivel (Primaria 1-9 / Bachiller 1-3)...", true);
        final EditText etAddress = DialogUtils.createStyledEditText(requireContext(), "Dirección (Opcional)", 0);

        if (isEncargado) {
            layout.addView(etAddress);
        } else {
            layout.addView(tvGrade);
            final String[] gradeOptions = {
                "1º Primaria", "2º Primaria", "3º Primaria", "4º Primaria",
                "5º Primaria", "6º Primaria", "7º Primaria", "8º Primaria", "9º Primaria",
                "1º Bachillerato", "2º Bachillerato", "3º Bachillerato"
            };
            tvGrade.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Seleccionar Grado y Nivel")
                        .setItems(gradeOptions, (dialog, which) -> {
                            selectedGrade[0] = gradeOptions[which];
                            tvGrade.setError(null);
                            DialogUtils.setOptionState(tvGrade, "Grado: " + selectedGrade[0], false, requireContext());
                        }).show();
            });
        }

        final EditText etEmail = DialogUtils.createStyledEditText(requireContext(), "Email Personal (Opcional)", 0);
        if (isEncargado) {
            layout.addView(etEmail);
        }

        final String[] facultyNames;
        List<String> filteredNames = new ArrayList<>();
        for (Faculty f : faculties) {
            if (!f.name.equals("Docencia") && !f.name.equals("Administrativa") && !f.name.equals("Encargados de Estudiantes")) {
                filteredNames.add(f.name);
            }
        }
        facultyNames = filteredNames.toArray(new String[0]);

        tvFaculty.setOnClickListener(v -> {
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
            if (isEncargado && ValidationUtils.getPasswordStrength(pass) == ValidationUtils.PasswordStrength.WEAK) {
                etPass.setError("Contraseña muy débil");
                isValid = false;
            }
            if (selectedFaculty[0] == null) {
                tvFaculty.setError("Seleccione una facultad o escuela");
                isValid = false;
            }
            if (!isEncargado && selectedEncargado[0] == null) {
                tvEncargado.setError("Debe seleccionar un Encargado obligatoriamente");
                isValid = false;
            }
            if (!isEncargado && selectedGrade[0] == null) {
                tvGrade.setError("Seleccione un grado y nivel");
                isValid = false;
            }

            if (isValid) {
                if (isEncargado) {
                    String fullCarnet = "ENC" + carnet;
                    String address = etAddress.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    viewModel.performOnlineAction(() -> {
                        User newUser = new User(fullCarnet, name, pass, "ENCARGADO", selectedFaculty[0]);
                        newUser.address = address.isEmpty() ? null : address;
                        newUser.personal_email = email.isEmpty() ? null : email;
                        viewModel.insertUser(newUser);
                        dialog.dismiss();
                    });
                } else {
                    String fullCarnet = "EST" + carnet;
                    String grade = selectedGrade[0];
                    Student newStudent = new Student(fullCarnet, name, grade, selectedFaculty[0], selectedEncargado[0].id);
                    viewModel.performOnlineAction(() -> {
                        viewModel.insertStudent(newStudent, new retrofit2.Callback<Student>() {
                            @Override
                            public void onResponse(@NonNull retrofit2.Call<Student> call, @NonNull retrofit2.Response<Student> response) {
                                Toast.makeText(getContext(), "Alumno registrado exitosamente", Toast.LENGTH_SHORT).show();
                                viewModel.refreshData();
                                dialog.dismiss();
                            }

                            @Override
                            public void onFailure(@NonNull retrofit2.Call<Student> call, @NonNull Throwable t) {
                                Toast.makeText(getContext(), "Error al guardar alumno", Toast.LENGTH_SHORT).show();
                            }
                        });
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
