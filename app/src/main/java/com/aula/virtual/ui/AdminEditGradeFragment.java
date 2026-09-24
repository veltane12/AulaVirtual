package com.aula.virtual.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import java.util.Locale;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.aula.virtual.data.entity.Enrollment;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentEditGradesBinding;

public class AdminEditGradeFragment extends Fragment {
    private FragmentEditGradesBinding binding;
    private MainViewModel viewModel;
    private Enrollment enrollment;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEditGradesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        int enrollmentId = getArguments().getInt("enrollmentId");

        setupUI();
        observeViewModel();
        
        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        viewModel.fetchEnrollmentById(enrollmentId);
    }

    private void setupUI() {
        binding.btnSave.setOnClickListener(v -> saveChanges());
    }

    private void observeViewModel() {
        viewModel.getSelectedEnrollment().observe(getViewLifecycleOwner(), en -> {
            if (en != null && binding != null) {
                enrollment = en;
                viewModel.getSubjectById(enrollment.subjectId, subject -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (binding != null && subject != null) {
                                binding.tvSubjectName.setText("Materia: " + subject.name);
                            }
                        });
                    }
                });
                binding.etGrade1.setText(formatGrade(enrollment.grade1));
                binding.etGrade2.setText(formatGrade(enrollment.grade2));
                binding.etGrade3.setText(formatGrade(enrollment.grade3));
                binding.etGrade4.setText(formatGrade(enrollment.grade4));
                binding.etGrade5.setText(formatGrade(enrollment.grade5));
            }
        });
    }

    private void saveChanges() {
        if (enrollment != null) {
            Double g1 = parseGrade(binding.etGrade1);
            Double g2 = parseGrade(binding.etGrade2);
            Double g3 = parseGrade(binding.etGrade3);
            Double g4 = parseGrade(binding.etGrade4);
            Double g5 = parseGrade(binding.etGrade5);

            if (!isValidRange(g1, binding.etGrade1) || 
                !isValidRange(g2, binding.etGrade2) ||
                !isValidRange(g3, binding.etGrade3) ||
                !isValidRange(g4, binding.etGrade4) ||
                !isValidRange(g5, binding.etGrade5)) {
                return;
            }

            enrollment.grade1 = g1;
            enrollment.grade2 = g2;
            enrollment.grade3 = g3;
            enrollment.grade4 = g4;
            enrollment.grade5 = g5;
            
            viewModel.performOnlineAction(() -> {
                viewModel.updateEnrollment(enrollment);
                Toast.makeText(getContext(), "Calificaciones actualizadas", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).popBackStack();
            });
        }
    }

    private boolean isValidRange(Double grade, EditText et) {
        if (grade == null) return true;
        if (grade < 0 || grade > 10) {
            et.setError("La nota debe estar entre 0.0 y 10.0");
            return false;
        }
        return true;
    }

    private String formatGrade(Double grade) {
        if (grade == null) return "";
        return String.format(Locale.getDefault(), "%.1f", grade);
    }

    private Double parseGrade(EditText et) {
        String val = et.getText().toString();
        if (val.isEmpty()) return null;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
