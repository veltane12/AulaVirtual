package com.aula.virtual.ui;

import android.os.Bundle;
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

public class StudentDashboardFragment extends Fragment {
    private FragmentStudentDashboardBinding binding;
    private MainViewModel viewModel;
    private SubjectAdapter adapter;

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
                    List<Subject> subjects = new ArrayList<>();
                    if (gradeInfos != null) {
                        for (StudentGradeInfo info : gradeInfos) {
                            if (info.subject != null) {
                                subjects.add(info.subject);
                            }
                        }
                    }
                    adapter.setSubjects(subjects);
                });

                binding.btnViewTimetableStudent.setOnClickListener(v -> {
                    Bundle args = new Bundle();
                    args.putInt("studentId", user.id);
                    Navigation.findNavController(requireView()).navigate(R.id.action_studentDashboardFragment_to_adminFacilityTimetableFragment, args);
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
