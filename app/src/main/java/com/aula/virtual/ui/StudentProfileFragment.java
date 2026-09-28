package com.aula.virtual.ui;

import android.Manifest;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentStudentProfileBinding;
import java.io.IOException;

public class StudentProfileFragment extends Fragment {
    private FragmentStudentProfileBinding binding;
    private MainViewModel viewModel;
    private User currentUser;

    private final ActivityResultLauncher<Void> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    ImageUtils.showCropAndAdjustDialog(requireContext(), bitmap, this::updateProfilePicture);
                }
            }
    );

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    cameraLauncher.launch(null);
                } else {
                    Toast.makeText(getContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
                        ImageUtils.showCropAndAdjustDialog(requireContext(), bitmap, this::updateProfilePicture);
                    } catch (IOException e) {
                        Toast.makeText(getContext(), "Error al cargar imagen", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        int studentId = -1;
        if (getArguments() != null) {
            studentId = getArguments().getInt("studentId", -1);
        }

        if (studentId == -1) {
            // My own profile
            viewModel.getCurrentUser().observe(getViewLifecycleOwner(), this::displayUser);
            binding.cardProfilePicture.setOnClickListener(v -> showImageSourceDialog());
            
            viewModel.getModificationError().observe(getViewLifecycleOwner(), error -> {
                if (error != null) {
                    Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    viewModel.clearModificationError();
                }
            });
        } else {
            // Viewing another student's profile (Professor view)
            viewModel.getUserById(studentId, this::displayUser);
            binding.cardEditPhotoIndicator.setVisibility(View.GONE);
            binding.cardProfilePicture.setClickable(false);
        }

        setupCopyButtons();
    }

    private void setupCopyButtons() {
        binding.btnCopyName.setOnClickListener(v -> ClipboardUtils.copyToClipboard(getContext(), "Nombre", binding.tvProfileName.getText().toString()));
        binding.btnCopyCarnet.setOnClickListener(v -> ClipboardUtils.copyToClipboard(getContext(), "Carnet", binding.tvProfileCarnet.getText().toString()));
        binding.btnCopyAddress.setOnClickListener(v -> ClipboardUtils.copyToClipboard(getContext(), "Dirección", binding.tvValAddress.getText().toString()));
        binding.btnCopyPersonalEmail.setOnClickListener(v -> ClipboardUtils.copyToClipboard(getContext(), "Correo Personal", binding.tvValPersonalEmail.getText().toString()));
        binding.btnCopyInstEmail.setOnClickListener(v -> ClipboardUtils.copyToClipboard(getContext(), "Correo Institucional", binding.tvValInstitutionalEmail.getText().toString()));
    }

    private void displayUser(User user) {
        if (user != null) {
            currentUser = user; // Only used for updates in my own profile
            binding.tvProfileName.setText(user.name);
            binding.tvProfileCarnet.setText(user.carnet);
            binding.tvValFaculty.setText(user.faculty != null ? user.faculty : "Sin asignar");
            binding.tvValAddress.setText(user.address != null ? user.address : "No registrada");
            binding.tvValPersonalEmail.setText(user.personal_email != null ? user.personal_email : "No registrado");
            
            String emailSuffix = ("PROFESSOR".equals(user.role) || "ADMIN".equals(user.role)) ? "@mail.AulaV.edu.sv" : "@mail.AulaV.sv";
            binding.tvValInstitutionalEmail.setText(user.carnet + emailSuffix);
            
            if (user.can_change_photo == 0) {
                binding.cardEditPhotoIndicator.setVisibility(View.GONE);
            } else if (getArguments() == null || getArguments().getInt("studentId", -1) == -1) {
                binding.cardEditPhotoIndicator.setVisibility(View.VISIBLE);
            }

            int paddingPx = (int) (20 * getResources().getDisplayMetrics().density);
            ImageUtils.setProfileImage(binding.ivProfilePicture, user.profile_image, paddingPx);
        }
    }

    private void showImageSourceDialog() {
        if (currentUser != null && currentUser.can_change_photo == 0) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Acceso Restringido")
                    .setMessage("Tu función de cambio de foto ha sido deshabilitada por subir contenido inapropiado.")
                    .setPositiveButton("Entendido", null)
                    .show();
            return;
        }
        String[] options = {"Cámara", "Galería", "Eliminar foto"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cambiar foto de perfil")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) checkCameraPermission();
                    else if (which == 1) galleryLauncher.launch("image/*");
                    else removeProfilePicture();
                }).show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null);
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void updateProfilePicture(Bitmap bitmap) {
        if (currentUser != null) {
            String base64 = ImageUtils.bitmapToBase64(bitmap);
            currentUser.profile_image = base64;
            
            binding.ivProfilePicture.setPadding(0, 0, 0, 0);
            binding.ivProfilePicture.setImageBitmap(bitmap);
            binding.ivProfilePicture.setImageTintList(null);
            
            viewModel.performOnlineAction(() -> {
                viewModel.updateUser(currentUser);
                Toast.makeText(getContext(), "Foto de perfil actualizada", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void removeProfilePicture() {
        if (currentUser != null) {
            viewModel.performOnlineAction(() -> {
                currentUser.profile_image = null;
                viewModel.updateUser(currentUser);
                Toast.makeText(getContext(), "Foto de perfil eliminada", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
