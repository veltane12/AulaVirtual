package com.aula.virtual.ui;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.aula.virtual.R;
import com.aula.virtual.databinding.FragmentStudentHomeBinding;

public class StudentHomeFragment extends Fragment {
    private FragmentStudentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MainViewModel viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        binding.cardGrades.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_studentHomeFragment_to_studentDashboardFragment));
            
        binding.cardSettings.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_studentHomeFragment_to_settingsFragment));

        binding.cardLogout.setOnClickListener(v -> {
            viewModel.logout();
            Navigation.findNavController(view).navigate(R.id.action_studentHomeFragment_to_loginFragment);
        });

        binding.btnGoToProfileHome.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_studentHomeFragment_to_studentProfileFragment));

        if (viewModel.isImpersonating()) {
            binding.cardReturnToAdmin.setVisibility(View.VISIBLE);
            binding.cardReturnToAdmin.setOnClickListener(v -> {
                viewModel.stopImpersonation();
                Navigation.findNavController(view).popBackStack();
            });
        }

        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                String roleTitle = ("ENCARGADO".equals(user.role) || "GUARDIAN".equals(user.role)) ? "Encargado/a " : "";
                binding.tvWelcomeName.setText("Bienvenido " + roleTitle + user.name);
                updateProfileImage(user.profile_image, binding.ivProfileImageHome);
            }
        });
    }

    private void updateProfileImage(String profileImage, ImageView iv) {
        int paddingPx = (int) (8 * getResources().getDisplayMetrics().density);
        ImageUtils.setProfileImage(iv, profileImage, paddingPx);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
