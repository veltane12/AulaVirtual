package com.aula.virtual.ui;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.aula.virtual.data.entity.BlogComment;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentBlogDiscussionBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BlogDiscussionFragment extends Fragment {
    private FragmentBlogDiscussionBinding binding;
    private MainViewModel viewModel;
    private BlogCommentAdapter adapter;
    private int blogEntryId;
    
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private static final int POLL_INTERVAL = 4000; // Poll every 4 seconds

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewModel != null && isResumed()) {
                viewModel.fetchBlogComments(blogEntryId);
                pollHandler.postDelayed(this, POLL_INTERVAL);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBlogDiscussionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        
        blogEntryId = getArguments().getInt("blogEntryId");
        String entryTitle = getArguments().getString("blogTitle");
        int subjectId = getArguments().getInt("subjectId");
        binding.tvEntryTitle.setText(entryTitle);

        User currentUser = viewModel.getCurrentUser().getValue();
        int currentUserId = currentUser != null ? currentUser.id : -1;
        boolean isAdmin = currentUser != null && "ADMIN".equals(currentUser.role);

        adapter = new BlogCommentAdapter(currentUserId, isAdmin);
        binding.rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvComments.setAdapter(adapter);

        if (isAdmin) {
            binding.btnClearChat.setVisibility(View.VISIBLE);
            binding.btnClearChat.setOnClickListener(v -> showClearChatConfirmation());
        }

        adapter.setOnCommentLongClickListener(this::showDeleteConfirmation);

        viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                viewModel.clearModificationError();
            }
        });

        viewModel.getBlogComments().observe(getViewLifecycleOwner(), comments -> {
            if (comments != null) {
                int oldSize = adapter.getItemCount();
                adapter.setComments(comments);
                int newSize = adapter.getItemCount();
                if (newSize > oldSize && newSize > 0) {
                    binding.rvComments.scrollToPosition(newSize - 1);
                }
            }
        });

        // Start polling
        pollHandler.post(pollRunnable);

        binding.btnSend.setOnClickListener(v -> {
            String content = binding.etComment.getText().toString().trim();
            if (!content.isEmpty()) {
                User user = viewModel.getCurrentUser().getValue();
                if (user != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
                    String timestamp = sdf.format(new Date());
                    BlogComment newComment = new BlogComment(blogEntryId, user.id, content);
                    newComment.timestamp = timestamp;
                    viewModel.performOnlineAction(() -> {
                        viewModel.insertBlogComment(newComment);
                        binding.etComment.setText("");
                    });
                }
            }
        });
    }

    private void showDeleteConfirmation(BlogComment comment) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Eliminar mensaje")
                .setMessage("¿Estás seguro de que deseas eliminar este mensaje definitivamente?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    viewModel.performOnlineAction(() -> viewModel.deleteBlogComment(comment));
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showClearChatConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Vaciar conversación")
                .setMessage("¿Deseas eliminar TODOS los mensajes de esta discusión? Esta acción no se puede deshacer.")
                .setPositiveButton("Vaciar todo", (dialog, which) -> {
                    viewModel.performOnlineAction(() -> viewModel.clearBlogDiscussion(blogEntryId));
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
        if (blogEntryId != 0) {
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
