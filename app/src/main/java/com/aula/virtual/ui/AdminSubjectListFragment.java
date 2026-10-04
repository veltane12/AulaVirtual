package com.aula.virtual.ui;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
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
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;
import com.aula.virtual.data.entity.Faculty;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.databinding.FragmentAdminDashboardBinding;

public class AdminSubjectListFragment extends Fragment {
    private FragmentAdminDashboardBinding binding;
    private MainViewModel viewModel;
    private SubjectAdapter adapter;
    private List<Subject> allSubjects = new ArrayList<>();
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

        binding.tvTitle.setText("Listado de Materias");
        adapter = new SubjectAdapter();
        adapter.setManageButtonText("Gestionar");
        adapter.setShowDeleteButton(true);
        adapter.setOnDeleteClickListener(this::showDeleteSubjectDialog);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        setupSearch();

        viewModel.getAllSubjects().observe(getViewLifecycleOwner(), subjects -> {
            allSubjects = subjects;
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

        viewModel.refreshData();

        adapter.setOnItemClickListener(subject -> {
            Bundle args = new Bundle();
            args.putInt("subjectId", subject.id);
            Navigation.findNavController(view).navigate(R.id.action_adminSubjectListFragment_to_subjectBlogFragment, args);
        });

        binding.btnAdd.setOnClickListener(v -> showAddSubjectDialog());
    }

    private void setupSearch() {
        String[] options = {"Todo", "Nombre", "Facultad"};
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

        if (query.isEmpty()) {
            adapter.setSubjects(allSubjects);
            binding.tvRecordCount.setText(allSubjects.size() + (allSubjects.size() == 1 ? " registro" : " registros"));
            return;
        }

        List<Subject> filtered = allSubjects.stream().filter(s -> {
            if (s == null) return false;
            boolean match = false;
            String name = s.name != null ? s.name.toLowerCase() : "";
            String faculty = s.faculty != null ? s.faculty.toLowerCase() : "";

            if (filterType.equals("Todo")) {
                match = name.contains(query) || faculty.contains(query);
            } else if (filterType.equals("Nombre")) {
                match = name.contains(query);
            } else if (filterType.equals("Facultad")) {
                match = faculty.contains(query);
            }
            return match;
        }).collect(Collectors.toList());

        adapter.setSubjects(filtered);
        binding.tvRecordCount.setText(filtered.size() + (filtered.size() == 1 ? " registro" : " registros"));
    }

    private void showAddSubjectDialog() {
        if (availableFaculties == null || availableFaculties.isEmpty()) {
            Toast.makeText(getContext(), "Cree una facultad primero", Toast.LENGTH_SHORT).show();
            return;
        }
        showAddSubjectAlertDialog(availableFaculties);
    }

    private void showAddSubjectAlertDialog(List<Faculty> faculties) {
        MaterialAlertDialogBuilder builder = DialogUtils.createMaterialDialog(requireContext(), "Añadir Materia");
        LinearLayout layout = DialogUtils.createDialogContainer(requireContext());

        final EditText etName = DialogUtils.createStyledEditText(requireContext(), "Nombre de la Materia", 0);
        layout.addView(etName);

        final EditText etDesc = DialogUtils.createStyledEditText(requireContext(), "Descripción", 0);
        layout.addView(etDesc);

        final EditText etSection = DialogUtils.createStyledEditText(requireContext(), "Sección (Número del 1 al 9)", InputType.TYPE_CLASS_NUMBER);
        layout.addView(etSection);

        final TextView tvFaculty = DialogUtils.createDialogOptionButton(requireContext(), "Seleccionar Facultad...", true);
        layout.addView(tvFaculty);

        final String[] facultyNames = new String[faculties.size()];
        for (int i = 0; i < faculties.size(); i++) facultyNames[i] = faculties.get(i).name;
        final String[] selectedFaculty = {null};

        tvFaculty.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Facultades")
                .setItems(facultyNames, (dialog, which) -> {
                    selectedFaculty[0] = facultyNames[which];
                    tvFaculty.setError(null);
                    DialogUtils.setOptionState(tvFaculty, "Facultad: " + selectedFaculty[0], false, requireContext());
                }).show();
        });

        builder.setView(layout);
        builder.setPositiveButton("Añadir", null);
        builder.setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v1 -> {
            String name = etName.getText().toString();
            String desc = etDesc.getText().toString();
            String sectionStr = etSection.getText().toString().trim();

            boolean isValid = true;
            if (name.isEmpty()) {
                etName.setError("El nombre es obligatorio");
                isValid = false;
            }
            if (selectedFaculty[0] == null) {
                tvFaculty.setError("Seleccione una facultad");
                isValid = false;
            }
            
            String finalSection = "01";
            if (sectionStr.isEmpty()) {
                etSection.setError("La sección es obligatoria");
                isValid = false;
            } else {
                try {
                    int val = Integer.parseInt(sectionStr);
                    if (val < 1 || val > 9) {
                        etSection.setError("Debe ser un número entero entre 1 y 9");
                        isValid = false;
                    } else {
                        finalSection = "0" + val;
                    }
                } catch (NumberFormatException e) {
                    etSection.setError("Debe ser un número válido");
                    isValid = false;
                }
            }

            if (isValid) {
                String sectionParam = finalSection;
                viewModel.performOnlineAction(() -> {
                    Subject newSub = new Subject(name, desc, selectedFaculty[0], null);
                    newSub.section = sectionParam;
                    viewModel.insertSubject(newSub);
                    dialog.dismiss();
                });
            }
        });
    }

    private void showDeleteSubjectDialog(Subject subject) {
        if (subject == null) return;
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Borrar Materia")
            .setMessage("¿Estás seguro de eliminar la materia \"" + subject.name + "\"? Esta acción no se puede deshacer.")
            .setPositiveButton("Borrar", (dialog, which) -> {
                viewModel.performOnlineAction(() -> {
                    viewModel.deleteSubject(subject);
                    Toast.makeText(getContext(), "Materia eliminada", Toast.LENGTH_SHORT).show();
                    viewModel.refreshData();
                });
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
