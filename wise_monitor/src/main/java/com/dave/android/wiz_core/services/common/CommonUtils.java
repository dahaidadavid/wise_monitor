package com.dave.android.wiz_core.services.common;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Debug;
import android.os.StatFs;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import com.dave.android.wiz_core.WiseInitCenter;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;
import javax.crypto.Cipher;

/**
 * @author rendawei
 * @since 2018/6/5
 */
public class CommonUtils {

    private static final String LOG_PRIORITY_NAME_ASSERT = "A";
    private static final String LOG_PRIORITY_NAME_DEBUG = "D";
    private static final String LOG_PRIORITY_NAME_ERROR = "E";
    private static final String LOG_PRIORITY_NAME_INFO = "I";
    private static final String LOG_PRIORITY_NAME_VERBOSE = "V";
    private static final String LOG_PRIORITY_NAME_WARN = "W";
    private static final String LOG_PRIORITY_NAME_UNKNOWN = "?";

    public static final String SHA1_INSTANCE = "SHA-1";
    public static final String SHA256_INSTANCE = "SHA-256";
    public static final String GOOGLE_SDK = "google_sdk";
    public static final String SDK = "sdk";

    private static final char[] HEX_VALUES = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private static final int BYTES_IN_A_GIGABYTE = 1073741824;
    private static final int BYTES_IN_A_MEGABYTE = 1048576;
    private static final int BYTES_IN_A_KILOBYTE = 1024;
    private static long totalRamInBytes = -1L;

    public static final Comparator<File> FILE_MODIFIED_COMPARATOR = new Comparator<File>() {
        public int compare(File file0, File file1) {
            return (int) (file0.lastModified() - file1.lastModified());
        }
    };

    public CommonUtils() {
    }

    public static String extractFieldFromSystemFile(File file, String fieldname) {
        String toReturn = null;
        if (file.exists()) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(file), BYTES_IN_A_KILOBYTE);
                String line;
                while ((line = br.readLine()) != null) {
                    Pattern pattern = Pattern.compile("\\s*:\\s*");
                    String[] pieces = pattern.split(line, 2);
                    if (pieces.length > 1 && pieces[0].equals(fieldname)) {
                        toReturn = pieces[1];
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeOrLog(br, "Failed to close system file reader.");
            }
        }

