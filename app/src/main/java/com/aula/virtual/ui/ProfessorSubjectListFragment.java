package com.aula.virtual.ui;

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
import com.aula.virtual.data.ScheduleInfo;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentAdminDashboardBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

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
        adapter.setManageButtonText("Gestionar");
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        boolean isManagingProfessor = getArguments() != null && (getArguments().containsKey("professorId") || getArguments().containsKey("adminId"));
        int targetProfId = -1;
        if (getArguments() != null && getArguments().containsKey("professorId")) {
            targetProfId = getArguments().getInt("professorId");
        } else if (getArguments() != null && getArguments().containsKey("adminId")) {
            targetProfId = getArguments().getInt("adminId");
        } else {
            User prof = viewModel.getCurrentUser().getValue();
            if (prof != null) targetProfId = prof.id;
        }

        if (targetProfId != -1) {
            final int profId = targetProfId;

            if (isManagingProfessor) {
                binding.tvTitle.setText("Materias Asignadas");
                viewModel.getUserById(profId, user -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (binding != null && user != null) {
                                binding.tvSubtitle.setText("Profesor: " + user.name);
                                binding.tvSubtitle.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });

                // Botón Inscribir en Materia
                binding.btnAdd.setText("Inscribir en Materia");
                binding.btnAdd.setVisibility(View.VISIBLE);
                binding.btnAdd.setOnClickListener(v -> showAssignSubjectDialog(profId));

                adapter.setShowDeleteButton(true);
                adapter.setDeleteButtonText("Desinscribir");
                adapter.setOnDeleteClickListener(subject -> showUnassignSubjectDialog(profId, subject));

                // Carnet / Professor Detail Header Action
                binding.cardHeaderDetail.setVisibility(View.VISIBLE);
                binding.btnHeaderDetail.setVisibility(View.VISIBLE);

                View.OnClickListener openProfessorDetail = v -> {
                    Bundle args = new Bundle();
                    args.putInt("adminId", profId);
                    Navigation.findNavController(view).navigate(R.id.action_professorSubjectListFragment_to_adminDetailFragment, args);
                };
                binding.btnHeaderDetail.setOnClickListener(openProfessorDetail);
                binding.cardHeaderDetail.setOnClickListener(openProfessorDetail);
            } else {
                binding.tvTitle.setText("Mis Materias Asignadas");
            }

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
            Navigation.findNavController(view).navigate(R.id.action_professorSubjectListFragment_to_subjectBlogFragment, args);
        });
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

    private void showAssignSubjectDialog(int profId) {
        observeOnce(viewModel.getAllSubjects(), subjects -> {
            if (subjects == null || subjects.isEmpty()) {
                Toast.makeText(getContext(), "No hay materias registradas", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] names = new String[subjects.size()];
            for (int i = 0; i < subjects.size(); i++) {
                Subject s = subjects.get(i);
                String sec = s.section != null ? s.section : "01";
                names[i] = s.name + " (Sección: " + sec + ")";
            }

            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Inscribir Materia a Profesor")
                .setItems(names, (dialog, which) -> {
                    Subject selected = subjects.get(which);
                    if (selected.professorId != null && selected.professorId != 0 && selected.professorId != profId) {
                        viewModel.getUserById(selected.professorId, existingProf -> {
                            String profName = existingProf != null ? existingProf.name : "otro profesor";
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> 
                                    new MaterialAlertDialogBuilder(requireContext())
                                        .setTitle("⚠️ Asignación Restringida")
                                        .setMessage("La materia \"" + selected.name + "\" ya está asignada al profesor " + profName + ".\n\nSolo se permite UN Profesor por Materia.")
                                        .setPositiveButton("Entendido", null)
                                        .show()
                                );
                            }
                        });
                        return;
                    }

                    selected.professorId = profId;
                    viewModel.performOnlineAction(() -> {
                        viewModel.updateSubject(selected);
                        viewModel.updateSchedulesProfessorBySubject(selected.id, profId);
                        Toast.makeText(getContext(), "¡Profesor inscrito/asignado a la materia!", Toast.LENGTH_SHORT).show();
                        viewModel.getSubjectsByProfessor(profId);
                        viewModel.refreshData();
                    });
                })
                .show();
        });
    }

    private void showUnassignSubjectDialog(int profId, Subject subject) {
        if (subject == null) return;
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Desinscribir Materia")
            .setMessage("¿Estás seguro de desinscribir a este profesor de la materia \"" + subject.name + "\"? Se eliminará la asignación del profesor.")
            .setPositiveButton("Desinscribir", (dialog, which) -> {
                viewModel.performOnlineAction(() -> {
                    subject.professorId = null;
                    viewModel.updateSubject(subject);
                    viewModel.updateSchedulesProfessorBySubject(subject.id, 0);

                    Toast.makeText(getContext(), "Inscripción eliminada", Toast.LENGTH_SHORT).show();
                    viewModel.getSubjectsByProfessor(profId);
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
