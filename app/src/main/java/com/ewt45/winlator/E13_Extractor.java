package com.ewt45.winlator;

import static android.content.Intent.EXTRA_ALLOW_MULTIPLE;

import static com.ewt45.winlator.UtilsReflect.getMethod;
import static com.ewt45.winlator.UtilsReflect.invokeMethod;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;

import com.winlator.R;
import com.winlator.SettingsFragment;
import com.winlator.core.AppUtils;
import com.winlator.core.DownloadProgressDialog;
import com.winlator.core.FileUtils;
import com.winlator.core.TarCompressorUtils;
import com.winlator.xenvironment.ImageFs;
import com.winlator.xenvironment.ImageFsInstaller;

import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用于解压deb吧
 */
class E13_Extractor extends DialogFragment {
    private static final String TAG = "E13_Extractor";
    private static final int REQUEST_CODE_BASE = 20;
    private static final int PICK_DEB_FILE = REQUEST_CODE_BASE + 1;
    private static final int PICK_ROOTFS = REQUEST_CODE_BASE + 2;
    private TextView tvInfo;
    private int rootfsInstallStyle = 0; //0:覆盖 1:移走
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Activity a = requireActivity();
        Button btnDeb = new Button(a);
        btnDeb.setText("选择deb");
        btnDeb.setEnabled(false);
        btnDeb.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, PICK_DEB_FILE);
        });

        tvInfo = new TextView(a);


        RadioGroup groupInstallStyle = new RadioGroup(a);
        groupInstallStyle.setOrientation(LinearLayout.VERTICAL);

        RadioButton radioOverride = new RadioButton(a);
        radioOverride.setId(View.generateViewId());
        radioOverride.setText("保留原rootfs，相同文件直接覆盖");

        RadioButton radioBackup = new RadioButton(a);
        radioBackup.setId(View.generateViewId());
        radioBackup.setText("移除原rootfs至别处，仅使用新rootfs的内容");

        groupInstallStyle.addView(radioOverride);
        groupInstallStyle.addView(radioBackup);
        groupInstallStyle.setOnCheckedChangeListener((group, checkedId) ->
                rootfsInstallStyle = checkedId == radioOverride.getId() ? 0 : 1);
        groupInstallStyle.check(radioOverride.getId());

        Button btnRootfs = new Button(a);
        btnRootfs.setText("选择新的 rootfs");
        btnRootfs.setOnClickListener(v -> {
            groupInstallStyle.setEnabled(false);
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
//            intent.putExtra(EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, PICK_ROOTFS);
        });

        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
