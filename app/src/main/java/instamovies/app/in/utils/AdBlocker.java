package instamovies.app.in.utils;

import android.content.Context;
import android.text.TextUtils;
import android.webkit.WebResourceResponse;
import androidx.annotation.WorkerThread;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import okhttp3.HttpUrl;
import okio.BufferedSource;
import okio.Okio;

public class AdBlocker {

    private static final Set<String> AD_HOSTS = new HashSet<>();

    public static void init(Context context) {
        new Thread(() -> {
            try {
                loadFromAssets(context);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static boolean isAd(String url){
        HttpUrl httpUrl = HttpUrl.parse(url);
        return isAdHost(httpUrl != null ? httpUrl.host() : "");
    }

    @NotNull
    @Contract(" -> new")
    public static WebResourceResponse createEmptyResource(){
        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
    }

    @Nullable
    @WorkerThread
    private static Void loadFromAssets(@NotNull Context context) throws IOException {
        InputStream stream = context.getAssets().open("ad-servers.txt");
        BufferedSource buffer = Okio.buffer(Okio.source(stream));
        String line;
        while ((line = buffer.readUtf8Line()) != null){
            AD_HOSTS.add(line);
        }
        buffer.close();
        stream.close();
        return null;
    }

    private static boolean isAdHost(String host){
        if (TextUtils.isEmpty(host)){
            return false;
        }
        int index = host.indexOf(".");
        return index >= 0 && (AD_HOSTS.contains(host) ||
                index + 1 < host.length() && isAdHost(host.substring(index + 1)));
    }
}