package com.gathin.tvhome;

import android.content.Context;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.os.Process;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shortcuts an app asked to pin to Home (ShortcutManager.requestPinShortcut, Android 8+): a channel, a
 * film, a web page, a Plex library. Android itself remembers which shortcuts this launcher pinned, so
 * there is no file of our own: the launcher asks LauncherApps for them, and unpins one to remove it.
 * Only the default Home app may do this; on any other launcher the list is simply empty.
 */
final class PinnedShortcuts {
    /** Sent (to this package only) after a shortcut was pinned or removed, so Home rebuilds its tiles. */
    static final String ACTION_CHANGED = "com.gathin.tvhome.SHORTCUTS_CHANGED";
    /** A shortcut tile's component class: "shortcut:" + id under the app's package, so keys stay unique. */
    static final String PREFIX = "shortcut:";

    private PinnedShortcuts() { }

    static List<ShortcutInfo> load(Context ctx) {
        try {
            LauncherApps la = ctx.getSystemService(LauncherApps.class);
            if (la == null || !la.hasShortcutHostPermission()) return Collections.emptyList();
            LauncherApps.ShortcutQuery q = new LauncherApps.ShortcutQuery()
                    .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);
            List<ShortcutInfo> all = la.getShortcuts(q, Process.myUserHandle());
            if (all == null) return Collections.emptyList();
            List<ShortcutInfo> out = new ArrayList<>();
            for (ShortcutInfo si : all) if (si.isPinned() && si.isEnabled()) out.add(si);
            return out;
        } catch (Exception e) {
            Log.w("TvHome.Shortcuts", "Could not read pinned shortcuts", e);
            return Collections.emptyList();
        }
    }

    /** Unpins one shortcut by keeping all other pinned shortcuts of that app. */
    static boolean remove(Context ctx, String pkg, String id) {
        try {
            LauncherApps la = ctx.getSystemService(LauncherApps.class);
            if (la == null || !la.hasShortcutHostPermission()) return false;
            List<String> keep = new ArrayList<>();
            for (ShortcutInfo si : load(ctx)) {
                if (si.getPackage().equals(pkg) && !si.getId().equals(id)) keep.add(si.getId());
            }
            la.pinShortcuts(pkg, keep, Process.myUserHandle());
            return true;
        } catch (Exception e) {
            Log.w("TvHome.Shortcuts", "Could not unpin " + pkg + "/" + id, e);
            return false;
        }
    }

    static boolean start(Context ctx, String pkg, String id) {
        try {
            LauncherApps la = ctx.getSystemService(LauncherApps.class);
            if (la == null) return false;
            la.startShortcut(pkg, id, null, null, Process.myUserHandle());
            return true;
        } catch (Exception e) {
            Log.w("TvHome.Shortcuts", "Could not open " + pkg + "/" + id, e);
            return false;
        }
    }

    /**
     * Name of a shortcut's own picture (Knop's web page, or files/banners/): the package, "@", and 12 hex
     * characters of the shortcut id's SHA-1. An id can hold any character; this key is always a safe file name.
     */
    static String imageKey(String pkg, String id) {
        try {
            byte[] d = java.security.MessageDigest.getInstance("SHA-1").digest(id.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(pkg).append('@');
            for (int i = 0; i < 6; i++) sb.append(String.format(java.util.Locale.ROOT, "%02x", d[i] & 0xFF));
            return sb.toString();
        } catch (Exception e) {
            return pkg;
        }
    }

    private static String published;

    /**
     * Tells Knop (be.toivo.knop) which shortcuts are on Home, so its web page lists them for a picture.
     * Only when the list changed; nothing happens without Knop.
     */
    static void publish(Context ctx, java.util.ArrayList<String> keys, java.util.ArrayList<String> labels) {
        String sig = keys + "|" + labels;
        if (sig.equals(published)) return;
        try {
            android.os.Bundle b = new android.os.Bundle();
            b.putStringArrayList("keys", keys);
            b.putStringArrayList("labels", labels);
            ctx.getContentResolver().call(android.net.Uri.parse("content://be.toivo.knop.banners"), "shortcuts", null, b);
            published = sig;
        } catch (Exception ignored) {
            // No Knop installed.
        }
    }

    /** The shortcut id behind a tile's component class, or null for an ordinary app tile. */
    static String idOf(String className) {
        return className != null && className.startsWith(PREFIX) ? className.substring(PREFIX.length()) : null;
    }
}
