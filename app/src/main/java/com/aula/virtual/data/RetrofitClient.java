package com.aula.virtual.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_SERVER_URL = "server_url";

    public static final String EMULATOR_URL = "http://10.0.2.2:8000/";
    public static final String BASE_URL = "https://personnel-cents-resolve-specifics.trycloudflare.com/";

    private static Retrofit retrofit = null;
    private static String activeUrl = null;

    public static String getActiveUrl(Context context) {
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String savedUrl = prefs.getString(KEY_SERVER_URL, null);
            
            if (savedUrl != null && !savedUrl.trim().isEmpty()) {
                // If saved URL is a Cloudflare Tunnel URL and BASE_URL changed, update to active BASE_URL
                if (savedUrl.contains("trycloudflare.com") && !savedUrl.trim().equalsIgnoreCase(BASE_URL.trim())) {
                    activeUrl = BASE_URL;
                    prefs.edit().putString(KEY_SERVER_URL, BASE_URL).apply();
                    retrofit = null;
                } else {
                    activeUrl = savedUrl;
                }
            } else {
                activeUrl = BASE_URL;
            }
        } else if (activeUrl == null) {
            activeUrl = BASE_URL;
        }

        if (!activeUrl.endsWith("/")) {
            activeUrl += "/";
        }
        return activeUrl;
    }

    public static String getActiveUrl() {
        return getActiveUrl(null);
    }

    public static void setCustomUrl(Context context, String customUrl) {
        if (customUrl != null && !customUrl.trim().isEmpty()) {
            String url = customUrl.trim();
            if (!url.endsWith("/")) url += "/";
            activeUrl = url;
            if (context != null) {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit().putString(KEY_SERVER_URL, activeUrl).apply();
            }
            retrofit = null;
        }
    }

    public static void setCustomUrl(String customUrl) {
        setCustomUrl(null, customUrl);
    }

    public static void resetClient() {
        retrofit = null;
    }

    public static ApiService getApiService() {
        return getApiService(null);
    }

    public static ApiService getApiService(Context context) {
        String currentUrl = getActiveUrl(context);
        if (retrofit == null || !retrofit.baseUrl().toString().equalsIgnoreCase(currentUrl)) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(currentUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
