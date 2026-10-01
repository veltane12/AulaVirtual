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
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentAdminDashboardBinding;

public class ProfessorSubjectListFragment extends Fragment {
    private FragmentAdminDashboardBinding binding;
    private MainViewModel viewModel;
    private SubjectAdapter adapter;

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

        binding.tvTitle.setText("Mis Materias Asignadas");
        binding.btnAdd.setVisibility(View.GONE);
        binding.spinnerFilter.setVisibility(View.GONE);
        binding.etSearch.setHint("Buscar materia...");

        adapter = new SubjectAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        int targetProfId = -1;
        if (getArguments() != null && getArguments().containsKey("professorId")) {
            targetProfId = getArguments().getInt("professorId");
            binding.tvTitle.setText("Materias Asignadas al Profesor");
        } else {
            User prof = viewModel.getCurrentUser().getValue();
            if (prof != null) targetProfId = prof.id;
        }

        if (targetProfId != -1) {
            final int profId = targetProfId;
            viewModel.getSubjectsByProfessor(profId).observe(getViewLifecycleOwner(), subjects -> {
                adapter.setSubjects(subjects);
                if (subjects != null) {
                    binding.tvRecordCount.setText(subjects.size() + (subjects.size() == 1 ? " materia" : " materias"));
                } else {
                    binding.tvRecordCount.setText("0 materias");
                }
            });

            viewModel.fetchSubjectFacilityMap(map -> {
                if (adapter != null && map != null) {
                    adapter.setFacilityMap(map);
                }
            });

            binding.cardHeaderAction.setVisibility(View.VISIBLE);
            binding.btnHeaderAction.setImageResource(R.drawable.ic_timetable);
            
            View.OnClickListener openTimetable = v -> {
                Bundle args = new Bundle();
                args.putInt("professorId", profId);
                Navigation.findNavController(view).navigate(R.id.action_professorSubjectListFragment_to_adminFacilityTimetableFragment, args);
            };

            binding.btnHeaderAction.setOnClickListener(openTimetable);
            binding.cardHeaderAction.setOnClickListener(openTimetable);
        }

        adapter.setOnItemClickListener(subject -> {
            Bundle args = new Bundle();
            args.putInt("subjectId", subject.id);
            Navigation.findNavController(view).navigate(R.id.action_professorSubjectListFragment_to_subjectDetailFragment, args);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
