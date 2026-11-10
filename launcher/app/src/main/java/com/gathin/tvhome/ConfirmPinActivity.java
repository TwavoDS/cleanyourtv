package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Android asks the default Home app to confirm when an app wants to pin a shortcut ("Add to home screen"
 * in an app). This accepts it straight away, shows a short note, and lets Home add the tile. Widget
 * requests are not supported and are left unanswered, which the requesting app reads as declined.
 */
public class ConfirmPinActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            LauncherApps la = getSystemService(LauncherApps.class);
            LauncherApps.PinItemRequest req = la != null ? la.getPinItemRequest(getIntent()) : null;
            if (req != null && req.isValid() && req.getRequestType() == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
                ShortcutInfo si = req.getShortcutInfo();
                if (req.accept()) {
                    CharSequence label = si != null ? si.getShortLabel() : null;
                    Toast.makeText(this, getString(R.string.shortcut_added, label != null ? label : ""), Toast.LENGTH_SHORT).show();
                    sendBroadcast(new Intent(PinnedShortcuts.ACTION_CHANGED).setPackage(getPackageName()));
                }
            }
        } catch (Exception ignored) {
            // A request that vanished meanwhile: nothing to add.
        }
        finish();
    }
}
