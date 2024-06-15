package com.anmol.easypdfreader;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    Adapter adapter;
    List<File> list;
    ProgressBar progressBar;
    int scrollPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initvar();
        checkPermission();

        if(savedInstanceState != null) {
            scrollPosition = savedInstanceState.getInt("Scroll_position", 0);
            recyclerView.scrollToPosition(scrollPosition);
        }

    }
    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            setuprv();
        }

        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager != null) {
            layoutManager.scrollToPosition(scrollPosition);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager != null) {
            scrollPosition = layoutManager.findFirstVisibleItemPosition();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("Scroll_position", scrollPosition);
    }

    private void checkPermission() {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            if(Environment.isExternalStorageManager()){
                setuprv();
            }
            else{
                showStoragePermissionDialog();
            }
        }
        else{
            Dexter.withContext(this).withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .withListener(new MultiplePermissionsListener() {
                        @Override
                        public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                            setuprv();
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                            permissionToken.continuePermissionRequest();
                        }
                    })
                    .check();
        }
    }

    private void showStoragePermissionDialog() {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this);
        builder.setTitle("Storage Permission");
        builder.setMessage("This app needs storage permission to function properly.");
        builder.setPositiveButton("Allow", new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.R)
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                    startActivityIfNeeded(intent, 101);
                }
                catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivityIfNeeded(intent, 101);
                }
            }
        });
        builder.setNegativeButton("Deny", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(MainActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void setuprv() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        setupSearchView();

        new Thread(() -> {
            List<File> files = getallFiles();
            Collections.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return Long.valueOf(o2.lastModified()).compareTo(o1.lastModified());
                }
            });

            list.clear();
            list.addAll(files);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (adapter == null) {
                        adapter = new Adapter(MainActivity.this, list);
                        recyclerView.setAdapter(adapter);
                    }
                    else {
                        adapter.notifyDataSetChanged();
                    }
                    handleProgressBar();

                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        layoutManager.scrollToPosition(scrollPosition);
                    }
                }
            });
        }).start();
    }

    private void setupSearchView() {
        SearchView searchView = findViewById(R.id.searchView);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText!=null) {
                    filter(newText);
                }
                else{
                    Toast.makeText(MainActivity.this, "No File Found", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });
    }

    private void filter(String newText) {
        List<File> filterlist = new ArrayList<>();
        for (File item : list){
            if (item.getName().toLowerCase().contains(newText)){
                filterlist.add(item);
            }
        }
        adapter.filterList(filterlist);
    }

    private List<File> getallFiles() {
        Uri uri = MediaStore.Files.getContentUri("external");

        String[] projection = {MediaStore.Files.FileColumns.DATA};
        String selection = MediaStore.Files.FileColumns.MIME_TYPE + "=?";
        String[] selectionArgs = {"application/pdf"};

        Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, null);

        int pdfPathIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);

        ArrayList<File> list = new ArrayList<>();

        while (cursor.moveToNext()) {
            if (pdfPathIndex != -1) {
                String pdfPath = cursor.getString(pdfPathIndex);
                File pdfFile = new File(pdfPath);
                if (pdfFile.exists() && pdfFile.isFile()) {
                    list.add(pdfFile);
                }
            }
        }
        cursor.close();
        return list;
    }

    private void handleProgressBar() {
        progressBar.setVisibility(View.GONE);
        if (adapter.getItemCount() == 0) {
            Toast.makeText(this, "No Pdf File in Phone", Toast.LENGTH_SHORT).show();
        } else {
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void initvar() {
        recyclerView = findViewById(R.id.rv_files);
        progressBar = findViewById(R.id.progressBar);
        list = new ArrayList<>();
    }
}