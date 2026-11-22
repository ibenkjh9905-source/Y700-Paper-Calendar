package com.example.papercalendar;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A read-only calendar dashboard for Android tablets.
 * Google account synchronization is managed by Android, not by this app.
 * The app never sends calendar data to any server.
 */
public class MainActivity extends Activity {
    private static final int PERMISSION_REQUEST = 108;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger latestRequest = new AtomicInteger();
    private WebView webView;
    private boolean pageReady = false;
    private int requestedYear;
    private int requestedMonthZeroBased;
    private final Runnable refreshTask = new Runnable() {
        @Override public void run() {
            if (pageReady && hasPermission()) loadCalendar(requestedYear, requestedMonthZeroBased);
            mainHandler.postDelayed(this, 60_000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Calendar now = Calendar.getInstance();
        requestedYear = now.get(Calendar.YEAR);
        requestedMonthZeroBased = now.get(Calendar.MONTH);
        getWindow().setStatusBarColor(android.graphics.Color.rgb(234, 233, 228));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(234, 233, 228));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        webView = new WebView(this);
        webView.setBackgroundColor(android.graphics.Color.rgb(234, 233, 228));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setSupportZoom(false);
        settings.setDefaultTextEncodingName("UTF-8");
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                pageReady = true;
                if (hasPermission()) {
                    pushJavaScript("CalendarUI.permission(true)");
                } else {
                    requestPermissions(new String[] { Manifest.permission.READ_CALENDAR }, PERMISSION_REQUEST);
                }
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !url.startsWith("file:///android_asset/");
            }
        });
        webView.addJavascriptInterface(new LocalCalendarBridge(), "AndroidCalendar");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    private boolean hasPermission() {
        return checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == PERMISSION_REQUEST) {
            boolean ok = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
            pushJavaScript("CalendarUI.permission(" + (ok ? "true" : "false") + ")");
        }
    }

    private void pushJavaScript(String expression) {
        runOnUiThread(() -> {
            if (pageReady && webView != null) webView.evaluateJavascript("window." + expression + ";", null);
        });
    }

    private void loadCalendar(int year, int month) {
        if (!hasPermission()) {
            pushJavaScript("CalendarUI.permission(false)");
            return;
        }
        final int requestNo = latestRequest.incrementAndGet();
        new Thread(() -> {
            JSONObject payload = new JSONObject();
            JSONArray calendars = new JSONArray();
            JSONArray events = new JSONArray();
            try {
                try (Cursor cursor = getContentResolver().query(
                        CalendarContract.Calendars.CONTENT_URI,
                        new String[] {
                                CalendarContract.Calendars._ID,
                                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                                CalendarContract.Calendars.VISIBLE
                        },
                        CalendarContract.Calendars.VISIBLE + "=1", null,
                        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + " ASC")) {
                    if (cursor != null) while (cursor.moveToNext()) {
                        JSONObject item = new JSONObject();
                        item.put("id", cursor.getLong(0));
                        item.put("name", cursor.isNull(1) ? "캘린더" : cursor.getString(1));
                        calendars.put(item);
                    }
                }

                Calendar begin = Calendar.getInstance();
                begin.clear();
                begin.set(year, month, 1, 0, 0, 0);
                begin.add(Calendar.DAY_OF_MONTH, -8);
                Calendar end = Calendar.getInstance();
                end.clear();
                end.set(year, month + 2, 1, 0, 0, 0);
                end.add(Calendar.DAY_OF_MONTH, 8);

                Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
                ContentUris.appendId(builder, begin.getTimeInMillis());
                ContentUris.appendId(builder, end.getTimeInMillis());
                try (Cursor cursor = getContentResolver().query(
                        builder.build(),
                        new String[] {
                                CalendarContract.Instances.EVENT_ID,
                                CalendarContract.Instances.TITLE,
                                CalendarContract.Instances.BEGIN,
                                CalendarContract.Instances.END,
                                CalendarContract.Instances.ALL_DAY,
                                CalendarContract.Instances.CALENDAR_ID
                        },
                        CalendarContract.Instances.VISIBLE + "=1",
                        null,
                        CalendarContract.Instances.BEGIN + " ASC")) {
                    if (cursor != null) while (cursor.moveToNext()) {
                        JSONObject item = new JSONObject();
                        item.put("id", cursor.getLong(0) + "-" + cursor.getLong(2));
                        item.put("title", cursor.isNull(1) ? "(제목 없음)" : cursor.getString(1));
                        item.put("start", cursor.getLong(2));
                        item.put("end", cursor.getLong(3));
                        item.put("allDay", cursor.getInt(4) != 0);
                        item.put("calendarId", cursor.getLong(5));
                        events.put(item);
                    }
                }
                payload.put("calendars", calendars);
                payload.put("events", events);
            } catch (Exception error) {
                try {
                    payload.put("calendars", calendars);
                    payload.put("events", events);
                    payload.put("error", error.getClass().getSimpleName());
                } catch (Exception ignored) { }
            }
            if (requestNo == latestRequest.get()) {
                pushJavaScript("CalendarUI.receiveNative(" + payload.toString() + ")");
            }
        }, "calendar-read-thread").start();
    }

    private class LocalCalendarBridge {
        @JavascriptInterface public void requestMonth(int year, int monthZeroBased) {
            if (year < 1900 || year > 2200 || monthZeroBased < 0 || monthZeroBased > 11) return;
            requestedYear = year;
            requestedMonthZeroBased = monthZeroBased;
            loadCalendar(year, monthZeroBased);
        }

        @JavascriptInterface public void setKeepAwake(boolean enabled) {
            runOnUiThread(() -> {
                if (enabled) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
        }
    }

    @Override protected void onResume() {
        super.onResume();
        mainHandler.removeCallbacks(refreshTask);
        mainHandler.postDelayed(refreshTask, 60_000);
        // Restoring a background activity does not reload index.html. The WebView
        // must explicitly reset its selected date to *today*, then request events
        // for the current month from the device's calendar provider.
        if (pageReady) pushJavaScript("CalendarUI.onAppResume()");
    }

    @Override protected void onPause() {
        mainHandler.removeCallbacks(refreshTask);
        super.onPause();
    }

    @Override protected void onDestroy() {
        mainHandler.removeCallbacks(refreshTask);
        pageReady = false;
        if (webView != null) {
            webView.removeJavascriptInterface("AndroidCalendar");
            webView.destroy();
        }
        super.onDestroy();
    }
}
