package com.kagg886.seiko.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.kagg886.seiko.R;
import com.kagg886.seiko.SeikoApplication;
import com.kagg886.seiko.adapter.LogAdapter;
import com.kagg886.seiko.bot.BotLogConfiguration;
import com.kagg886.seiko.util.IOUtil;
import net.mamoe.mirai.Bot;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LogActivity extends AppCompatActivity implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {
    private ListView log;
    private LogAdapter adapter;
    private FloatingActionButton btn;

    private ActivityResultLauncher<Intent> writeCall;

    private String nowBot;

    private SwipeRefreshLayout noLay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_plugin);
        nowBot = getIntent().getStringExtra("uin");
        noLay = findViewById(R.id.fragment_plugin_refresh);
        noLay.setOnRefreshListener(this);
        File file;
        Bot bot = Bot.findInstance(Long.parseLong(nowBot));
        if (bot == null) { //bot未登录时拉取今日的日志
            //String parentPath = String.format("%s/%s/", getExternalFilesDir("bots").getAbsolutePath(), nowBot);
            Path parentPath = Paths.get(getExternalFilesDir("bots").getAbsolutePath(), nowBot);
            File offlineLogFile;

            if (SeikoApplication.globalConfig.getBoolean("mergeAllLogs", false)) {
                offlineLogFile = getExternalFilesDir("logs").toPath().resolve(new SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis()) + ".log").toFile();
            } else {
                offlineLogFile = parentPath.resolve("log").resolve(new SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis()) + ".log").toFile();
            }

            if (offlineLogFile.exists()) {
                file = offlineLogFile;
            } else {
                Toast.makeText(this, R.string.log_empty, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        } else {
            file = ((BotLogConfiguration) bot.getConfiguration()).getLogFile();
        }


        log = findViewById(R.id.fragment_plugin_list);
        log.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        log.setStackFromBottom(true);

        adapter = new LogAdapter(this, file);
        log.setAdapter(adapter);

        btn = findViewById(R.id.fragment_plugin_menu);
        btn.setOnClickListener(this);

        registerActivityResultLauncher();
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, "log-" + nowBot + ".zip");
        writeCall.launch(intent);
    }

    public void snack(@StringRes int text) {
        Snackbar.make(log, text, BaseTransientBottomBar.LENGTH_LONG).show();
    }

    private void registerActivityResultLauncher() {
        writeCall = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getData() == null) {
                return;
            }
            try {
                File p = new File(getExternalFilesDir("bots") + "/" + nowBot + "/log");
                if (!p.isDirectory()) {
                    snack(R.string.log_export_empty);
                    return;
                }
                OutputStream stream = getContentResolver().openOutputStream(result.getData().getData());
                ZipOutputStream output = new ZipOutputStream(stream);
                for (File a : p.listFiles()) {
                    output.putNextEntry(new ZipEntry(a.getName()));
                    output.write(IOUtil.loadByteFromFile(a.getAbsolutePath()));
                }
                output.close();
                stream.close();
                snack(R.string.log_export_success);
            } catch (Exception e) {
                Log.w("DEBUG", e);
                snack(R.string.log_export_fail);
            }
        });
    }

    @Override
    public void onRefresh() {
        noLay.setRefreshing(false);
    }
}