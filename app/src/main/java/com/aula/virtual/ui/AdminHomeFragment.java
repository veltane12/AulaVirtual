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
import com.aula.virtual.databinding.FragmentAdminHomeBinding;

public class AdminHomeFragment extends Fragment {
    private FragmentAdminHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MainViewModel viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        binding.cardStudents.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_adminHomeFragment_to_adminStudentListFragment));
        
        binding.cardSubjects.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_adminHomeFragment_to_adminSubjectListFragment));

        binding.cardFaculties.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_adminHomeFragment_to_adminFacultyListFragment));

        binding.cardAdmins.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_adminHomeFragment_to_adminAdminListFragment));

        binding.cardProfessors.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_adminHomeFragment_to_adminProfessorListFragment));

        binding.cardFacilities.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_adminHomeFragment_to_adminFacilityListFragment));
            
        binding.cardSettings.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_adminHomeFragment_to_settingsFragment));

        binding.cardLogout.setOnClickListener(v -> {
            viewModel.logout();
            Navigation.findNavController(view).navigate(R.id.action_adminHomeFragment_to_loginFragment);
        });

        binding.btnGoToProfileAdmin.setOnClickListener(v -> 
            Navigation.findNavController(view).navigate(R.id.action_adminHomeFragment_to_studentProfileFragment));

        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                binding.tvAdminName.setText("Bienvenido " + user.name);
                updateProfileImage(user.profile_image, binding.ivProfileImageAdmin);
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
