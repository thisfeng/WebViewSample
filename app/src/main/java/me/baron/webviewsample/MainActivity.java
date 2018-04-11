package me.baron.webviewsample;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * 使用前务必允许文件和拍照权限,以下省略此步 自行根据项目获取权限或直接去设置页面手动打开 ，否则无法打开相机
 *
 * fix 7.0文件访问，测试OK 8.0； 7.0； 6.0 ；4.4
 */
public class MainActivity extends AppCompatActivity {

    private ValueCallback<Uri> uploadMessage;
    private ValueCallback<Uri[]> uploadMessageAboveL;
    private final static int FILE_CHOOSER_RESULT_CODE = 10000;

    /**
     * 拍照的临时图片uri
     */
    private Uri tokeImageUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WebView webview = (WebView) findViewById(R.id.web_view);
        assert webview != null;
        WebSettings settings = webview.getSettings();
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setJavaScriptEnabled(true);
        webview.setWebChromeClient(new WebChromeClient() {

            // For Android < 3.0
            public void openFileChooser(ValueCallback<Uri> valueCallback) {
                uploadMessage = valueCallback;
//                openImageChooserActivity();
                takePhotoOrOpenFileChooser();

            }

            // For Android  >= 3.0
            public void openFileChooser(ValueCallback valueCallback, String acceptType) {
                uploadMessage = valueCallback;
//                openImageChooserActivity();
                takePhotoOrOpenFileChooser();

            }

            //For Android  >= 4.1
            public void openFileChooser(ValueCallback<Uri> valueCallback, String acceptType, String capture) {
                uploadMessage = valueCallback;
//                openImageChooserActivity();
                takePhotoOrOpenFileChooser();

            }

            // For Android >= 5.0
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                uploadMessageAboveL = filePathCallback;
//                    openImageChooserActivity();
                takePhotoOrOpenFileChooser();
                return true;
            }
        });
        String targetUrl = "file:///android_asset/up.html";
        webview.loadUrl(targetUrl);
    }

    private void openImageChooserActivity() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(Intent.createChooser(i, "Image Chooser"), FILE_CHOOSER_RESULT_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (null == uploadMessage && null == uploadMessageAboveL) {
                return;
            }
            Uri result = data == null || resultCode != RESULT_OK ? null : data.getData();
            if (uploadMessageAboveL != null) {

                onActivityResultAboveL(requestCode, resultCode, data);
            } else if (uploadMessage != null) {
                if (result != null) {
                    uploadMessage.onReceiveValue(result);
                } else {
                    //result等于空的话是拍照的
                    uploadMessage.onReceiveValue(tokeImageUri);
                }
            }
            uploadMessage = null;

        }
    }

    /**
     * 5.0以上
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void onActivityResultAboveL(int requestCode, int resultCode, Intent intent) {
        if (requestCode != FILE_CHOOSER_RESULT_CODE || uploadMessageAboveL == null) {
            return;
        }
        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            if (intent != null) {
                String dataString = intent.getDataString();
                ClipData clipData = intent.getClipData();
                if (clipData != null) {
                    results = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        results[i] = item.getUri();
                    }
                }
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                    uploadMessageAboveL.onReceiveValue(results);
                } else {
                    results = new Uri[]{tokeImageUri};
                    uploadMessageAboveL.onReceiveValue(results);
                }
            } else {
                results = new Uri[]{tokeImageUri};
                uploadMessageAboveL.onReceiveValue(results);
            }
        }
        uploadMessageAboveL = null;
    }


    /**
     * 系统方式 打开拍照或文件选择
     */
    private void takePhotoOrOpenFileChooser() {

        File file = new File(Environment.getExternalStorageDirectory(), "/temp/" + System.currentTimeMillis() + ".jpg");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        //通过FileProvider创建一个content:// 前缀类型的Uri
        tokeImageUri = getUriForFile(this, file);
        final List<Intent> cameraIntents = new ArrayList<>();
        final Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent i = new Intent(captureIntent);
            i.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            i.setPackage(packageName);
            i.putExtra(MediaStore.EXTRA_OUTPUT, tokeImageUri);
            cameraIntents.add(i);
        }


        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        Intent chooserIntent = Intent.createChooser(i, "图像选择器");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));
        startActivityForResult(chooserIntent, FILE_CHOOSER_RESULT_CODE);
    }

    /**
     * 适配7.0文件访问,通过此方式拿到文件地址
     * Uri fileUri = FileProvider7.getUriForFile(this, file);
     *
     * @param context
     * @param file
     * @return
     */

    public static Uri getUriForFile(Context context, File file) {
        Uri fileUri = null;
        if (Build.VERSION.SDK_INT >= 24) {
            fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".android7.fileprovider", file);
        } else {
            fileUri = Uri.fromFile(file);
        }
        return fileUri;
    }
}
