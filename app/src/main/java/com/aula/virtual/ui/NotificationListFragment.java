package com.aula.virtual.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.aula.virtual.R;
import com.aula.virtual.data.ScheduleInfo;
import com.aula.virtual.data.StudentGradeInfo;
import com.aula.virtual.data.entity.Facility;
import com.aula.virtual.data.entity.Faculty;
import com.aula.virtual.data.entity.Notification;
import com.aula.virtual.data.entity.Subject;
import com.aula.virtual.data.entity.User;
import com.aula.virtual.databinding.FragmentNotificationListBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotificationListFragment extends Fragment {
    private FragmentNotificationListBinding binding;
    private MainViewModel viewModel;
    private User currentUser;
    private List<Notification> allUserNotifications = new ArrayList<>();
    private List<Notification> rawServerNotifications = new ArrayList<>();
    private final List<String> userSubjectNames = new ArrayList<>();
    private final List<String> userFacilityNames = new ArrayList<>();
    private final Map<Integer, String> facilityMap = new HashMap<>();
    private final String[] selectedChannelFilter = {"ALL"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        currentUser = viewModel.getCurrentUser().getValue();

        if (currentUser == null) {
            binding.cardLoginNotice.setVisibility(View.VISIBLE);
            binding.cardListContainer.setVisibility(View.GONE);
            binding.chipsScrollView.setVisibility(View.GONE);
            binding.etSearchNotification.setVisibility(View.GONE);

            binding.btnGoLogin.setOnClickListener(v -> 
                Navigation.findNavController(view).navigate(R.id.action_global_loginFragment));
            return;
        }

        binding.cardLoginNotice.setVisibility(View.GONE);
        binding.cardListContainer.setVisibility(View.VISIBLE);

        if ("ADMIN".equals(currentUser.role)) {
            binding.btnCreateNotification.setVisibility(View.VISIBLE);
            binding.btnCreateNotification.setOnClickListener(v -> showCreateNotificationDialog(null));
        } else {
            binding.btnCreateNotification.setVisibility(View.GONE);
        }

        setupSearch();

        viewModel.getAllFacilities().observe(getViewLifecycleOwner(), facilities -> {
            if (facilities != null) {
                for (Facility f : facilities) {
                    facilityMap.put(f.id, f.name);
                }
                reFilterAndRender();
            }
        });

        if ("STUDENT".equals(currentUser.role)) {
            viewModel.getStudentSchedules(currentUser.id).observe(getViewLifecycleOwner(), schedules -> {
                if (schedules != null) {
                    for (ScheduleInfo info : schedules) {
                        if (info.subjectName != null && !userSubjectNames.contains(info.subjectName)) {
                            userSubjectNames.add(info.subjectName);
                        }
                        if (info.schedule != null) {
                            String facName = facilityMap.get(info.schedule.facilityId);
                            if (facName != null && !userFacilityNames.contains(facName)) {
                                userFacilityNames.add(facName);
                            }
                        }
                    }
                    reFilterAndRender();
                }
            });

            viewModel.getGradeInfoForStudent(currentUser.id).observe(getViewLifecycleOwner(), gradeInfos -> {
                if (gradeInfos != null) {
                    for (StudentGradeInfo info : gradeInfos) {
                        if (info.subject != null && info.subject.name != null && !userSubjectNames.contains(info.subject.name)) {
                            userSubjectNames.add(info.subject.name);
                        }
                    }
                    reFilterAndRender();
                }
            });
        } else if ("PROFESSOR".equals(currentUser.role)) {
            viewModel.getProfessorSchedules(currentUser.id).observe(getViewLifecycleOwner(), schedules -> {
                if (schedules != null) {
                    for (ScheduleInfo info : schedules) {
                        if (info.subjectName != null && !userSubjectNames.contains(info.subjectName)) {
                            userSubjectNames.add(info.subjectName);
                        }
                        if (info.schedule != null) {
                            String facName = facilityMap.get(info.schedule.facilityId);
                            if (facName != null && !userFacilityNames.contains(facName)) {
                                userFacilityNames.add(facName);
                            }
                        }
                    }
                    reFilterAndRender();
                }
            });

            viewModel.getSubjectsByProfessor(currentUser.id).observe(getViewLifecycleOwner(), subjects -> {
                if (subjects != null) {
                    for (Subject sub : subjects) {
                        if (sub.name != null && !userSubjectNames.contains(sub.name)) {
                            userSubjectNames.add(sub.name);
                        }
                    }
                    reFilterAndRender();
                }
            });
        }

        viewModel.getAllNotifications().observe(getViewLifecycleOwner(), notifications -> {
            if (notifications != null) {
                rawServerNotifications = notifications;
                reFilterAndRender();
            }
        });

        viewModel.fetchNotifications();
    }

    private void setupSearch() {
        binding.etSearchNotification.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderNotificationsAndChips();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void renderNotificationsAndChips() {
        if (binding == null) return;

        String query = binding.etSearchNotification.getText().toString().toLowerCase().trim();

        // Apply text query filter
        List<Notification> queryFiltered = new ArrayList<>();
        for (Notification n : allUserNotifications) {
            if (n == null) continue;
            if (query.isEmpty()) {
                queryFiltered.add(n);
            } else {
                String title = n.title != null ? n.title.toLowerCase() : "";
                String msg = n.message != null ? n.message.toLowerCase() : "";
                String target = getNotificationTargetLabel(n).toLowerCase();
                if (title.contains(query) || msg.contains(query) || target.contains(query)) {
                    queryFiltered.add(n);
                }
            }
        }

        // Divide into specific channels
        List<Notification> generalNotifs = new ArrayList<>();
        List<Notification> supportNotifs = new ArrayList<>();
        List<Notification> studentNotifs = new ArrayList<>();
        List<Notification> professorNotifs = new ArrayList<>();
        List<Notification> adminNotifs = new ArrayList<>();
        List<Notification> facultyNotifs = new ArrayList<>();
        List<Notification> subjectNotifs = new ArrayList<>();
        List<Notification> facilityNotifs = new ArrayList<>();

        for (Notification n : queryFiltered) {
            if (n == null || n.targetType == null) {
                generalNotifs.add(n);
                continue;
            }
            String type = n.targetType.toUpperCase();
            if ("SUPPORT".equals(type)) {
                supportNotifs.add(n);
            } else if ("ROLE_STUDENTS".equals(type)) {
                studentNotifs.add(n);
            } else if ("ROLE_PROFESSORS".equals(type)) {
                professorNotifs.add(n);
            } else if ("ROLE_ADMINS".equals(type)) {
                adminNotifs.add(n);
            } else if ("FACULTY".equals(type)) {
                facultyNotifs.add(n);
            } else if ("SUBJECT".equals(type)) {
                subjectNotifs.add(n);
            } else if ("FACILITY".equals(type)) {
                facilityNotifs.add(n);
            } else {
                generalNotifs.add(n);
            }
        }

        // Render Channel Chips
        int activeThemeColor = ThemeHelper.getSubjectColor(requireContext(), ThemeHelper.getAccentColorName(requireContext()));
        renderChannelChips(generalNotifs, supportNotifs, studentNotifs, professorNotifs, adminNotifs, facultyNotifs, subjectNotifs, facilityNotifs, activeThemeColor);

        // Render Cards inside List Container
        binding.notificationsContainer.removeAllViews();
        boolean isDark = ThemeHelper.isDarkMode(requireContext());
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        int primaryTextColor = typedValue.data;
        requireContext().getTheme().resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
        int secondaryTextColor = typedValue.data;

        String filter = selectedChannelFilter[0];
        boolean hasContent = false;

        if ("ALL".equals(filter) || "GENERAL".equals(filter)) {
            if (!generalNotifs.isEmpty()) {
                addChannelHeaderView(binding.notificationsContainer, "📢 Canal General", activeThemeColor);
                renderNotificationCards(binding.notificationsContainer, generalNotifs, primaryTextColor, secondaryTextColor, isDark);
                hasContent = true;
            }
        }
        if ("ALL".equals(filter) || "SUPPORT".equals(filter)) {
            if (!supportNotifs.isEmpty()) {
                addChannelHeaderView(binding.notificationsContainer, "✉️ Canal Soporte", 0xFFD32F2F);
                renderNotificationCards(binding.notificationsContainer, supportNotifs, primaryTextColor, secondaryTextColor, isDark);
                hasContent = true;
            }
        }
        if ("ALL".equals(filter) || "ROLE_STUDENTS".equals(filter)) {
            if (!studentNotifs.isEmpty()) {
                addChannelHeaderView(binding.notificationsContainer, "👨‍🎓 Canal Estudiantes", 0xFF0288D1);
                renderNotificationCards(binding.notificationsContainer, studentNotifs, primaryTextColor, secondaryTextColor, isDark);
                hasContent = true;
            }
        }
        if ("ALL".equals(filter) || "ROLE_PROFESSORS".equals(filter)) {
            if (!professorNotifs.isEmpty()) {
                addChannelHeaderView(binding.notificationsContainer, "👨‍🏫 Canal Profesores", 0xFF1565C0);
                renderNotificationCards(binding.notificationsContainer, professorNotifs, primaryTextColor, secondaryTextColor, isDark);
                hasContent = true;
            }
        }
        if ("ALL".equals(filter) || "ROLE_ADMINS".equals(filter)) {
            if (!adminNotifs.isEmpty()) {
                addChannelHeaderView(binding.notificationsContainer, "⚙️ Canal Administradores", 0xFF00838F);
                renderNotificationCards(binding.notificationsContainer, adminNotifs, primaryTextColor, secondaryTextColor, isDark);
                hasContent = true;
            }
        }
        if ("ALL".equals(filter) || "FACULTY".equals(filter)) {
            if (!facultyNotifs.isEmpty()) {
                addChannelHeaderView(binding.notificationsContainer, "🏫 Canal Facultades", 0xFF2E7D32);
                renderNotificationCards(binding.notificationsContainer, facultyNotifs, primaryTextColor, secondaryTextColor, isDark);
                hasContent = true;
            }
        }
        if ("ALL".equals(filter) || "SUBJECT".equals(filter)) {
            if (!subjectNotifs.isEmpty()) {
                addChannelHeaderView(binding.notificationsContainer, "📚 Canal Materias", 0xFF6A1B9A);
                renderNotificationCards(binding.notificationsContainer, subjectNotifs, primaryTextColor, secondaryTextColor, isDark);
                hasContent = true;
            }
        }
        if ("ALL".equals(filter) || "FACILITY".equals(filter)) {
            if (!facilityNotifs.isEmpty()) {
                addChannelHeaderView(binding.notificationsContainer, "🏛️ Canal Instalaciones", 0xFFE65100);
                renderNotificationCards(binding.notificationsContainer, facilityNotifs, primaryTextColor, secondaryTextColor, isDark);
                hasContent = true;
            }
        }

        if (!hasContent) {
            TextView tvEmpty = new TextView(requireContext());
            tvEmpty.setText("\nNo hay avisos o notificaciones para esta categoría.\n");
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setTextColor(0xFF757575);
            binding.notificationsContainer.addView(tvEmpty);
        }
    }

    private void renderChannelChips(List<Notification> gen, List<Notification> sup, List<Notification> stu, List<Notification> prof, List<Notification> adm, List<Notification> fac, List<Notification> sub, List<Notification> fcl, int activeColor) {
        binding.chipsLayout.removeAllViews();
        List<MaterialButton> chipButtons = new ArrayList<>();

        int activeChannelsCount = 0;
        if (!gen.isEmpty()) activeChannelsCount++;
        if (!sup.isEmpty()) activeChannelsCount++;
        if (!stu.isEmpty()) activeChannelsCount++;
        if (!prof.isEmpty()) activeChannelsCount++;
        if (!adm.isEmpty()) activeChannelsCount++;
        if (!fac.isEmpty()) activeChannelsCount++;
        if (!sub.isEmpty()) activeChannelsCount++;
        if (!fcl.isEmpty()) activeChannelsCount++;

        if (activeChannelsCount > 1) {
            addChipButton(binding.chipsLayout, chipButtons, "🌐 Todos", "ALL", activeColor);
        }
        if (!gen.isEmpty()) {
            addChipButton(binding.chipsLayout, chipButtons, "📢 General", "GENERAL", activeColor);
        }
        if (!sup.isEmpty()) {
            addChipButton(binding.chipsLayout, chipButtons, "✉️ Soporte", "SUPPORT", activeColor);
        }
        if (!stu.isEmpty()) {
            addChipButton(binding.chipsLayout, chipButtons, "👨‍🎓 Estudiantes", "ROLE_STUDENTS", activeColor);
        }
        if (!prof.isEmpty()) {
            addChipButton(binding.chipsLayout, chipButtons, "👨‍🏫 Profesores", "ROLE_PROFESSORS", activeColor);
        }
        if (!adm.isEmpty()) {
            addChipButton(binding.chipsLayout, chipButtons, "⚙️ Administradores", "ROLE_ADMINS", activeColor);
        }
        if (!fac.isEmpty()) {
            addChipButton(binding.chipsLayout, chipButtons, "🏫 Facultades", "FACULTY", activeColor);
        }
        if (!sub.isEmpty()) {
            addChipButton(binding.chipsLayout, chipButtons, "📚 Materias", "SUBJECT", activeColor);
        }
        if (!fcl.isEmpty()) {
            addChipButton(binding.chipsLayout, chipButtons, "🏛️ Instalaciones", "FACILITY", activeColor);
        }
    }

    private void addChipButton(LinearLayout layout, List<MaterialButton> allChips, String label, String channelKey, int activeColor) {
        MaterialButton chip = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        chip.setText(label);
        chip.setTextSize(11);
        chip.setAllCaps(false);
        chip.setCornerRadius(30);
        chip.setInsetTop(0);
        chip.setInsetBottom(0);

        boolean isSelected = channelKey.equals(selectedChannelFilter[0]);
        updateChipStyle(chip, isSelected, activeColor);

        chip.setOnClickListener(v -> {
            selectedChannelFilter[0] = channelKey;
            for (MaterialButton btn : allChips) {
                boolean active = channelKey.equals(btn.getTag());
                updateChipStyle(btn, active, activeColor);
            }
            renderNotificationsAndChips();
        });

        chip.setTag(channelKey);
        allChips.add(chip);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 12, 0);
        chip.setLayoutParams(lp);

        layout.addView(chip);
    }

    private void updateChipStyle(MaterialButton chip, boolean isSelected, int activeColor) {
        if (isSelected) {
            chip.setBackgroundColor(activeColor);
            chip.setTextColor(0xFFFFFFFF);
            chip.setStrokeWidth(0);
        } else {
            chip.setBackgroundColor(Color.TRANSPARENT);
            chip.setTextColor(ThemeHelper.isDarkMode(requireContext()) ? 0xFFDDDDDD : 0xFF333333);
            chip.setStrokeColor(ColorStateList.valueOf(activeColor));
            chip.setStrokeWidth((int) (1.5f * getResources().getDisplayMetrics().density));
        }
    }

    private void addChannelHeaderView(LinearLayout container, String channelName, int accentColor) {
        TextView header = new TextView(requireContext());
        header.setText(channelName);
        header.setTextSize(13);
        header.setTypeface(null, Typeface.BOLD);
        header.setTextColor(0xFFFFFFFF);
        header.setPadding(24, 10, 24, 10);
        
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(accentColor);
        gd.setCornerRadius(16f);
        header.setBackground(gd);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 16, 0, 8);
        header.setLayoutParams(lp);

        container.addView(header);
    }

    private void renderNotificationCards(LinearLayout container, List<Notification> notifs, int primaryTextColor, int secondaryTextColor, boolean isDark) {
        for (Notification n : notifs) {
            MaterialCardView card = new MaterialCardView(requireContext());
            card.setCardElevation(2f);
            card.setRadius(12f);
            card.setStrokeWidth(1);
            card.setStrokeColor(isDark ? 0xFF333333 : 0xFFCCCCCC);
            card.setCardBackgroundColor(isDark ? 0xFF1E1E1E : 0xFFFFFFFF);
            
            LinearLayout cardLayout = new LinearLayout(requireContext());
            cardLayout.setOrientation(LinearLayout.VERTICAL);
            cardLayout.setPadding(16, 16, 16, 16);

            TextView tvTitle = new TextView(requireContext());
            tvTitle.setText(n.title != null ? n.title : "Notificación");
            tvTitle.setTextSize(15);
            tvTitle.setTypeface(null, Typeface.BOLD);
            tvTitle.setTextColor(primaryTextColor);

            TextView tvTargetTag = new TextView(requireContext());
            tvTargetTag.setTextSize(11);
            tvTargetTag.setTextColor(ThemeHelper.getSubjectColor(requireContext(), ThemeHelper.getAccentColorName(requireContext())));
            tvTargetTag.setText(getNotificationTargetLabel(n));

            TextView tvMsg = new TextView(requireContext());
            tvMsg.setText(n.message != null ? n.message : "");
            tvMsg.setTextSize(13);
            tvMsg.setTextColor(primaryTextColor);
            tvMsg.setPadding(0, 6, 0, 6);

        TextView tvSender = new TextView(requireContext());
        String senderStr = (n.senderName != null && !n.senderName.isEmpty() ? "Enviado por: " + n.senderName + " • " : "") + 
                           (n.timestamp != null ? n.timestamp : "");
        tvSender.setText(senderStr);
            tvSender.setTextSize(11);
            tvSender.setTextColor(secondaryTextColor);

            cardLayout.addView(tvTitle);
            cardLayout.addView(tvTargetTag);
            cardLayout.addView(tvMsg);
            cardLayout.addView(tvSender);

            if (currentUser != null && "ADMIN".equals(currentUser.role)) {
                LinearLayout actionLayout = new LinearLayout(requireContext());
                actionLayout.setOrientation(LinearLayout.HORIZONTAL);
                actionLayout.setGravity(Gravity.END);
                actionLayout.setPadding(0, 8, 0, 0);

                boolean isSupport = n.targetType != null && "SUPPORT".equalsIgnoreCase(n.targetType.trim());

                if (isSupport) {
                    MaterialButton btnProfile = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.borderlessButtonStyle);
                    btnProfile.setText("👤 Ver Perfil");
                    btnProfile.setTextSize(12);
                    btnProfile.setAllCaps(false);
                    btnProfile.setOnClickListener(v -> navigateToUserProfile(n.senderName));
                    actionLayout.addView(btnProfile);
                } else {
                    MaterialButton btnEdit = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.borderlessButtonStyle);
                    btnEdit.setText("✏️ Editar");
                    btnEdit.setTextSize(12);
                    btnEdit.setAllCaps(false);
                    btnEdit.setOnClickListener(v -> showCreateNotificationDialog(n));
                    actionLayout.addView(btnEdit);
                }

                MaterialButton btnDelete = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.borderlessButtonStyle);
                btnDelete.setText("🗑️ Eliminar");
                btnDelete.setTextColor(0xFFD32F2F);
                btnDelete.setTextSize(12);
                btnDelete.setAllCaps(false);
                btnDelete.setOnClickListener(v -> showDeleteNotificationConfirmation(n));

                actionLayout.addView(btnDelete);
                cardLayout.addView(actionLayout);
            }

            card.addView(cardLayout);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 8, 0, 8);
            card.setLayoutParams(lp);

            container.addView(card);
        }
    }

    private void navigateToUserProfile(String senderName) {
        if (senderName == null || senderName.trim().isEmpty() || getView() == null) return;

        String extractedCarnet = "";
        int start = senderName.lastIndexOf('(');
        int end = senderName.lastIndexOf(')');
        if (start != -1 && end != -1 && end > start) {
            extractedCarnet = senderName.substring(start + 1, end).trim();
        } else {
            extractedCarnet = senderName.trim();
        }

        final String searchCarnet = extractedCarnet;
        final String searchName = senderName.trim().toLowerCase();

        boolean[] found = {false};

        // Check students
        List<User> students = viewModel.getAllStudents().getValue();
        if (students != null) {
            for (User s : students) {
                if (s != null && ((s.carnet != null && s.carnet.equalsIgnoreCase(searchCarnet)) || (s.name != null && searchName.contains(s.name.toLowerCase())))) {
                    Bundle args = new Bundle();
                    args.putInt("studentId", s.id);
                    Navigation.findNavController(requireView()).navigate(R.id.action_notificationListFragment_to_studentProfileFragment, args);
                    found[0] = true;
                    return;
                }
            }
        }

        // Check professors
        List<User> profs = viewModel.getAllProfessors().getValue();
        if (!found[0] && profs != null) {
            for (User p : profs) {
                if (p != null && ((p.carnet != null && p.carnet.equalsIgnoreCase(searchCarnet)) || (p.name != null && searchName.contains(p.name.toLowerCase())))) {
                    Bundle args = new Bundle();
                    args.putInt("studentId", p.id);
                    Navigation.findNavController(requireView()).navigate(R.id.action_notificationListFragment_to_studentProfileFragment, args);
                    found[0] = true;
                    return;
                }
            }
        }

        // Check admins
        List<User> admins = viewModel.getAllAdmins().getValue();
        if (!found[0] && admins != null) {
            for (User a : admins) {
                if (a != null && ((a.carnet != null && a.carnet.equalsIgnoreCase(searchCarnet)) || (a.name != null && searchName.contains(a.name.toLowerCase())))) {
                    Bundle args = new Bundle();
                    args.putInt("adminId", a.id);
                    Navigation.findNavController(requireView()).navigate(R.id.adminDetailFragment, args);
                    found[0] = true;
                    return;
                }
            }
        }

        if (!found[0]) {
            Toast.makeText(getContext(), "Perfil del emisor no encontrado (" + searchCarnet + ")", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteNotificationConfirmation(Notification n) {
        if (n == null) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Eliminar Notificación")
                .setMessage("¿Estás seguro de que deseas eliminar esta notificación?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    viewModel.deleteNotification(n.id, () -> {
                        Toast.makeText(getContext(), "Notificación eliminada", Toast.LENGTH_SHORT).show();
                        viewModel.fetchNotifications();
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private <T> void observeOnce(LiveData<T> liveData, Observer<T> observer) {
        liveData.observe(getViewLifecycleOwner(), new Observer<T>() {
            @Override
            public void onChanged(T t) {
                if (t != null) {
                    liveData.removeObserver(this);
                    observer.onChanged(t);
                }
            }
        });
    }

    private void showCreateNotificationDialog(Notification existingNotif) {
        if (currentUser == null) return;

        boolean isEditing = existingNotif != null;
        String dialogTitle = isEditing ? "Editar Notificación" : "Crear Notificación";

        MaterialAlertDialogBuilder builder = DialogUtils.createMaterialDialog(requireContext(), dialogTitle);
        LinearLayout layout = DialogUtils.createDialogContainer(requireContext());

        final EditText etTitle = DialogUtils.createStyledEditText(requireContext(), "Título de la Notificación", 0);
        if (isEditing && existingNotif.title != null) {
            etTitle.setText(existingNotif.title);
        }
        layout.addView(etTitle);

        final EditText etMessage = DialogUtils.createStyledEditText(requireContext(), "Mensaje / Contenido de la Notificación", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        if (isEditing && existingNotif.message != null) {
            etMessage.setText(existingNotif.message);
        }
        layout.addView(etMessage);

        final String[] targetType = {isEditing && existingNotif.targetType != null ? existingNotif.targetType : "ALL"};
        final String[] targetValue = {isEditing ? existingNotif.targetValue : null};

        String initialTargetLabel = "Destinatarios: " + getNotificationTargetLabel(existingNotif);
        final TextView tvTarget = DialogUtils.createDialogOptionButton(requireContext(), initialTargetLabel, false);
        layout.addView(tvTarget);

        tvTarget.setOnClickListener(v -> {
            String[] options = {
                "📢 General (Todos los usuarios)",
                "👨‍🎓 Todos los Estudiantes",
                "👨‍🏫 Todos los Profesores",
                "⚙️ Todos los Administradores",
                "🏫 Integrantes de una Facultad...",
                "📚 Integrantes de una Materia...",
                "🏛️ Integrantes de una Instalación..."
            };

            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Seleccionar Destinatarios")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        targetType[0] = "ALL";
                        targetValue[0] = null;
                        DialogUtils.setOptionState(tvTarget, "Destinatarios: 📢 General (Todos)", false, requireContext());
                    } else if (which == 1) {
                        targetType[0] = "ROLE_STUDENTS";
                        targetValue[0] = null;
                        DialogUtils.setOptionState(tvTarget, "Destinatarios: 👨‍🎓 Todos los Estudiantes", false, requireContext());
                    } else if (which == 2) {
                        targetType[0] = "ROLE_PROFESSORS";
                        targetValue[0] = null;
                        DialogUtils.setOptionState(tvTarget, "Destinatarios: 👨‍🏫 Todos los Profesores", false, requireContext());
                    } else if (which == 3) {
                        targetType[0] = "ROLE_ADMINS";
                        targetValue[0] = null;
                        DialogUtils.setOptionState(tvTarget, "Destinatarios: ⚙️ Todos los Administradores", false, requireContext());
                    } else if (which == 4) {
                        observeOnce(viewModel.getAllFaculties(), faculties -> {
                            if (faculties != null && !faculties.isEmpty()) {
                                String[] facNames = faculties.stream().map(f -> f.name).toArray(String[]::new);
                                new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Seleccionar Facultad")
                                    .setItems(facNames, (d2, w2) -> {
                                        targetType[0] = "FACULTY";
                                        targetValue[0] = facNames[w2];
                                        DialogUtils.setOptionState(tvTarget, "Facultad: " + facNames[w2], false, requireContext());
                                    })
                                    .setNegativeButton("Atrás", (d2, w2) -> tvTarget.performClick())
                                    .setOnCancelListener(d2 -> tvTarget.performClick())
                                    .show();
                            } else {
                                Toast.makeText(getContext(), "No hay facultades registradas", Toast.LENGTH_SHORT).show();
                                tvTarget.performClick();
                            }
                        });
                    } else if (which == 5) {
                        observeOnce(viewModel.getAllSubjects(), subjects -> {
                            if (subjects != null && !subjects.isEmpty()) {
                                String[] subNames = subjects.stream().map(s -> s.name).toArray(String[]::new);
                                new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Seleccionar Materia")
                                    .setItems(subNames, (d2, w2) -> {
                                        targetType[0] = "SUBJECT";
                                        targetValue[0] = subNames[w2];
                                        DialogUtils.setOptionState(tvTarget, "Materia: " + subNames[w2], false, requireContext());
                                    })
                                    .setNegativeButton("Atrás", (d2, w2) -> tvTarget.performClick())
                                    .setOnCancelListener(d2 -> tvTarget.performClick())
                                    .show();
                            } else {
                                Toast.makeText(getContext(), "No hay materias registradas", Toast.LENGTH_SHORT).show();
                                tvTarget.performClick();
                            }
                        });
                    } else if (which == 6) {
                        observeOnce(viewModel.getAllFacilities(), facilities -> {
                            if (facilities != null && !facilities.isEmpty()) {
                                String[] facNames = facilities.stream().map(f -> f.name).toArray(String[]::new);
                                new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Seleccionar Instalación")
                                    .setItems(facNames, (d2, w2) -> {
                                        targetType[0] = "FACILITY";
                                        targetValue[0] = facNames[w2];
                                        DialogUtils.setOptionState(tvTarget, "Instalación: " + facNames[w2], false, requireContext());
                                    })
                                    .setNegativeButton("Atrás", (d2, w2) -> tvTarget.performClick())
                                    .setOnCancelListener(d2 -> tvTarget.performClick())
                                    .show();
                            } else {
                                Toast.makeText(getContext(), "No hay instalaciones registradas", Toast.LENGTH_SHORT).show();
                                tvTarget.performClick();
                            }
                        });
                    }
                }).show();
        });

        builder.setView(layout);
        builder.setPositiveButton(isEditing ? "Guardar" : "Enviar", null);
        builder.setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            hideKeyboard(etTitle);
            hideKeyboard(etMessage);
            hideKeyboard();
            String title = etTitle.getText().toString().trim();
            String message = etMessage.getText().toString().trim();

            boolean isValid = true;
            if (title.isEmpty()) {
                etTitle.setError("El título es obligatorio");
                isValid = false;
            }
            if (message.isEmpty()) {
                etMessage.setError("El mensaje es obligatorio");
                isValid = false;
            }

            if (isValid) {
                if (isEditing) {
                    existingNotif.title = title;
                    existingNotif.message = message;
                    existingNotif.targetType = targetType[0];
                    existingNotif.targetValue = targetValue[0];
                    viewModel.updateNotification(existingNotif, () -> {
                        Toast.makeText(getContext(), "¡Notificación actualizada con éxito!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        viewModel.fetchNotifications();
                    });
                } else {
                    String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
                    Notification notif = new Notification(title, message, targetType[0], targetValue[0], currentUser.name, timestamp);

                    viewModel.insertNotification(notif, () -> {
                        Toast.makeText(getContext(), "¡Notificación enviada con éxito!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        viewModel.fetchNotifications();
                    });
                }
            }
        });
    }

    private void hideKeyboard(View view) {
        if (view != null && getContext() != null) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void hideKeyboard() {
        if (getView() != null) {
            hideKeyboard(getView());
        }
    }

    private void reFilterAndRender() {
        allUserNotifications = filterNotificationsForUser(rawServerNotifications, currentUser);
        renderNotificationsAndChips();
    }

    private boolean isUserInSubject(String subjectTarget) {
        if (subjectTarget == null || subjectTarget.trim().isEmpty()) return true;
        for (String name : userSubjectNames) {
            if (name != null && name.equalsIgnoreCase(subjectTarget.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean isUserInFacility(String facilityTarget) {
        if (facilityTarget == null || facilityTarget.trim().isEmpty()) return true;
        for (String name : userFacilityNames) {
            if (name != null && name.equalsIgnoreCase(facilityTarget.trim())) {
                return true;
            }
        }
        return false;
    }

    private List<Notification> filterNotificationsForUser(List<Notification> all, User user) {
        List<Notification> result = new ArrayList<>();
        if (all == null) return result;

        for (Notification n : all) {
            if (n == null || n.targetType == null) {
                result.add(n);
                continue;
            }

            String type = n.targetType.toUpperCase();
            String val = n.targetValue != null ? n.targetValue.trim() : "";

            if ("ALL".equals(type)) {
                result.add(n);
            } else if (user != null) {
                if ("ADMIN".equals(user.role)) {
                    // Administrators can see ALL notifications across all user types, faculties, subjects, facilities, and SUPPORT
                    result.add(n);
                } else if ("SUPPORT".equals(type)) {
                    if ("ADMIN".equals(user.role)) {
                        result.add(n);
                    }
                } else if ("ROLE_STUDENTS".equals(type) && "STUDENT".equals(user.role)) {
                    result.add(n);
                } else if ("ROLE_PROFESSORS".equals(type) && "PROFESSOR".equals(user.role)) {
                    result.add(n);
                } else if ("FACULTY".equals(type)) {
                    if (user.faculty != null && user.faculty.equalsIgnoreCase(val)) {
                        result.add(n);
                    }
                } else if ("SUBJECT".equals(type)) {
                    if (isUserInSubject(val)) {
                        result.add(n);
                    }
                } else if ("FACILITY".equals(type)) {
                    if (isUserInFacility(val)) {
                        result.add(n);
                    }
                }
            }
        }
        return result;
    }

    private String getNotificationTargetLabel(Notification n) {
        if (n == null || n.targetType == null) return "[📢 General]";
        String type = n.targetType.toUpperCase();
        String val = n.targetValue != null ? n.targetValue : "";

        switch (type) {
            case "SUPPORT": return "[✉️ Soporte - Mensaje de Usuario]";
            case "ROLE_STUDENTS": return "[👨‍🎓 Todos los Estudiantes]";
            case "ROLE_PROFESSORS": return "[👨‍🏫 Todos los Profesores]";
            case "ROLE_ADMINS": return "[⚙️ Todos los Administradores]";
            case "FACULTY": return "[🏫 Facultad: " + val + "]";
            case "SUBJECT": return "[📚 Materia: " + val + "]";
            case "FACILITY": return "[🏛️ Instalación: " + val + "]";
            default: return "[📢 General (Todos los usuarios)]";
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
