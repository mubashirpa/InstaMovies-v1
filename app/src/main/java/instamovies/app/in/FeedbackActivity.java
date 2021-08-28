package instamovies.app.in;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import instamovies.app.in.utils.AppUtils;
import instamovies.app.in.utils.FileUtil;

public class FeedbackActivity extends AppCompatActivity {

    private final int REQUEST_CODE_ACCOUNTS = 1000;
    private final int REQUEST_CODE_STORAGE = 1001;
    private String imagePath = "";
    private String imageName = "";
    private final String aboutPhone =
            "OS VERSION - "+System.getProperty("os.version")+"\n"
                    +"SDK VERSION - "+Build.VERSION.SDK_INT+"\n"
                    +"DEVICE - "+Build.DEVICE+"\n"
                    +"MODEL - "+Build.MODEL+"\n"
                    +"MANUFACTURER - "+Build.MANUFACTURER;
    private Spinner emailSpinner;
    private EditText feedbackText;
    private ImageView screenshotImage;
    private RelativeLayout screenshotLayout;
    private StorageReference storageReference;
    private List<String> emailList = new ArrayList<>();
    private final FirebaseFirestore feedbackDatabase = FirebaseFirestore.getInstance();
    private Map<String, Object> Details = new HashMap<>();
    private AlertDialog progressDialog;
    private AlertDialog.Builder permissionDialog;
    private Context context;
    private String TERMS_URL, PRIVACY_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        context = this;

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(context, R.color.colorLayoutPrimary));
        if ((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                //Deprecated in Api level 30
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

            } else {

                //Deprecated in Api level 23
                window.setStatusBarColor(Color.BLACK);

            }
        }

        SharedPreferences settingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String themePreference = settingsPreferences.getString("theme_preference","");
        switch (themePreference) {
            case "System default":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case "Set by Battery Saver":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                break;
            case "Light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "Dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            initializeActivity();
        } else {
            permissionDialog = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
            permissionDialog.setTitle("Need permission");
            permissionDialog.setMessage("To select email to send us feedback, give us permission to your accounts");
            permissionDialog.setPositiveButton("GRANT PERMISSION", (dialogInterface, i) -> ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.GET_ACCOUNTS, Manifest.permission.READ_CONTACTS}, REQUEST_CODE_ACCOUNTS));
            permissionDialog.setNegativeButton("EXIT", ((dialog, which) -> finish()));
            permissionDialog.setCancelable(false);
            permissionDialog.create().show();
        }
    }

    private void initializeActivity() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        toolbar.setNavigationOnClickListener(_v -> onBackPressed());
        emailSpinner = findViewById(R.id.email_spinner);
        feedbackText = findViewById(R.id.feedback_text);
        ImageView unpickImage = findViewById(R.id.unpick_image);
        ImageView pickImage = findViewById(R.id.imagePicker);
        screenshotLayout = findViewById(R.id.layout_screenshot);
        screenshotImage = findViewById(R.id.screenshot_img);
        screenshotImage.setClipToOutline(true);
        storageReference = FirebaseStorage.getInstance().getReference();
        progressDialog = new AlertDialog.Builder(context).create();
        TERMS_URL = getString(R.string.terms_url);
        PRIVACY_URL = getString(R.string.privacy_url);

        TextView infoText = findViewById(R.id.info_text);
        String feedbackInfo = getString(R.string.feedback_info);
        SpannableString spannableString = new SpannableString(feedbackInfo);
        ClickableSpan privacySpan = new ClickableSpan() {
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setColor(ContextCompat.getColor(context, R.color.colorLinkText));
                ds.setUnderlineText(false);
            }

            @Override
            public void onClick(@NonNull View widget) {
                Intent webIntent = new Intent();
                webIntent.setClass(context, HiddenWebActivity.class);
                webIntent.putExtra("HIDDEN_URL", PRIVACY_URL);
                startActivity(webIntent);
            }
        };
        ClickableSpan termsSpan = new ClickableSpan() {
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setColor(ContextCompat.getColor(context, R.color.colorLinkText));
                ds.setUnderlineText(false);
            }

            @Override
            public void onClick(@NonNull View widget) {
                Intent webIntent = new Intent();
                webIntent.setClass(context, HiddenWebActivity.class);
                webIntent.putExtra("HIDDEN_URL", TERMS_URL);
                startActivity(webIntent);
            }
        };
        spannableString.setSpan(privacySpan, 166, 180, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(termsSpan, 185, 201, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        infoText.setHighlightColor(Color.TRANSPARENT);
        infoText.setText(spannableString);
        infoText.setMovementMethod(LinkMovementMethod.getInstance());

        getAccounts();

        unpickImage.setOnClickListener(view -> {
            imageName = "";
            imagePath = "";
            screenshotLayout.setVisibility(View.GONE);
        });

        pickImage.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                pickImage();
            } else {
                permissionDialog = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
                permissionDialog.setTitle("Need permission");
                permissionDialog.setMessage("To select the screenshot to send, give us permission to use your storage");
                permissionDialog.setPositiveButton("GRANT PERMISSION", (dialogInterface, i) -> ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE));
                permissionDialog.setNegativeButton("CANCEL", (dialogInterface, i) -> {
                });
                permissionDialog.setCancelable(false);
                permissionDialog.create().show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        switch (requestCode) {
            case REQUEST_CODE_ACCOUNTS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initializeActivity();
                } else {
                    AppUtils.toastError(context, FeedbackActivity.this, getString(R.string.error_permission_denied, "accounts"));
                    finish();
                }
                return;
            case REQUEST_CODE_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickImage();
                } else {
                    AppUtils.toastError(context, FeedbackActivity.this, getString(R.string.error_permission_denied, "storage"));
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_feedback, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getTitle().equals(getString(R.string.send))) {
            if (feedbackText.getText().toString().equals("")) {
                AppUtils.toast(context,FeedbackActivity.this, "Write your feedback before sending");
            } else {
                if (emailList.size() == 0) {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    AppUtils.toastError(context,FeedbackActivity.this, getString(R.string.error_default));
                    sendUsingMail();
                } else {
                    showProgressDialog();
                    if (!imagePath.equals("") || FileUtil.isExistFile(new File(imagePath))){
                        uploadImage(imagePath, imageName);
                    } else {
                        String timeStamp = new SimpleDateFormat("dd/MM hh:mm a", Locale.getDefault()).format(new Date());
                        Details = new HashMap<>();
                        Details.put("Message", feedbackText.getText().toString());
                        Details.put("Time", timeStamp);
                        Details.put("Contact", emailSpinner.getSelectedItem().toString());
                        Details.put("DeviceInfo",aboutPhone);
                        sendFeedback();
                    }
                }
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    int resultCode = result.getResultCode();
                    Intent data = result.getData();
                    if (resultCode == Activity.RESULT_OK) {
                        if (data != null) {
                            Uri contentUri = data.getData();
                            String timeStamp = new SimpleDateFormat("dd/MM hh:mm a", Locale.getDefault()).format(new Date());
                            String imageFileName = "IMG_" + timeStamp + "." + getFileExt(contentUri);
                            imagePath = String.valueOf(contentUri);
                            imageName = imageFileName;
                            screenshotImage.setImageURI(contentUri);
                            screenshotLayout.setVisibility(View.VISIBLE);
                        } else {
                            AppUtils.toastError(context, FeedbackActivity.this, "Failed select image");
                        }
                    } else {
                        imagePath = "";
                        imageName = "";
                        screenshotLayout.setVisibility(View.GONE);
                    }
                }
            });

    private String getFileExt(Uri contentUri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap typeMap = MimeTypeMap.getSingleton();
        return typeMap.getExtensionFromMimeType(contentResolver.getType(contentUri));
    }

    private void sendFeedback() {
        feedbackDatabase.collection("Feedback")
                .add(Details).addOnSuccessListener(documentReference -> {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            feedbackText.setText("");
            imagePath = "";
            imageName = "";
            screenshotLayout.setVisibility(View.GONE);
            AppUtils.toast(context,FeedbackActivity.this, "Thank you for the Feedback");
        }).addOnFailureListener(e -> {
            AppUtils.toastError(context,FeedbackActivity.this, "Failed to Send Feedback");
            if (progressDialog.isShowing()){
                progressDialog.dismiss();
            }
        });
    }

    private void getAccounts() {
        emailList = new ArrayList<>();
        Pattern emailPattern = Patterns.EMAIL_ADDRESS;
        Account[] accounts = AccountManager.get(this).getAccounts();
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches()) {
                emailList.add(account.name);
            }
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, emailList);
        emailSpinner.setAdapter(arrayAdapter);
    }

    private void uploadImage(String Path, String Name) {
        Uri file = Uri.parse(Path);
        StorageReference riversReference = storageReference.child("Screenshot/"+Name);
        riversReference.putFile(file).addOnSuccessListener(taskSnapshot -> {
            String downloadUrl;
            downloadUrl = riversReference.getDownloadUrl().toString();
            String timeStamp = new SimpleDateFormat("dd/MM hh:mm a", Locale.getDefault()).format(new Date());
            Details = new HashMap<>();
            Details.put("Message", feedbackText.getText().toString());
            Details.put("Time", timeStamp);
            Details.put("Contact", emailSpinner.getSelectedItem().toString());
            Details.put("DeviceInfo", aboutPhone);
            Details.put("screenShot", downloadUrl);
            sendFeedback();
        }).addOnFailureListener(e -> {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            AppUtils.toastError(context,FeedbackActivity.this, getString(R.string.error_default));
            sendUsingMail();
        });
    }

    private void pickImage(){
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activityResultLauncher.launch(pickIntent);
    }

    private void showProgressDialog() {
        progressDialog = new AlertDialog.Builder(context).create();
        Window window = progressDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
        LayoutInflater inflater = getLayoutInflater();
        View convertView = inflater.inflate(R.layout.layout_progress_dialog, null);
        progressDialog.setView(convertView);
        progressDialog.setTitle(null);
        TextView titleText = convertView.findViewById(R.id.title_text);
        ImageView dialogCloseButton = convertView.findViewById(R.id.dialog_close_button);
        titleText.setText(getString(R.string.sending));
        dialogCloseButton.setOnClickListener(v -> progressDialog.dismiss());
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    private void sendUsingMail() {
        Intent mailIntent = new Intent(Intent.ACTION_SEND);
        mailIntent.setType("text/plain");
        mailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {getString(R.string.developer_email)});
        mailIntent.putExtra(Intent.EXTRA_SUBJECT, "Insta Movies - User Feedback");
        mailIntent.putExtra(Intent.EXTRA_TEXT, feedbackText.getText()+"\n\n"+aboutPhone);
        if (!imagePath.equals("") || FileUtil.isExistFile(new File(imagePath))){
            Uri uri = Uri.parse(imagePath);
            mailIntent.putExtra(Intent.EXTRA_STREAM, uri);
        }
        Intent chooserInt = Intent.createChooser(mailIntent,"Select Email");
        try {
            startActivity(chooserInt);
        } catch (ActivityNotFoundException e){
            AppUtils.toastError(context,FeedbackActivity.this, "Failed to send email");
        }
    }
}