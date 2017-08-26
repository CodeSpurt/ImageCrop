package com.codespurt.imagecrop;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button capture;
    private ImageView image;

    private final int PERMISSION_CAMERA = 1001;
    private final int PERMISSION_STORAGE = 1002;

    private final int CAMERA_CAPTURE = 1010;
    private final int IMAGE_CROP = 1011;

    private boolean isExternalStorageAvailable = false;
    private String directoryPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        capture = (Button) findViewById(R.id.btn_capture);
        image = (ImageView) findViewById(R.id.iv_image);
    }

    @Override
    protected void onResume() {
        super.onResume();
        capture.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_capture:
                openCamera();
                break;
        }
    }

    private void openCamera() {
        // check permission
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // request permission
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
        } else if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // request permission
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
        } else {
            try {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, CAMERA_CAPTURE);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Device doesn't support image capture", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case CAMERA_CAPTURE:
                performCrop(saveImageLocally(intent));
                break;
            case IMAGE_CROP:
                Bundle extras = intent.getExtras();
                Bitmap bitmap = extras.getParcelable("data");
                image.setImageBitmap(bitmap);
                Toast.makeText(this, "Image cropped successfully", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void performCrop(Uri imageUri) {
        if (imageUri != null) {
            try {
                Intent cropIntent = new Intent("com.android.camera.action.CROP");
                cropIntent.setDataAndType(imageUri, "image/*");
                cropIntent.putExtra("crop", "true");
                cropIntent.putExtra("aspectX", 1);
                cropIntent.putExtra("aspectY", 1);
                cropIntent.putExtra("outputX", 256);
                cropIntent.putExtra("outputY", 256);
                cropIntent.putExtra("return-data", true);
                startActivityForResult(cropIntent, IMAGE_CROP);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Device doesn't support image crop", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Unable to capture image", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Permission required", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case PERMISSION_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Permission required", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    private Uri saveImageLocally(Intent intent) {
        Bitmap thumbnail = null;
        try {
            thumbnail = (Bitmap) intent.getExtras().get("data");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);

        createDirectoryInStorage();

        // write file to directory
        File destination = null;
        if (isExternalStorageAvailable) {
            destination = new File(directoryPath, System.currentTimeMillis() + ".jpg");
            if (destination != null) {
                FileOutputStream fo;
                try {
                    destination.createNewFile();
                    fo = new FileOutputStream(destination);
                    fo.write(bytes.toByteArray());
                    fo.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return Uri.fromFile(destination);
    }

    private void createDirectoryInStorage() {
        boolean sdCardAvailability = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        if (sdCardAvailability) {
            String directoryName = getPackageName();
            directoryPath = Environment.getExternalStorageDirectory().getPath().toString() + "/Android/data/" + directoryName + "/Images";

            // storage
            File f = new File(directoryPath);
            if (!f.exists()) {
                if (!f.mkdirs()) {
                    isExternalStorageAvailable = false;
                }
            }
            isExternalStorageAvailable = true;
        } else {
            isExternalStorageAvailable = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        capture.setOnClickListener(null);
    }
}