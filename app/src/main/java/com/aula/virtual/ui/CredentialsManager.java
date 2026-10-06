package com.aula.virtual.ui;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CredentialsManager {
    private static final String PREF_NAME = "secure_credentials";
    private static final String KEY_ACCOUNTS = "remembered_accounts";
    private SharedPreferences sharedPreferences;

    public CredentialsManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            sharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    public void saveCredentials(String carnet, String password) {
        saveCredentials(carnet, password, carnet);
    }

    public void saveCredentials(String carnet, String password, String name) {
        if (sharedPreferences == null) return;
        
        sharedPreferences.edit()
                .putString(carnet, password)
                .putString(carnet + "_name", name != null ? name : carnet)
                .apply();
        
        Set<String> accounts = sharedPreferences.getStringSet(KEY_ACCOUNTS, new HashSet<>());
        Set<String> updatedAccounts = new HashSet<>(accounts);
        updatedAccounts.add(carnet);
        sharedPreferences.edit().putStringSet(KEY_ACCOUNTS, updatedAccounts).apply();
    }

    public String getUserName(String carnet) {
        if (sharedPreferences == null) return carnet;
        return sharedPreferences.getString(carnet + "_name", carnet);
    }

    public String getPassword(String carnet) {
        if (sharedPreferences == null) return null;
        return sharedPreferences.getString(carnet, null);
    }

    public List<String> getRememberedAccounts() {
        if (sharedPreferences == null) return new ArrayList<>();
        Set<String> accounts = sharedPreferences.getStringSet(KEY_ACCOUNTS, new HashSet<>());
        return new ArrayList<>(accounts);
    }

    public void removeCredentials(String carnet) {
        if (sharedPreferences == null) return;
        
        sharedPreferences.edit()
                .remove(carnet)
                .remove(carnet + "_name")
                .apply();
        
        Set<String> accounts = sharedPreferences.getStringSet(KEY_ACCOUNTS, new HashSet<>());
        Set<String> updatedAccounts = new HashSet<>(accounts);
        updatedAccounts.remove(carnet);
        sharedPreferences.edit().putStringSet(KEY_ACCOUNTS, updatedAccounts).apply();
    }

    public void clearAllCredentials() {
        if (sharedPreferences == null) return;
        sharedPreferences.edit().clear().apply();
    }
}
