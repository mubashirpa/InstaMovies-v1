package instamovies.app.in.utils;

import android.content.Context;
import android.content.ContextWrapper;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileUtil {

    @NotNull
    public static String readFile(File file) {
        FileInputStream inputStream;
        StringBuilder sb = new StringBuilder();
        try {
            inputStream =  new FileInputStream(file);
            while (inputStream.available() > 0){
                try {
                    sb.append((char)inputStream.read());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static boolean isExistFile(File file) {
        return file.exists();
    }

    @NotNull
    public static String getPackageDataDir(@NotNull Context context) {
        return new ContextWrapper(context).getExternalFilesDir(null).getAbsolutePath();
    }

    public static void deleteFile(File path) {
        if (path.exists()) {
            //noinspection ResultOfMethodCallIgnored
            path.delete();
        }
    }

    public static boolean deleteDirectory(File path) {
        boolean result = false;
        if (path.exists()) {
            result = path.delete();
        }
        return result;
    }

    public static void deleteCache(Context context){
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception ignored){
        }
    }

    @Contract("null -> false")
    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()){
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()){
            return dir.delete();
        } else {
            return false;
        }
    }
}