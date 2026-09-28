package com.aula.virtual.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.aula.virtual.R;
import com.aula.virtual.data.StudentGradeInfo;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.databinding.FragmentStudentDashboardBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StudentDashboardFragment extends Fragment {
    private FragmentStudentDashboardBinding binding;
    private MainViewModel viewModel;
    private SubjectAdapter adapter;
    private List<Subject> allStudentSubjects = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        adapter = new SubjectAdapter();
        binding.rvGrades.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvGrades.setAdapter(adapter);

        setupSearch();

        adapter.setOnItemClickListener(subject -> {
            if (subject != null) {
                Bundle args = new Bundle();
                args.putInt("subjectId", subject.id);
                Navigation.findNavController(requireView()).navigate(R.id.action_studentDashboardFragment_to_subjectDetailFragment, args);
            }
        });

        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                viewModel.getGradeInfoForStudent(user.id).observe(getViewLifecycleOwner(), gradeInfos -> {
                    allStudentSubjects.clear();
                    if (gradeInfos != null) {
                        for (StudentGradeInfo info : gradeInfos) {
                            if (info.subject != null) {
                                allStudentSubjects.add(info.subject);
                            }
                        }
                    }
                    applyFilter();
                });

                View.OnClickListener openTimetable = v -> {
                    Bundle args = new Bundle();
                    args.putInt("studentId", user.id);
                    Navigation.findNavController(requireView()).navigate(R.id.action_studentDashboardFragment_to_adminFacilityTimetableFragment, args);
                };

                binding.btnViewTimetableStudent.setOnClickListener(openTimetable);
                binding.cardTimetableStudent.setOnClickListener(openTimetable);
            }
        });
    }

    private void setupSearch() {
        binding.etSearchStudent.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void applyFilter() {
        String query = binding.etSearchStudent.getText().toString().toLowerCase().trim();
        if (query.isEmpty()) {
            adapter.setSubjects(allStudentSubjects);
            return;
        }

        List<Subject> filtered = allStudentSubjects.stream().filter(s -> {
            if (s == null) return false;
            String name = s.name != null ? s.name.toLowerCase() : "";
            String faculty = s.faculty != null ? s.faculty.toLowerCase() : "";
            String desc = s.description != null ? s.description.toLowerCase() : "";
            return name.contains(query) || faculty.contains(query) || desc.contains(query);
        }).collect(Collectors.toList());

        adapter.setSubjects(filtered);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
