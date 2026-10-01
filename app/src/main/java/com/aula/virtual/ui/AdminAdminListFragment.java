package com.aula.virtual.ui;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
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
import com.aula.virtual.R;
import com.aula.virtual.data.entity.Faculty;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentAdminDashboardBinding;

public class AdminAdminListFragment extends Fragment {
    private FragmentAdminDashboardBinding binding;
    private MainViewModel viewModel;
    private StudentAdapter adapter;
    private List<User> allAdmins = new ArrayList<>();
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

        binding.tvTitle.setText("Gestión de Administradores");
        adapter = new StudentAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        setupSearch();

        viewModel.getAllAdmins().observe(getViewLifecycleOwner(), admins -> {
            allAdmins = admins;
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

        adapter.setOnItemClickListener(admin -> {
            Bundle args = new Bundle();
            args.putInt("adminId", admin.id);
            Navigation.findNavController(view).navigate(R.id.action_adminAdminListFragment_to_adminDetailFragment, args);
        });

        binding.btnAdd.setOnClickListener(v -> showAddAdminDialog());
    }

    private void setupSearch() {
        String[] options = {"Todo", "Carnet", "Nombre"};
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
            adapter.setStudents(allAdmins);
            binding.tvRecordCount.setText(allAdmins.size() + (allAdmins.size() == 1 ? " registro" : " registros"));
            return;
        }

        List<User> filtered = allAdmins.stream().filter(a -> {
            if (a == null) return false;
            String name = a.name != null ? a.name.toLowerCase() : "";
            String carnet = a.carnet != null ? a.carnet.toLowerCase() : "";

            if (filterType.equals("Todo")) {
                return name.contains(query) || carnet.contains(query);
            } else if (filterType.equals("Carnet")) {
                return carnet.contains(query);
            } else {
                return name.contains(query);
            }
        }).collect(Collectors.toList());

        adapter.setStudents(filtered);
        binding.tvRecordCount.setText(filtered.size() + (filtered.size() == 1 ? " registro" : " registros"));
    }

    private void showAddAdminDialog() {
        if (availableFaculties == null || availableFaculties.isEmpty()) {
            Toast.makeText(getContext(), "Cree una facultad primero", Toast.LENGTH_SHORT).show();
            return;
        }
        showAddAdminAlertDialog(availableFaculties);
    }

    private void showAddAdminAlertDialog(List<Faculty> faculties) {
        MaterialAlertDialogBuilder builder = DialogUtils.createMaterialDialog(requireContext(), "Añadir Administrador");
        LinearLayout layout = DialogUtils.createDialogContainer(requireContext());
        
        final EditText etName = DialogUtils.createStyledEditText(requireContext(), "Nombre Completo", 0);
        layout.addView(etName);
        
        final EditText etCarnet = DialogUtils.createStyledEditText(requireContext(), "Ingrese los 5 dígitos del carnet", InputType.TYPE_CLASS_NUMBER);
        etCarnet.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
        layout.addView(etCarnet);
        
        final EditText etPass = DialogUtils.createStyledEditText(requireContext(), "Contraseña", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etPass);

        final TextView tvStrength = new TextView(getContext());
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

        final TextView tvFaculty = DialogUtils.createDialogOptionButton(requireContext(), "Facultad: Administrativa", false);
        tvFaculty.setClickable(false);
        tvFaculty.setFocusable(false);
        layout.addView(tvFaculty);

        final EditText etAddress = DialogUtils.createStyledEditText(requireContext(), "Dirección (Opcional)", 0);
        layout.addView(etAddress);

        final EditText etEmail = DialogUtils.createStyledEditText(requireContext(), "Email Personal (Opcional)", 0);
        layout.addView(etEmail);
        
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
            if (carnet.length() != 5) {
                etCarnet.setError("Debe ingresar exactamente 5 dígitos");
                isValid = false;
            }
            if (ValidationUtils.getPasswordStrength(pass) == ValidationUtils.PasswordStrength.WEAK) {
                etPass.setError("Contraseña muy débil");
                isValid = false;
            }

            if (isValid) {
                String fullCarnet = "ADMIN" + carnet;
                viewModel.performOnlineAction(() -> {
                    User newAdmin = new User(fullCarnet, name, pass, "ADMIN", "Administrativa");
                    newAdmin.address = etAddress.getText().toString();
                    newAdmin.personal_email = etEmail.getText().toString();
                    viewModel.insertUser(newAdmin);
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
