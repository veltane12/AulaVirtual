package com.aula.virtual.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.aula.virtual.R;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentAdminDashboardBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminProfessorListFragment extends Fragment {
    private FragmentAdminDashboardBinding binding;
    private MainViewModel viewModel;
    private StudentAdapter adapter;
    private List<User> allProfessors = new ArrayList<>();

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

        binding.tvTitle.setText("Gestión de Profesores");
        adapter = new StudentAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        setupSearch();

        viewModel.getAllProfessors().observe(getViewLifecycleOwner(), professors -> {
            allProfessors = professors;
            applyFilter();
        });

        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        viewModel.refreshData();

        adapter.setOnItemClickListener(prof -> {
            Bundle args = new Bundle();
            args.putInt("adminId", prof.id); // Reuse adminId arg for Detail
            Navigation.findNavController(view).navigate(R.id.action_adminProfessorListFragment_to_adminDetailFragment, args);
        });

        binding.btnAdd.setOnClickListener(v -> showAddProfessorDialog());
    }

    private void setupSearch() {
        String[] options = {"Todo", "Carnet", "Nombre"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, options);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFilter.setAdapter(spinnerAdapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.spinnerFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { applyFilter(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void applyFilter() {
        String query = binding.etSearch.getText().toString().toLowerCase().trim();
        String filterType = binding.spinnerFilter.getSelectedItem().toString();

        if (query.isEmpty()) {
            adapter.setStudents(allProfessors);
            return;
        }

        List<User> filtered = allProfessors.stream().filter(p -> {
            if (p == null) return false;
            String name = p.name != null ? p.name.toLowerCase() : "";
            String carnet = p.carnet != null ? p.carnet.toLowerCase() : "";

            if (filterType.equals("Todo")) {
                return name.contains(query) || carnet.contains(query);
            } else if (filterType.equals("Carnet")) {
                return carnet.contains(query);
            } else {
                return name.contains(query);
            }
        }).collect(Collectors.toList());

        adapter.setStudents(filtered);
    }

    private void showAddProfessorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Añadir Profesor");
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText etName = new EditText(getContext());
        etName.setHint("Nombre Completo");
        layout.addView(etName);

        final EditText etCarnet = new EditText(getContext());
        etCarnet.setHint("Ingrese los 6 dígitos numéricos del carnet");
        etCarnet.setInputType(InputType.TYPE_CLASS_NUMBER);
        etCarnet.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        layout.addView(etCarnet);

        final EditText etPass = new EditText(getContext());
        etPass.setHint("Contraseña");
        etPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etPass);

        builder.setView(layout);
        builder.setPositiveButton("Añadir", null);
        builder.setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v1 -> {
            String name = etName.getText().toString().trim();
            String carnet = etCarnet.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            boolean isValid = true;
            if (name.isEmpty()) {
                etName.setError("El nombre es obligatorio");
                isValid = false;
            }
            if (carnet.length() != 6) {
                etCarnet.setError("Debe ingresar exactamente 6 dígitos");
                isValid = false;
            }
            if (pass.isEmpty()) {
                etPass.setError("La contraseña es obligatoria");
                isValid = false;
            }

            if (isValid) {
                String fullCarnet = "PROF" + carnet;
                viewModel.performOnlineAction(() -> {
                    viewModel.insertUser(new User(fullCarnet, name, pass, "PROFESSOR", "Docencia"));
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
