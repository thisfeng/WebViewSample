package me.baron.webviewsample;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.File;


/**
 *
 *
 * fix 7.0文件访问，测试OK 8.0； 7.0； 6.0 ；4.4
 */
public class MainActivity extends AppCompatActivity {

    private ValueCallback<Uri> uploadMessage;
    private ValueCallback<Uri[]> uploadMessageAboveL;

    private static final int REQUEST_CODE_ALBUM = 0x01;
    private static final int REQUEST_CODE_CAMERA = 0x02;
    private static final int REQUEST_CODE_PERMISSION_CAMERA = 0x03;

    private String mCurrentPhotoPath;
    private String mLastPhotoPath;


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
                uploadPicture();
            }

            // For Android  >= 3.0
            public void openFileChooser(ValueCallback valueCallback, String acceptType) {
                uploadMessage = valueCallback;
                uploadPicture();


            }

            //For Android  >= 4.1
            public void openFileChooser(ValueCallback<Uri> valueCallback, String acceptType, String capture) {
                uploadMessage = valueCallback;
                uploadPicture();

            }

            // For Android >= 5.0
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                uploadMessageAboveL = filePathCallback;
                uploadPicture();
                return true;
            }
        });
        String targetUrl = "file:///android_asset/up.html";
        webview.loadUrl(targetUrl);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ALBUM || requestCode == REQUEST_CODE_CAMERA) {
            if (uploadMessage == null && uploadMessageAboveL == null) {
                return;
            }

            //取消拍照或者图片选择时
            if (resultCode != RESULT_OK) {
                //一定要返回null,否则<input file> 就是没有反应
                returnNullValue();
            }

            //拍照成功和选取照片时
            if (resultCode == RESULT_OK) {
                Uri imageUri = null;

                switch (requestCode) {
                    case REQUEST_CODE_ALBUM:

                        if (data != null) {
                            imageUri = data.getData();
                        }

                        break;
                    case REQUEST_CODE_CAMERA:

                        if (!TextUtils.isEmpty(mCurrentPhotoPath)) {
                            File file = new File(mCurrentPhotoPath);
                            Uri localUri = Uri.fromFile(file);
                            Intent localIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, localUri);
                            sendBroadcast(localIntent);
                            imageUri = Uri.fromFile(file);
                            mLastPhotoPath = mCurrentPhotoPath;
                        }
                        break;
                    default:
                }


                //上传文件
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(imageUri);
                    uploadMessage = null;
                }
                if (uploadMessageAboveL != null) {
                    uploadMessageAboveL.onReceiveValue(new Uri[]{imageUri});
                    uploadMessageAboveL = null;

                }
            }
        }
    }


    /**
     * 选择相机或者相册
     */
    public void uploadPicture() {
        String[] items = new String[]{"相机", "相册"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("请选择图片上传方式");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        if (!TextUtils.isEmpty(mLastPhotoPath)) {
                            File file = new File(mLastPhotoPath);
                            if (file != null) {
                                file.delete();
                            }
                        }
                        //请求拍照权限
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            takePhoto();
                        } else {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_PERMISSION_CAMERA);
                        }

                        break;
                    case 1:
                        chooseAlbumPic();
                        break;
                    default:
                }
            }
        });

        //取消对话框时候置空
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {

                //一定要返回null,否则<input type='file'>
                returnNullValue();
            }
        });


        builder.show();

    }

    /**
     * 解决再次点击无响应问题
     */
    private void returnNullValue() {
        if (uploadMessage != null) {
            uploadMessage.onReceiveValue(null);
            uploadMessage = null;
        }
        if (uploadMessageAboveL != null) {
            uploadMessageAboveL.onReceiveValue(null);
            uploadMessageAboveL = null;

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePhoto();
                } else {
                    // Permission Denied
                    new AlertDialog.Builder(this)
                            .setTitle("无法拍照")
                            .setMessage("您未授予拍照权限")
                            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    returnNullValue();
                                }
                            })
                            .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent localIntent = new Intent();
                                    localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                                    localIntent.setData(Uri.fromParts("package", getPackageName(), null));
                                    startActivity(localIntent);
                                }
                            }).show();
                }
                break;
            default:
        }

    }

    /**
     * 拍照
     */
    private void takePhoto() {

        StringBuilder fileName = new StringBuilder();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri takeUri = null;

        fileName.append(System.currentTimeMillis()).append("_temp.png");
        File tempFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName.toString());
        //适配7.0文件访问,通过此方式拿到文件地址
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            takeUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".android7.fileprovider", tempFile);
        } else {
            takeUri = Uri.fromFile(tempFile);
        }

        intent.putExtra(MediaStore.EXTRA_OUTPUT, takeUri);

        mCurrentPhotoPath = tempFile.getAbsolutePath();
        startActivityForResult(intent, REQUEST_CODE_CAMERA);

    }

    /**
     * 选择相册照片
     */
    private void chooseAlbumPic() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(Intent.createChooser(i, "Image Chooser"), REQUEST_CODE_ALBUM);
    }
}