//        root.addView(btnDeb);
        root.addView(btnRootfs);
        root.addView(groupInstallStyle);
        root.addView(tvInfo);
        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK)
            return;
        if (requestCode == PICK_DEB_FILE) {
            List<Uri> uris = getUrisFromResult(data);
            if(uris == null)
                return;
            tvInfo.setText("正在解压...");
            startExtractDeb(uris);
        } else if (requestCode == PICK_ROOTFS && data != null) {
            Uri uri = data.getData();
            if(uri != null)
                startExtractRootfs(uri);
        }

    }

    private void startExtractRootfs(Uri uri) {
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        ImageFs imageFs = ImageFs.find(activity);

        if(rootfsInstallStyle == 1) {
            File rootfsDir = imageFs.getRootDir();
            File rootfsBakDir = new File(rootfsDir.getParent(), rootfsDir.getName() + System.currentTimeMillis());
            if (rootfsBakDir.exists()) {
                AppUtils.showToast(requireActivity(), "目标目录已存在，无法备份" + rootfsBakDir.getAbsolutePath());
                return;
            }
            if (!rootfsDir.renameTo(rootfsBakDir)) {
                AppUtils.showToast(requireActivity(), "目录重命名失败，无法备份" + rootfsBakDir.getAbsolutePath());
                return;
            } else
                AppUtils.showToast(requireActivity(), "imagefs文件夹已重命名为 "+rootfsBakDir.getName());
        }

        //参考ImageFsInstaller.installFromAssets
        AppUtils.keepScreenOn(activity);
        final File rootDir = imageFs.getRootDir();
        SettingsFragment.resetBox86_64Version(activity);

        final DownloadProgressDialog dialog = new DownloadProgressDialog(activity);
        dialog.show(R.string.installing_system_files);
        Executors.newSingleThreadExecutor().execute(() -> {
            final long contentLength = getSizeFromUri(uri);
            AtomicLong totalSizeRef = new AtomicLong();
            boolean success = TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, activity, uri, rootDir, (file, size) -> {
                if (size > 0) {
                    long totalSize = totalSizeRef.addAndGet(size);
                    final int progress = (int)(((float)totalSize / contentLength) * 100);
                    activity.runOnUiThread(() -> dialog.setProgress(progress));
                }
                return file;
            });

            if (success) {
                imageFs.createImgVersionFile(ImageFsInstaller.LATEST_VERSION);
                invokeMethod(getMethod(ImageFsInstaller.class, "resetContainerImgVersions", Context.class)
                        , null, activity);
            }
            else AppUtils.showToast(activity, "文件解压失败！");

            dialog.closeOnUiThread();
        });
    }

    private long getSizeFromUri(Uri uri) {
        long compressedSize = 0;
        try (InputStream is = requireActivity().getContentResolver().openInputStream(uri)) {
            assert is != null;
            compressedSize  = is.available();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return compressedSize;
    }

    private void startExtractDeb(List<Uri> uris) {
        new Thread(() -> {
            File tmpFile = new File(requireActivity().getCacheDir(), "tmp-deb-data");

            for (Uri uri : uris) {
                appendTextToTextView(parseFileNameFromUri(uri));

                try (InputStream is = requireContext().getContentResolver().openInputStream(uri);
                     ArArchiveInputStream arais  = new ArArchiveInputStream(is)) {
                    ArArchiveEntry arEntry = null;
                    while ((arEntry = arais.getNextArEntry()) != null) {
                        if(arEntry.getName().startsWith("data"))
                            break;
                    }
                    assert arEntry != null : "deb中未找到data";
                    if(arEntry.getName().endsWith(".tar.xz")) {
                        try (OutputStream os = new FileOutputStream(tmpFile)) {
                            IOUtils.copy(arais, os);
                        }
                        TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, tmpFile, ImageFs.find(requireActivity()).getRootDir());
                        appendTextToTextView("解压完成");
                    } else {
                        appendTextToTextView("不支持的压缩类型："+arEntry.getName());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    appendTextToTextView("\n报错：" + e.getCause());
                }
            }
            appendTextToTextView("\n全部解压完成");

        }).start();
    }

    private void appendTextToTextView(String text) {
        tvInfo.post(() -> tvInfo.setText(tvInfo.getText() + "\n" + text));
    }

    private static @Nullable List<Uri> getUrisFromResult(Intent intent) {
        if (intent == null)
            return null;
        if (intent.getData() != null)
            return List.of(intent.getData());
        if (intent.getClipData() != null) {
            List<Uri> list = new ArrayList<>();
            ClipData clipData = intent.getClipData();
            for(int i = 0; i < clipData.getItemCount(); i++)
                list.add(clipData.getItemAt(i).getUri());
            return list;
        }
        return null;
    }

    private String parseFileNameFromUri(Uri uri) {
        //获取文件名
        String filename = null;
        //如果是从“最近”分类下打开的话，uri里不包含文件名，需要通过这个获取
        DocumentFile dcFile = DocumentFile.fromSingleUri(requireActivity(),uri);
        if(dcFile != null)
            filename = dcFile.getName();

        //保留原来的从uri提取文件名的方法吧
        if(filename == null){
            List<String> list = uri.getPathSegments();
            String[] names = list.get(list.size() - 1).split("/");
            filename = names[names.length - 1]; //文件
        }
        return filename;
    }

    public static boolean showDialog(AppCompatActivity a) {
        new E13_Extractor().show(a.getSupportFragmentManager(), TAG);
        return true;
    }

    public static void addItemToSettings(AppCompatActivity a, LinearLayout hostRoot) {
        Button btn = new Button(a);
        btn.setText("选择文件以解压...");
        btn.setOnClickListener(v -> showDialog(Objects.requireNonNull(QH.getActivity(v))));
        hostRoot.addView(btn, QH.lpLinear(-1, -2).top().to());
    }

}
