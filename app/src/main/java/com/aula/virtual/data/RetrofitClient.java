package com.aula.virtual.data;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // URL activa de Cloudflare Quick Tunnel (Actualizada automaticamente por run_tunnel_and_update_app.py):
    private static final String BASE_URL = "https://reaction-mats-what-drum.trycloudflare.com/";

    private static Retrofit retrofit = null;
    private static String activeUrl = BASE_URL;

    public static void setCustomUrl(String customUrl) {
        if (customUrl != null && !customUrl.trim().isEmpty()) {
            if (!customUrl.endsWith("/")) customUrl += "/";
            activeUrl = customUrl;
            retrofit = null; // Rebuild client with new URL
        }
    }

    public static String getActiveUrl() {
        return activeUrl;
    }

    public static ApiService getApiService() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(activeUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
