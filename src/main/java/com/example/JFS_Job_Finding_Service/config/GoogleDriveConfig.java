package com.example.JFS_Job_Finding_Service.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import java.io.*;


public class GoogleDriveConfig {
    private static final String CREDENTIALS_FILE_PATH = "/home/thanglm2006/IdeaProjects/JFS_Job_Finding_Service/src/main/resources/otherAPI/client_secret_830114601569-l50umpg29a837skdlagcoagsqhrftrtf.apps.googleusercontent.com.json";

    public static Drive getDriveService() throws Exception {
        HttpTransport httpTransport = new NetHttpTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        InputStream in = new FileInputStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(in));

        Credential credential = new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(jsonFactory)
                .setClientSecrets(clientSecrets)
                .build()
                .setAccessToken("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");

        return new Drive.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("Job Finding Service")
                .build();
    }
}
