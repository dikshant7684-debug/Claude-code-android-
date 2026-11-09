package com.example.extensions.github;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GitHubRepositoryClient {
    private static final String TAG = "GitHubClient";
    private static final String GITHUB_API_BASE = "https://api.github.com";

    public static class RepoTarget {
        public String owner;
        public String repo;
        public String branch = "main";
        public String subpath = "";

        public RepoTarget(String owner, String repo) {
            this.owner = owner;
            this.repo = repo;
        }
    }

    public static class RepoFileInfo {
        public String name;
        public String path;
        public String type; // "file" or "dir"
        public String downloadUrl;
        public long size;
    }

    /**
     * Parses diverse GitHub URLs:
     * - https://github.com/owner/repo
     * - https://github.com/owner/repo.git
     * - https://github.com/owner/repo/tree/branch/subpath
     * - owner/repo
     */
    public static RepoTarget parseGitHubUrl(String rawUrl) {
        if (rawUrl == null) return null;
        String clean = rawUrl.trim();
        clean = clean.replace("https://github.com/", "").replace("http://github.com/", "").replace("git@github.com:", "");
        if (clean.endsWith(".git")) clean = clean.substring(0, clean.length() - 4);
        clean = clean.replaceAll("^/+", "").replaceAll("/+$", "");

        String[] parts = clean.split("/");
        if (parts.length < 2) return null;

        RepoTarget target = new RepoTarget(parts[0], parts[1]);
        if (parts.length >= 4 && "tree".equals(parts[2])) {
            target.branch = parts[3];
            if (parts.length > 4) {
                StringBuilder sb = new StringBuilder();
                for (int i = 4; i < parts.length; i++) {
                    if (sb.length() > 0) sb.append("/");
                    sb.append(parts[i]);
                }
                target.subpath = sb.toString();
            }
        }
        return target;
    }

    /**
     * Fetches metadata for a GitHub repository.
     */
    public JSONObject getRepositoryMetadata(RepoTarget target, String token) throws IOException {
        String endpoint = GITHUB_API_BASE + "/repos/" + target.owner + "/" + target.repo;
        String json = executeGet(endpoint, token);
        try {
            return new JSONObject(json);
        } catch (Exception e) {
            throw new IOException("Failed to parse repo metadata JSON: " + e.getMessage());
        }
    }

    /**
     * Lists directory contents of a GitHub repository path.
     */
    public List<RepoFileInfo> listPathContents(RepoTarget target, String path, String token) throws IOException {
        String cleanPath = path != null ? path.trim() : "";
        String endpoint = GITHUB_API_BASE + "/repos/" + target.owner + "/" + target.repo + "/contents/" + cleanPath;
        if (target.branch != null && !target.branch.isEmpty()) {
            endpoint += "?ref=" + target.branch;
        }

        String json = executeGet(endpoint, token);
        List<RepoFileInfo> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                RepoFileInfo info = new RepoFileInfo();
                info.name = obj.optString("name");
                info.path = obj.optString("path");
                info.type = obj.optString("type");
                info.downloadUrl = obj.optString("download_url");
                info.size = obj.optLong("size", 0);
                list.add(info);
            }
        } catch (Exception e) {
            // Might be a single file
            try {
                JSONObject obj = new JSONObject(json);
                RepoFileInfo info = new RepoFileInfo();
                info.name = obj.optString("name");
                info.path = obj.optString("path");
                info.type = obj.optString("type");
                info.downloadUrl = obj.optString("download_url");
                info.size = obj.optLong("size", 0);
                list.add(info);
            } catch (Exception ex) {
                throw new IOException("Unexpected contents response format: " + e.getMessage());
            }
        }
        return list;
    }

    /**
     * Fetches text content of a file directly from GitHub.
     */
    public String fetchFileRawText(String downloadUrl, String token) throws IOException {
        return executeGet(downloadUrl, token);
    }

    /**
     * Downloads the repository archive ZIP to a destination file.
     */
    public File downloadRepositoryZip(RepoTarget target, File destinationZipFile, String token) throws IOException {
        String zipUrl = GITHUB_API_BASE + "/repos/" + target.owner + "/" + target.repo + "/zipball/" + target.branch;
        URL url = new URL(zipUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(25000);
        conn.setReadTimeout(35000);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "NEU-CODE-Android-Agent");
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        if (token != null && !token.trim().isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token.trim());
        }

        int code = conn.getResponseCode();
        // Handle redirect
        if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM || code == 307) {
            String newUrl = conn.getHeaderField("Location");
            conn.disconnect();
            conn = (HttpURLConnection) new URL(newUrl).openConnection();
            conn.setConnectTimeout(25000);
            conn.setReadTimeout(35000);
            conn.setRequestProperty("User-Agent", "NEU-CODE-Android-Agent");
            code = conn.getResponseCode();
        }

        if (code != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP " + code + " while downloading repo archive from " + zipUrl);
        }

        try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(destinationZipFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } finally {
            conn.disconnect();
        }

        return destinationZipFile;
    }

    private String executeGet(String endpoint, String token) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(25000);
        conn.setRequestProperty("User-Agent", "NEU-CODE-Android-Agent");
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        if (token != null && !token.trim().isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token.trim());
        }

        int code = conn.getResponseCode();
        if (code == 403 || code == 429) {
            throw new IOException("GitHub API rate limit reached (HTTP " + code + "). Please configure a personal access token or wait.");
        }
        if (code == 404) {
            throw new IOException("GitHub repository or resource not found: " + endpoint);
        }
        if (code != 200) {
            throw new IOException("GitHub request failed with HTTP " + code);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
        } finally {
            conn.disconnect();
        }
        return response.toString();
    }
}