        return toReturn;
    }

    public static int getCpuArchitectureInt() {
        return CommonUtils.Architecture.getValue().ordinal();
    }

    public static synchronized long getTotalRamInBytes() {
        if (totalRamInBytes == -1L) {
            long bytes = 0L;
            String result = extractFieldFromSystemFile(new File("/proc/meminfo"), "MemTotal");
            if (!TextUtils.isEmpty(result)) {
                result = result.toUpperCase(Locale.US);

                try {
                    if (result.endsWith("KB")) {
                        bytes = convertMemInfoToBytes(result, "KB", BYTES_IN_A_KILOBYTE);
                    } else if (result.endsWith("MB")) {
                        bytes = convertMemInfoToBytes(result, "MB", BYTES_IN_A_MEGABYTE);
                    } else if (result.endsWith("GB")) {
                        bytes = convertMemInfoToBytes(result, "GB", BYTES_IN_A_GIGABYTE);
                    } else {
                    }
                } catch (NumberFormatException var4) {
                }
            }

            totalRamInBytes = bytes;
        }

        return totalRamInBytes;
    }

    static long convertMemInfoToBytes(String memInfo, String notation, int notationMultiplier) {
        return Long.parseLong(memInfo.split(notation)[0].trim()) * (long) notationMultiplier;
    }

    public static RunningAppProcessInfo getAppProcessInfo(String packageName, Context context) {
        ActivityManager actman = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> processes = actman.getRunningAppProcesses();
        RunningAppProcessInfo procInfo = null;
        if (processes != null) {
            Iterator var5 = processes.iterator();

            while (var5.hasNext()) {
                RunningAppProcessInfo info = (RunningAppProcessInfo) var5.next();
                if (info.processName.equals(packageName)) {
                    procInfo = info;
                    break;
                }
            }
        }

        return procInfo;
    }

    public static String streamToString(InputStream is) throws IOException {
        Scanner s = (new Scanner(is)).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static String sha1(String source) {
        return hash(source, SHA1_INSTANCE);
    }

    public static String sha256(String source) {
        return hash(source, SHA256_INSTANCE);
    }

    public static String sha1(InputStream source) {
        return hash(source, SHA1_INSTANCE);
    }

    private static String hash(String s, String algorithm) {
        return hash(s.getBytes(), algorithm);
    }

    private static String hash(InputStream source, String sha1Instance) {
        try {
            MessageDigest digest = MessageDigest.getInstance(sha1Instance);
            byte[] buffer = new byte[1024];
            boolean var4 = false;

            int length;
            while ((length = source.read(buffer)) != -1) {
                digest.update(buffer, 0, length);
            }

            return hexify(digest.digest());
        } catch (Exception var5) {
            return "";
        }
    }

    private static String hash(byte[] bytes, String algorithm) {
        MessageDigest digest = null;

        try {
            digest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException var4) {
            return "";
        }

        digest.update(bytes);
        return hexify(digest.digest());
    }

    public static String createInstanceIdFrom(String... sliceIds) {
        if (sliceIds != null && sliceIds.length != 0) {
            List<String> sliceIdList = new ArrayList<>();
            for (String id : sliceIds) {
                if (id != null) {
                    sliceIdList.add(id.replace("-", "").toLowerCase(Locale.US));
                }
            }
            Collections.sort(sliceIdList);
            StringBuilder sb = new StringBuilder();
            Iterator var7 = sliceIdList.iterator();

            while (var7.hasNext()) {
                String id = (String) var7.next();
                sb.append(id);
            }

            String concatValue = sb.toString();
            return concatValue.length() > 0 ? sha1(concatValue) : null;
        } else {
            return null;
        }
    }

    public static long calculateFreeRamInBytes(Context context) {
        MemoryInfo mi = new MemoryInfo();
        ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(mi);
        return mi.availMem;
    }

    public static long calculateUsedDiskSpaceInBytes(String path) {
        StatFs statFs = new StatFs(path);
        long blockSizeBytes = (long) statFs.getBlockSize();
        long totalSpaceBytes = blockSizeBytes * (long) statFs.getBlockCount();
        long availableSpaceBytes = blockSizeBytes * (long) statFs.getAvailableBlocks();
        return totalSpaceBytes - availableSpaceBytes;
    }

    public static Float getBatteryLevel(Context context) {
        IntentFilter filter = new IntentFilter("android.intent.action.BATTERY_CHANGED");
        Intent battery = context.registerReceiver(null, filter);
        if (battery == null) {
            return null;
        } else {
            int level = battery.getIntExtra("level", -1);
            int scale = battery.getIntExtra("scale", -1);
            return (float) level / (float) scale;
        }
    }

    public static boolean getProximitySensorEnabled(Context context) {
        if (isEmulator(context)) {
            return false;
        } else {
            SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            Sensor prox = sm.getDefaultSensor(8);
            return prox != null;
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public static boolean isLoggingEnabled(Context context) {
        return false;
    }

    public static boolean getBooleanResourceValue(Context context, String key,
            boolean defaultValue) {
        if (context != null) {
            Resources resources = context.getResources();
            if (resources != null) {
                int id = getResourcesIdentifier(context, key, "bool");
                if (id > 0) {
                    return resources.getBoolean(id);
                }

                id = getResourcesIdentifier(context, key, "string");
                if (id > 0) {
                    return Boolean.parseBoolean(context.getString(id));
                }
            }
        }

        return defaultValue;
    }

    public static int getResourcesIdentifier(Context context, String key, String resourceType) {
        Resources resources = context.getResources();
        return resources.getIdentifier(key, resourceType, getResourcePackageName(context));
    }

    public static boolean isEmulator(Context context) {
        String androidId = Secure.getString(context.getContentResolver(), "android_id");
        return SDK.equals(Build.PRODUCT) || GOOGLE_SDK.equals(Build.PRODUCT) || androidId == null;
    }

    public static boolean isRooted(Context context) {
        boolean isEmulator = isEmulator(context);
        String buildTags = Build.TAGS;
        if (!isEmulator && buildTags != null && buildTags.contains("test-keys")) {
            return true;
        } else {
            File file = new File("/system/app/Superuser.apk");
            if (file.exists()) {
                return true;
            } else {
                file = new File("/system/xbin/su");
                return !isEmulator && file.exists();
            }
        }
    }

    public static boolean isDebuggerAttached() {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger();
    }

    public static int getDeviceState(Context context) {
        int deviceState = 0;
        if (isEmulator(context)) {
            deviceState |= 1;
        }

        if (isRooted(context)) {
            deviceState |= 2;
        }

        if (isDebuggerAttached()) {
            deviceState |= 4;
        }

        return deviceState;
    }

    public static int getBatteryVelocity(Context context, boolean powerConnected) {
        Float batteryLevel = getBatteryLevel(context);
        if (powerConnected && batteryLevel != null) {
            if ((double) batteryLevel >= 99.0D) {
                return 3;
            } else {
                return (double) batteryLevel < 99.0D ? 2 : 0;
            }
        } else {
            return 1;
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public static Cipher createCipher(int mode, String key) throws InvalidKeyException {
        throw new InvalidKeyException("This method is deprecated");
    }

    public static String hexify(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];

        for (int i = 0; i < bytes.length; ++i) {
            int v = bytes[i] & 255;
            hexChars[i * 2] = HEX_VALUES[v >>> 4];
            hexChars[i * 2 + 1] = HEX_VALUES[v & 15];
        }

        return new String(hexChars);
    }

    public static byte[] dehexify(String string) {
        int len = string.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(string.charAt(i), 16) << 4) + Character
                    .digit(string.charAt(i + 1), 16));
        }

        return data;
    }

    public static boolean isAppDebuggable(Context context) {
        return (context.getApplicationInfo().flags & 2) != 0;
    }

    public static String getStringsFileValue(Context context, String key) {
        int id = getResourcesIdentifier(context, key, "string");
        return id > 0 ? context.getString(id) : "";
    }

    public static void closeOrLog(Closeable c, String message) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException var3) {
            }
        }

    }

    public static void flushOrLog(Flushable f, String message) {
        if (f != null) {
            try {
                f.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.length() == 0;
    }

    public static String padWithZerosToMaxIntWidth(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be zero or greater");
        } else {
            return String.format(Locale.US, "%1$10s", value).replace(' ', '0');
        }
    }

    public static boolean stringsEqualIncludingNull(String s1, String s2) {
        if (s1 == s2) {
            return true;
        } else {
            return s1 != null ? s1.equals(s2) : false;
        }
    }

    public static String getResourcePackageName(Context context) {
        int iconId = context.getApplicationContext().getApplicationInfo().icon;
        return iconId > 0 ? context.getResources().getResourcePackageName(iconId)
                : context.getPackageName();
    }

    public static void copyStream(InputStream is, OutputStream os, byte[] buffer)
            throws IOException {
        int count;
        while ((count = is.read(buffer)) != -1) {
            os.write(buffer, 0, count);
        }

    }

    public static String logPriorityToString(int priority) {
        switch (priority) {
            case 2:
                return LOG_PRIORITY_NAME_VERBOSE;
            case 3:
                return LOG_PRIORITY_NAME_DEBUG;
            case 4:
                return LOG_PRIORITY_NAME_INFO;
            case 5:
                return LOG_PRIORITY_NAME_WARN;
            case 6:
                return LOG_PRIORITY_NAME_ERROR;
            case 7:
                return LOG_PRIORITY_NAME_ASSERT;
            default:
                return LOG_PRIORITY_NAME_UNKNOWN;
        }
    }

    public static String getAppIconHashOrNull(Context context) {
        InputStream is = null;
        try {
            is = context.getResources().openRawResource(getAppIconResourceId(context));
            String sha1 = sha1(is);
            String var3 = isNullOrEmpty(sha1) ? null : sha1;
            return var3;
        } catch (Exception var7) {
        } finally {
            closeOrLog(is, "Failed to close icon input stream.");
        }

        return null;
    }

    public static int getAppIconResourceId(Context context) {
        return context.getApplicationContext().getApplicationInfo().icon;
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException var2) {
                throw var2;
            } catch (Exception var3) {
                ;
            }
        }

    }

    public static boolean checkPermission(Context context, String permission) {
        int res = context.checkCallingOrSelfPermission(permission);
        return res == PackageManager.PERMISSION_GRANTED;
    }

    public static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void openKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInputFromInputMethod(view.getWindowToken(), 0);
        }
    }

    @TargetApi(16)
    public static void finishAffinity(Context context, int resultCode) {
        if (context instanceof Activity) {
            finishAffinity((Activity) context, resultCode);
        }

    }

    @TargetApi(16)
    public static void finishAffinity(Activity activity, int resultCode) {
        if (activity != null) {
            if (VERSION.SDK_INT >= 16) {
                activity.finishAffinity();
            } else {
                activity.setResult(resultCode);
                activity.finish();
            }

        }
    }

    @SuppressLint({"MissingPermission"})
    public static boolean canTryConnection(Context context) {
        if (!checkPermission(context, permission.ACCESS_NETWORK_STATE)) {
            return true;
        } else {
            ConnectivityManager cm = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
    }

    public static void logOrThrowIllegalStateException(String logTag, String errorMsg) {
        if (WiseInitCenter.isDebuggable()) {
            throw new IllegalStateException(errorMsg);
        } else {
            WiseInitCenter.getLogger().e(logTag, errorMsg);
        }
    }

    public static void logOrThrowIllegalArgumentException(String logTag, String errorMsg) {
        if (WiseInitCenter.isDebuggable()) {
            throw new IllegalArgumentException(errorMsg);
        } else {
            WiseInitCenter.getLogger().e(logTag, errorMsg);
        }
    }

    enum Architecture {
        X86_32,
        X86_64,
        ARM_UNKNOWN,
        PPC,
        PPC64,
        ARMV6,
        ARMV7,
        UNKNOWN,
        ARMV7S,
        ARM64;

        private static final Map<String, Architecture> matcher = new HashMap<>(4);

        Architecture() {
        }

        static CommonUtils.Architecture getValue() {
            String arch = Build.CPU_ABI;
            if (TextUtils.isEmpty(arch)) {
                return UNKNOWN;
            } else {
                arch = arch.toLowerCase(Locale.US);
                CommonUtils.Architecture value = matcher.get(arch);
                if (value == null) {
                    value = UNKNOWN;
                }

                return value;
            }
        }

        static {
            matcher.put("armeabi-v7a", ARMV7);
            matcher.put("armeabi", ARMV6);
            matcher.put("arm64-v8a", ARM64);
            matcher.put("x86", X86_32);
        }
    }
}
