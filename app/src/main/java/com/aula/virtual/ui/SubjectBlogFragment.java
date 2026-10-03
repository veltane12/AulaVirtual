package com.aula.virtual.ui;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.aula.virtual.R;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.aula.virtual.data.entity.BlogEntry;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentSubjectBlogBinding;
import java.util.ArrayList;
import java.util.List;

public class SubjectBlogFragment extends Fragment {
    private FragmentSubjectBlogBinding binding;
    private MainViewModel viewModel;
    private BlogAdapter adapter;
    private int subjectId;
    private boolean isAdmin;
    private ItemTouchHelper touchHelper;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private static final int POLL_INTERVAL = 4000; // Poll blog every 4 seconds

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewModel != null && isResumed()) {
                viewModel.fetchSubjectBlog(subjectId);
                pollHandler.postDelayed(this, POLL_INTERVAL);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSubjectBlogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        
        if (getArguments() != null) {
            subjectId = getArguments().getInt("subjectId");
        }
        
        User user = viewModel.getCurrentUser().getValue();
        isAdmin = user != null && ("ADMIN".equals(user.role) || "PROFESSOR".equals(user.role));

        setupRecyclerView();
        observeViewModel();
        
        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        if (isAdmin) {
            binding.btnAddEntryTop.setVisibility(View.VISIBLE);
            binding.btnAddEntryTop.setOnClickListener(v -> showEntryDialog(null));
            binding.fabAddEntry.setVisibility(View.GONE);
        }
        
        viewModel.fetchSubjectBlog(subjectId);
        viewModel.getSubjectById(subjectId, sub -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null && sub != null) binding.tvBlogTitle.setText("Foro de Discusión: " + sub.name);
                });
            }
        });

        binding.btnViewParticipantsBlog.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("subjectId", subjectId);
            Navigation.findNavController(view).navigate(R.id.action_subjectBlogFragment_to_subjectParticipantsFragment, args);
        });

        View.OnClickListener openSubjectDetail = v -> {
            Bundle args = new Bundle();
            args.putInt("subjectId", subjectId);
            Navigation.findNavController(view).navigate(R.id.action_subjectBlogFragment_to_subjectDetailFragment, args);
        };
        binding.cardSubjectDetailBlog.setOnClickListener(openSubjectDetail);
        binding.btnSubjectDetailBlog.setOnClickListener(openSubjectDetail);

        // Start polling
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL);
    }

    private void setupRecyclerView() {
        adapter = new BlogAdapter(isAdmin, new BlogAdapter.OnBlogActionListener() {
            @Override public void onEdit(BlogEntry entry) { showEntryDialog(entry); }
            @Override public void onDelete(BlogEntry entry) { showDeleteDialog(entry); }
            @Override public void onOrderChanged(List<BlogEntry> entries) {
                viewModel.performOnlineAction(() -> viewModel.updateBlogOrder(entries));
            }
            @Override public void onDiscussionClick(BlogEntry entry) {
                Bundle args = new Bundle();
                args.putInt("blogEntryId", entry.id);
                args.putString("blogTitle", entry.title);
                args.putInt("subjectId", subjectId);
                Navigation.findNavController(requireView()).navigate(R.id.action_subjectBlogFragment_to_blogDiscussionFragment, args);
            }
        });
        binding.rvBlog.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvBlog.setAdapter(adapter);

        if (isAdmin) {
            setupDragAndDrop();
        }
    }

    private void setupDragAndDrop() {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewModel.updateBlogOrder(adapter.getEntries());
            }
        };
        touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(binding.rvBlog);
        adapter.setOnStartDragListener(touchHelper::startDrag);
    }

    private void observeViewModel() {
        viewModel.getSubjectBlog().observe(getViewLifecycleOwner(), entries -> {
            if (entries != null) {
                adapter.setEntries(entries);
            }
        });
    }

    private void showEntryDialog(@Nullable BlogEntry existing) {
        MaterialAlertDialogBuilder builder = DialogUtils.createMaterialDialog(requireContext(), existing == null ? "Nueva Asignación" : "Editar Asignación");

        LinearLayout layout = DialogUtils.createDialogContainer(requireContext());

        final String[] selectedCategory = {existing != null ? existing.category : "Aviso"};
        final TextView tvCategory = DialogUtils.createDialogOptionButton(requireContext(), existing != null ? "Categoría: " + existing.category : "Categoría: Aviso", existing == null);
        String[] categories = {"Aviso", "Parcial", "Tarea"};
        tvCategory.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Categoría de Asignación")
                .setItems(categories, (dialog1, which) -> {
                    selectedCategory[0] = categories[which];
                    DialogUtils.setOptionState(tvCategory, "Categoría: " + selectedCategory[0], false, requireContext());
                }).show();
        });
        layout.addView(tvCategory);

        final EditText etTitle = DialogUtils.createStyledEditText(requireContext(), "Título", 0);
        if (existing != null) etTitle.setText(existing.title);
        layout.addView(etTitle);

        final EditText etContent = DialogUtils.createStyledEditText(requireContext(), "Contenido/Instrucciones", 0);
        if (existing != null) etContent.setText(existing.content);
        layout.addView(etContent);

        builder.setView(layout);
        builder.setPositiveButton("Guardar", null); 
        builder.setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String cat = selectedCategory[0];
            String title = etTitle.getText().toString();
            String content = etContent.getText().toString();

            boolean isValid = true;
            if (title.isEmpty()) {
                etTitle.setError("El título es obligatorio");
                isValid = false;
            }
            if (content.isEmpty()) {
                etContent.setError("El contenido es obligatorio");
                isValid = false;
            }

            if (isValid) {
                if (existing == null) {
                    viewModel.performOnlineAction(() -> {
                        viewModel.insertBlogEntry(new BlogEntry(subjectId, cat, title, content), result -> {
                            if (!"OK".equals(result)) Toast.makeText(getContext(), result, Toast.LENGTH_LONG).show();
                        });
                        dialog.dismiss();
                    });
                } else {
                    existing.category = cat;
                    existing.title = title;
                    existing.content = content;
                    viewModel.performOnlineAction(() -> {
                        viewModel.updateBlogEntry(existing, result -> {
                            if (!"OK".equals(result)) Toast.makeText(getContext(), result, Toast.LENGTH_LONG).show();
                        });
                        dialog.dismiss();
                    });
                }
            }
        });
    }

    private void showDeleteDialog(BlogEntry entry) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Entrada")
            .setMessage("¿Estás seguro de eliminar esta asignación?")
            .setPositiveButton("Eliminar", (dialog, which) -> {
                viewModel.performOnlineAction(() -> viewModel.deleteBlogEntry(entry));
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    @Override
    public void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (subjectId != 0) {
            pollHandler.postDelayed(pollRunnable, POLL_INTERVAL);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pollHandler.removeCallbacks(pollRunnable);
        binding = null;
    }
}
