package com.aula.virtual.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aula.virtual.R;
import com.aula.virtual.data.StudentGradeInfo;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.databinding.FragmentAdminDashboardBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminEnrollmentListFragment extends Fragment {
    private FragmentAdminDashboardBinding binding;
    private MainViewModel viewModel;
    private GradeAdapter adapter;
    private int studentId;
    private List<StudentGradeInfo> allGradeInfos = new ArrayList<>();

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
        
        if (getArguments() == null) {
            Navigation.findNavController(view).popBackStack();
            return;
        }
        
        studentId = getArguments().getInt("studentId");

        binding.tvTitle.setText("Materias y Notas");
        binding.spinnerFilter.setVisibility(View.GONE);
        binding.etSearch.setHint("Buscar materia...");

        viewModel.getUserById(studentId, user -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null && user != null) {
                        binding.tvSubtitle.setText("Alumno: " + user.name);
                        binding.tvSubtitle.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

        // Timetable Header Action for Student
        binding.cardHeaderAction.setVisibility(View.VISIBLE);
        binding.btnHeaderAction.setVisibility(View.VISIBLE);
        binding.btnHeaderAction.setImageResource(R.drawable.ic_timetable);
        
        View.OnClickListener openStudentTimetable = v -> {
            Bundle args = new Bundle();
            args.putInt("studentId", studentId);
            Navigation.findNavController(view).navigate(R.id.action_adminEnrollmentListFragment_to_adminFacilityTimetableFragment, args);
        };
        binding.btnHeaderAction.setOnClickListener(openStudentTimetable);
        binding.cardHeaderAction.setOnClickListener(openStudentTimetable);

        // Carnet / Student Detail Header Action
        binding.cardHeaderDetail.setVisibility(View.VISIBLE);
        binding.btnHeaderDetail.setVisibility(View.VISIBLE);

        View.OnClickListener openStudentDetail = v -> {
            Bundle args = new Bundle();
            args.putInt("studentId", studentId);
            Navigation.findNavController(view).navigate(R.id.action_adminEnrollmentListFragment_to_studentDetailFragment, args);
        };
        binding.btnHeaderDetail.setOnClickListener(openStudentDetail);
        binding.cardHeaderDetail.setOnClickListener(openStudentDetail);

        adapter = new GradeAdapter(true);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        setupSearch();

        viewModel.fetchSubjectFacilityMap(map -> {
            if (adapter != null && map != null) {
                adapter.setFacilityMap(map);
            }
        });

        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        viewModel.getGradeInfoForStudent(studentId).observe(getViewLifecycleOwner(), gradeInfos -> {
            if (gradeInfos != null) {
                allGradeInfos = new ArrayList<>(gradeInfos);
            } else {
                allGradeInfos = new ArrayList<>();
            }
            applyFilter();
        });

        adapter.setOnGradeActionListener(new GradeAdapter.OnGradeActionListener() {
            @Override
            public void onEditGrades(StudentGradeInfo info) {
                Bundle args = new Bundle();
                args.putInt("enrollmentId", info.enrollment.id);
                Navigation.findNavController(requireView()).navigate(R.id.action_adminEnrollmentListFragment_to_adminEditGradeFragment, args);
            }

            @Override
            public void onViewBlog(StudentGradeInfo info) {
                Bundle args = new Bundle();
                args.putInt("subjectId", info.subject.id);
                Navigation.findNavController(requireView()).navigate(R.id.action_adminEnrollmentListFragment_to_subjectBlogFragment, args);
            }

            @Override
            public void onDeleteEnrollment(StudentGradeInfo info) {
                if (info == null || info.enrollment == null) return;
                String subjName = (info.subject != null ? info.subject.name : "esta materia");
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Desinscribir Materia")
                    .setMessage("¿Estás seguro de desinscribir a este alumno de la materia \"" + subjName + "\"? Se eliminará su inscripción y sus calificaciones.")
                    .setPositiveButton("Desinscribir", (dialog, which) -> {
                        viewModel.performOnlineAction(() -> {
                            viewModel.deleteEnrollment(info.enrollment);
                            Toast.makeText(getContext(), "Inscripción eliminada", Toast.LENGTH_SHORT).show();
                        });
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
            }
        });

        binding.btnAdd.setText("Inscribir en Materia");
        binding.btnAdd.setOnClickListener(v -> showEnrollDialog());
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void applyFilter() {
        String query = binding.etSearch.getText().toString().toLowerCase().trim();
        if (query.isEmpty()) {
            adapter.setGradeInfos(allGradeInfos);
            binding.tvRecordCount.setText(allGradeInfos.size() + (allGradeInfos.size() == 1 ? " materia" : " materias"));
            return;
        }

        List<StudentGradeInfo> filtered = allGradeInfos.stream().filter(info -> {
            if (info == null || info.subject == null) return false;
            String name = info.subject.name != null ? info.subject.name.toLowerCase() : "";
            String desc = info.subject.description != null ? info.subject.description.toLowerCase() : "";
            return name.contains(query) || desc.contains(query);
        }).collect(Collectors.toList());

        adapter.setGradeInfos(filtered);
        binding.tvRecordCount.setText(filtered.size() + (filtered.size() == 1 ? " materia" : " materias"));
    }

    private <T> void observeOnce(androidx.lifecycle.LiveData<T> liveData, androidx.lifecycle.Observer<T> observer) {
        liveData.observe(getViewLifecycleOwner(), new androidx.lifecycle.Observer<T>() {
            @Override
            public void onChanged(T t) {
                if (t != null) {
                    liveData.removeObserver(this);
                    observer.onChanged(t);
                }
            }
        });
    }

    private void showEnrollDialog() {
        observeOnce(viewModel.getAllSubjects(), subjects -> {
            List<Integer> enrolledSubjectIds = allGradeInfos.stream()
                .filter(info -> info != null && info.subject != null)
                .map(info -> info.subject.id)
                .collect(Collectors.toList());

            List<Subject> availableSubjects = subjects.stream()
                .filter(s -> !enrolledSubjectIds.contains(s.id))
                .collect(Collectors.toList());

            if (availableSubjects.isEmpty()) {
                Toast.makeText(getContext(), "El alumno ya está inscrito en todas las materias disponibles.", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] names = new String[availableSubjects.size()];
            for (int i = 0; i < availableSubjects.size(); i++) {
                Subject s = availableSubjects.get(i);
                names[i] = s.name + " (Sección: " + (s.section != null ? s.section : "01") + ")";
            }

            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Seleccionar Materia")
                .setItems(names, (dialog, which) -> {
                    viewModel.performOnlineAction(() -> {
                        viewModel.enrollStudent(studentId, availableSubjects.get(which).id, error -> {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> 
                                    Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show()
                                );
                            }
                        });
                    });
                })
                .show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
