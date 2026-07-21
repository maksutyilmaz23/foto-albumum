package com.mesut.fotoalbum;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST = 1001;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        }

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setDatabaseEnabled(true);

        webView.addJavascriptInterface(new FileSaverBridge(), "AndroidFileSaver");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                              FileChooserParams params) {
                filePathCallback = callback;
                Intent intent = params.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback == null) return;
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    results = new Uri[]{data.getData()};
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /** Lets the web page hand PDF (base64) or JSON (plain text) content to native code
     *  to save into the Downloads folder — WebView doesn't support browser-style downloads. */
    private class FileSaverBridge {
        @JavascriptInterface
        public void saveBase64(final String base64Data, final String fileName) {
            writeToDownloads(Base64.decode(base64Data, Base64.DEFAULT), fileName);
        }

        @JavascriptInterface
        public void saveText(final String text, final String fileName) {
            writeToDownloads(text.getBytes(java.nio.charset.StandardCharsets.UTF_8), fileName);
        }

        private void writeToDownloads(final byte[] bytes, final String fileName) {
            runOnUiThread(() -> {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        saveViaMediaStore(bytes, fileName);
                    } else {
                        saveLegacy(bytes, fileName);
                    }
                    Toast.makeText(MainActivity.this, "Kaydedildi: İndirilenler/" + fileName, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Kaydedilemedi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }

        private String mimeTypeFor(String fileName) {
            if (fileName.toLowerCase().endsWith(".json")) return "application/json";
            if (fileName.toLowerCase().endsWith(".pdf")) return "application/pdf";
            return "application/octet-stream";
        }

        /** Android 10+ (API 29+): direct File writes to public Downloads are blocked by
         *  Scoped Storage, so we must go through MediaStore instead. */
        private void saveViaMediaStore(byte[] bytes, String fileName) throws Exception {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(android.provider.MediaStore.Downloads.MIME_TYPE, mimeTypeFor(fileName));
            values.put(android.provider.MediaStore.Downloads.IS_PENDING, 1);

            android.content.ContentResolver resolver = getContentResolver();
            Uri collection = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI;
            Uri item = resolver.insert(collection, values);
            if (item == null) throw new java.io.IOException("MediaStore kaydı oluşturulamadı");

            try (java.io.OutputStream out = resolver.openOutputStream(item)) {
                if (out == null) throw new java.io.IOException("Çıkış akışı açılamadı");
                out.write(bytes);
            }
            values.clear();
            values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0);
            resolver.update(item, values, null, null);
        }

        /** Pre-Android 10: plain File writes to the public Downloads directory still work. */
        private void saveLegacy(byte[] bytes, String fileName) throws Exception {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists()) dir.mkdirs();
            File outFile = new File(dir, fileName);
            FileOutputStream fos = new FileOutputStream(outFile);
            fos.write(bytes);
            fos.close();
        }
    }
}
