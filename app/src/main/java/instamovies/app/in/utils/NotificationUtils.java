package instamovies.app.in.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import instamovies.app.in.MainActivity;
import instamovies.app.in.R;

public class NotificationUtils {

    private final Context context;

    public NotificationUtils(Context context) {
        this.context = context;
    }

    public void MoviesNotificationService(String messageTitle, String messageBody) {
        int notificationId = 100;
        String channelId = context.getString(R.string.movies_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        CharSequence channelName = "Movies";
        String channelDescription = "Channel for movie notifications (Do not turn off this notification service).";

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_baseline_notifications_black_24)
                .setContentTitle(messageTitle)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(pendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH);
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel Channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            Channel.setDescription(channelDescription);
            Channel.enableLights(true);
            Channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            Channel.setShowBadge(true);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(Channel);
            }
        }

        if (notificationManager != null) {
            notificationId++;
            notificationManager.notify(notificationId, notificationBuilder.build());
        }
    }

    public void DefaultNotificationService(String messageTitle, String messageBody) {
        int notificationId = 200;
        String channelId = context.getString(R.string.default_notification_channel_id);
        CharSequence channelName = "Default";
        String channelDescription = "Channel for Default Notifications";

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_baseline_notifications_black_24)
                .setContentTitle(messageTitle)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(pendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationBuilder.setPriority(NotificationManager.IMPORTANCE_DEFAULT);
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel Channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            Channel.setDescription(channelDescription);
            Channel.enableLights(true);
            Channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            Channel.setShowBadge(true);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(Channel);
            }
        }

        if (notificationManager != null) {
            notificationId++;
            notificationManager.notify(notificationId, notificationBuilder.build());
        }
    }

    public void CreateUpdateChannel() {
        String channelId = context.getString(R.string.update_notification_channel_id);
        CharSequence channelName = "Update";
        String channelDescription = "Channel for update notifications (Do not turn off this notification service).";
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel Channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            Channel.setDescription(channelDescription);
            Channel.enableLights(true);
            Channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            Channel.setShowBadge(true);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(Channel);
            }
        }
    }

    public void CreateMoviesChannel() {
        String channelId = context.getString(R.string.movies_notification_channel_id);
        CharSequence channelName = "Movies";
        String channelDescription = "Channel for movie notifications (Do not turn off this notification service).";
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel Channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            Channel.setDescription(channelDescription);
            Channel.enableLights(true);
            Channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            Channel.setShowBadge(true);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(Channel);
            }
        }
    }
}