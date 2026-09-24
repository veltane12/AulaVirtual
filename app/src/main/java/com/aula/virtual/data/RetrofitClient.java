package com.aula.virtual.data;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // IMPORTANTE: Cambia esta IP por la IP local de tu PC donde corre FastAPI
    // Puedes obtenerla con 'ipconfig' en la terminal de Windows.
    // Ej: "http://192.168.1.15:8000/"
    private static final String BASE_URL = "http://192.168.1.35:8000/"; // IP para el emulador de Android (localhost de la PC)

    private static Retrofit retrofit = null;

    public static ApiService getApiService() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
