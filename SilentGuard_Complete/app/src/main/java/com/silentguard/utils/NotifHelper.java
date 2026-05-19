package com.silentguard.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class NotifHelper {

    public static void createChannel(Context ctx, String id, String name, int importance) {
        try {
            // Use reflection to call NotificationChannel on API 26+
            Class<?> channelClass = Class.forName("android.app.NotificationChannel");
            Object channel = channelClass.getConstructor(String.class, CharSequence.class, int.class)
                .newInstance(id, name, importance);
            NotificationManager nm = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.getClass().getMethod("createNotificationChannel", channelClass)
                .invoke(nm, channel);
        } catch (Exception e) {
            // API < 26, channels not needed
        }
    }

    @SuppressWarnings({"deprecation", "all"})
    public static Notification buildNotification(Context ctx, String channelId,
            String title, String text, int icon, boolean ongoing) {
        try {
            // Try API 26+ builder with channel
            Class<?> builderClass = Notification.Builder.class;
            Object builder = builderClass.getConstructor(Context.class, String.class)
                .newInstance(ctx, channelId);
            builderClass.getMethod("setContentTitle", CharSequence.class).invoke(builder, title);
            builderClass.getMethod("setContentText", CharSequence.class).invoke(builder, text);
            builderClass.getMethod("setSmallIcon", int.class).invoke(builder, icon);
            builderClass.getMethod("setOngoing", boolean.class).invoke(builder, ongoing);
            return (Notification) builderClass.getMethod("build").invoke(builder);
        } catch (Exception e) {
            // Fallback
            return new Notification.Builder(ctx)
                .setContentTitle(title).setContentText(text)
                .setSmallIcon(icon).setOngoing(ongoing).build();
        }
    }

    public static void startFgService(Context ctx, android.content.Intent intent) {
        try {
            // Try startForegroundService via reflection
            ctx.getClass().getMethod("startForegroundService", android.content.Intent.class)
                .invoke(ctx, intent);
        } catch (Exception e) {
            ctx.startService(intent);
        }
    }

    // NotificationManager importance constants
    public static final int IMPORTANCE_MIN  = 1;
    public static final int IMPORTANCE_HIGH = 4;
}
