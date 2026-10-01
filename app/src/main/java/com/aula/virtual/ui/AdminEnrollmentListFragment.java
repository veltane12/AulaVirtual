package com.aula.virtual.ui;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
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
import java.util.List;

public class AdminEnrollmentListFragment extends Fragment {
    private FragmentAdminDashboardBinding binding;
    private MainViewModel viewModel;
    private GradeAdapter adapter;
    private int studentId;

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

        binding.tvTitle.setText("Mis Materias");
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

        adapter = new GradeAdapter(true);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

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
            adapter.setGradeInfos(gradeInfos);
            if (gradeInfos != null) {
                binding.tvRecordCount.setText(gradeInfos.size() + (gradeInfos.size() == 1 ? " materia" : " materias"));
            } else {
                binding.tvRecordCount.setText("0 materias");
            }
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
                // For admin, we use the same blog fragment but it should detect admin role from viewModel
                Navigation.findNavController(requireView()).navigate(R.id.action_adminEnrollmentListFragment_to_subjectBlogFragment, args);
            }

            @Override
            public void onDeleteEnrollment(StudentGradeInfo info) {
                if (info == null || info.enrollment == null) return;
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Confirmar Desinscripción")
                    .setMessage("¿Estás seguro de que deseas eliminar esta materia inscrita?")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
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

    private void showEnrollDialog() {
        viewModel.getAllSubjects().observe(getViewLifecycleOwner(), subjects -> {
            String[] names = new String[subjects.size()];
            for (int i = 0; i < subjects.size(); i++) {
                Subject s = subjects.get(i);
                names[i] = s.name + " (Sección: " + (s.section != null ? s.section : "01") + ")";
            }

            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Seleccionar Materia")
                .setItems(names, (dialog, which) -> {
                    viewModel.performOnlineAction(() -> {
                        viewModel.enrollStudent(studentId, subjects.get(which).id, error -> {
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
