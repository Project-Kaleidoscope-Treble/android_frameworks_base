/*
 * Copyright (C) 2020 The Pixel Experience Project
 *               2022 Project Kaleidoscope
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.internal.util.kscope;

import android.app.Application;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CustomPropsUtils {

    private static final String PACKAGE_GMS = "com.google.android.gms";
    private static final String PROCESS_UNSTABLE = "com.google.android.gms.unstable";
    private static final String PACKAGE_FINSKY = "com.android.vending";

    private static final String SPOOF_MUSIC_APPS = "persist.sys.disguise_props_for_music_app";

    private static final String TAG = CustomPropsUtils.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final Map<String, Object> propsToChangePixelXL;
    private static final String[] packagesToChangePixelXL = {
            "com.google.android.apps.photos"
    };

    private static final Map<String, Object> propsToChangeMeizu;
    private static final String[] packagesToChangeMeizu = {
            "com.netease.cloudmusic",
            "com.tencent.qqmusic",
            "com.kugou.android",
            "com.kugou.android.lite",
            "cmccwm.mobilemusic",
            "cn.kuwo.player",
            "com.meizu.media.music"
    };

    private static volatile boolean sIsGms = false;
    private static volatile boolean sIsFinsky = false;

    static {
        propsToChangePixelXL = new HashMap<>();
        propsToChangePixelXL.put("BRAND", "google");
        propsToChangePixelXL.put("MANUFACTURER", "Google");
        propsToChangePixelXL.put("DEVICE", "marlin");
        propsToChangePixelXL.put("PRODUCT", "marlin");
        propsToChangePixelXL.put("MODEL", "Pixel XL");
        propsToChangePixelXL.put(
            "FINGERPRINT", "google/marlin/marlin:10/QP1A.191005.007.A3/5972272:user/release-keys");
        propsToChangeMeizu = new HashMap<>();
        propsToChangeMeizu.put("BRAND", "meizu");
        propsToChangeMeizu.put("MANUFACTURER", "Meizu");
        propsToChangeMeizu.put("DEVICE", "m1892");
        propsToChangeMeizu.put("DISPLAY", "Flyme");
        propsToChangeMeizu.put("PRODUCT", "meizu_16thPlus_CN");
        propsToChangeMeizu.put("MODEL", "meizu 16th Plus");
    }

    public static boolean setPropsForGms(String packageName) {
        if (packageName.equals(PACKAGE_GMS)) {
            final String processName = Application.getProcessName();

            if (PROCESS_UNSTABLE.equals(processName)) {
                sIsGms = true;

                if (DEBUG) Log.d(TAG, "Spoofing build for GMS");
                setPropValue("DEVICE", "walleye");
                setPropValue("PRODUCT", "walleye");
                setPropValue("MODEL", "Pixel 2");
                setPropValue("FINGERPRINT", "google/walleye/walleye:8.1.0/OPM1.171019.011/4448085:user/release-keys");
                setVersionField("DEVICE_INITIAL_SDK_INT", Build.VERSION_CODES.O);
            }
        } else if (packageName.equals(PACKAGE_FINSKY)) {
            sIsFinsky = true;
        } else {
            return false;
        }

        return true;
    }

    public static void setProps(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return;
        }
        if (setPropsForGms(packageName)) {
            return;
        }
        if (Arrays.asList(packagesToChangePixelXL).contains(packageName)) {
            if (DEBUG) Log.d(TAG, "Defining props for: " + packageName);
            for (Map.Entry<String, Object> prop : propsToChangePixelXL.entrySet()) {
                setPropValue(prop.getKey(), prop.getValue());
            }
        }
        if ((SystemProperties.getBoolean(SPOOF_MUSIC_APPS, false)) &&
            (Arrays.asList(packagesToChangeMeizu).contains(packageName))) {
            if (DEBUG) Log.d(TAG, "Defining props for: " + packageName);
            for (Map.Entry<String, Object> prop : propsToChangeMeizu.entrySet()) {
                setPropValue(prop.getKey(), prop.getValue());
            }
        }
    }

    private static void setPropValue(String key, Object value) {
        try {
            if (DEBUG) Log.d(TAG, "Defining prop " + key + " to " + value.toString());
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
    }

    private static void setVersionField(String key, Integer value) {
        try {
            if (DEBUG) Log.d(TAG, "Defining version field " + key + " to " + value.toString());
            Field field = Build.VERSION.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to set version field " + key, e);
        }
    }

    private static boolean isCallerSafetyNet() {
        return sIsGms && Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(elem -> elem.getClassName().contains("DroidGuard"));
    }

    public static void onEngineGetCertificateChain() {
        // Check stack for SafetyNet or Play Integrity
        if (isCallerSafetyNet() || sIsFinsky) {
            throw new UnsupportedOperationException();
        }
    }
}
