package instamovies.app.in.service;

import android.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import org.jetbrains.annotations.NotNull;

import instamovies.app.in.utils.NotificationUtils;

public class FirebaseCloudMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";

    @Override
    public void onMessageReceived(@NotNull RemoteMessage remoteMessage) {

        NotificationUtils notificationUtils = new NotificationUtils(getApplicationContext());
        notificationUtils.CreateMoviesChannel();
        notificationUtils.CreateUpdateChannel();

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    @Override
    public void onNewToken(@NotNull String token) {
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        Log.d(TAG, "Refreshed token: " + token);
    }
}