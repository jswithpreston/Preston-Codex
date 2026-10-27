package com.preston.codex;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ManifestPermissionTest {

    @Test
    public void manifest_hasAllowBackupFalse() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        PackageManager pm = context.getPackageManager();
        ApplicationInfo appInfo = pm.getApplicationInfo(context.getPackageName(), 0);

        assertFalse("allowBackup must be false in manifest", (appInfo.flags & ApplicationInfo.FLAG_ALLOW_BACKUP) != 0);
    }

    @Test
    public void manifest_requestedPermissions_containsExpectedAndNoInternet() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        PackageManager pm = context.getPackageManager();
        PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);

        assertNotNull(packageInfo);
        assertNotNull(packageInfo.requestedPermissions);

        List<String> permissions = Arrays.asList(packageInfo.requestedPermissions);

        assertTrue("Must declare RECEIVE_BOOT_COMPLETED", permissions.contains("android.permission.RECEIVE_BOOT_COMPLETED"));
        assertTrue("Must declare POST_NOTIFICATIONS", permissions.contains("android.permission.POST_NOTIFICATIONS"));

        assertFalse("Locked decision: strictly NO internet permission allowed", permissions.contains("android.permission.INTERNET"));
        assertFalse("Locked decision: no exact alarm permission", permissions.contains("android.permission.SCHEDULE_EXACT_ALARM"));
        assertFalse("Locked decision: no use exact alarm permission", permissions.contains("android.permission.USE_EXACT_ALARM"));
    }
}
