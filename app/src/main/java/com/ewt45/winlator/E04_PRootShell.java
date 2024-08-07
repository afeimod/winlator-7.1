package com.ewt45.winlator;

import static android.view.View.FOCUS_DOWN;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.ewt45.winlator.QH.dp8;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.widget.NestedScrollView;

import com.ewt45.winlator.widget.SimpleTextWatcher;
import com.winlator.R;
import com.winlator.XServerDisplayActivity;
import com.winlator.core.Callback;
import com.winlator.core.ProcessHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * ProcessHelper.exec最长的那个，里面有输出日志到文件的判断标准。两个flag都开启应该就行了。可以参考一下
 */
public class E04_PRootShell {
    private static final String TAG = "PRootTerminal";
    public static final int MAX_LINE = 6000; //允许缓存和显示的最大行数
    public static final int REDUCED_FRAGMENT = 3000; //达到最大缓存时，删掉的行数
    public static final int MIN_RECEIVE_INTERVAL = 17; //刷新频率，最快17毫秒刷新一次。
    static Handler handler = new Handler(Looper.getMainLooper());
    private static final String PREF_KEY_PROOT_TERMINAL_ENABLE = "proot_terminal_enable";
    private static final String PREF_KEY_PROOT_TERMINAL_TEXT_SIZE = "proot_terminal_text_size";
    private static final String PREF_KEY_PROOT_TERMINAL_AUTO_SCROLL = "proot_terminal_auto_scroll";
    //显示输出流的回调。进程结束时应置为null
    private static DisplayCallback displayCallback;
    //显示的dialog。在第一次点左侧菜单时初始化（开启终端选项时），在process结束时置为null。
    // 由于不开启终端选项时无法监听进程结束 不能置null，所以不开启选项时不能用这个。
    private static AlertDialog dialog;
    private static TextView tv;
    private static Process runningProcess;
    private static int pid = -1;
    private static boolean isAutoScroll = true; //是否自动滚动到底部

    /**
     * 主界面 - 设置中 添加选项。
     */
    public static void addItemToSettings(AppCompatActivity a, LinearLayout hostRoot) {
        CheckBox checkPRoot = new CheckBox(a);
        checkPRoot.setText(QH.string.proot终端);
        checkPRoot.setChecked(isChecked(a));
        checkPRoot.setOnCheckedChangeListener((v, isChecked) -> QH.getPreference(v.getContext())
                .edit().putBoolean(PREF_KEY_PROOT_TERMINAL_ENABLE, isChecked).apply());

        LinearLayout linearPRoot = QH.wrapHelpBtnWithLinear(a, checkPRoot, QH.string.proot终端说明);
        hostRoot.addView(linearPRoot, QH.lpLinear(-1, -2).top().to());
    }

    public static boolean isChecked(Context context) {
        return QH.getPreference(context).getBoolean(PREF_KEY_PROOT_TERMINAL_ENABLE, false);
    }

    /**
     * 在GuestProgramLauncherComponent.execGuestProgram()中，启动proot时，ProcessHelper.exec替换为此函数
     * <br/> 如果未开启终端，则和原来一样处理(调用ProcessHelper.exec)。否则自己创建process并重定向输入输出流
     * <br/> envp只包含PROOT_TMP_DIR，PROOT_LOADER和PROOT_LOADER_32 三个环境变量。其他环境变量写在command中
     * <br/> 将command分为三部分：
     * 1. proot.so及其设置参数：从开头到 /usr/bin/env 之前
     * 2. linux命令前半部分，设置环境变量：从/usr/bin/env到box64之前
     * 3. linux命令后半部分, 启动wine：从box64到最后
     * <br/> 创建process时，需要将命令按空格把每个参数分隔开，形成数组。由于终端需要能够输入命令，所以需要稍微改一下启动命令。
     * 注意，后续命令通过process.getOutputStream().write()和.flush()传入，注意要在末尾加“\n"否则不会被执行。
     * 第一部分+第二部分+ " /usr/bin/dash", 在给定环境变量下启动shell，保证后续可以继续输入命令。
     * 第三部分 + " &\n" &保证shell可以处理后续输入，注意 & 放在换行前
     */
    public static int exec(Context c, String command, String[] envp, File workingDir, Callback<Integer> terminationCallback) {
        int box64CmdIdx = command.indexOf(" box64 ");

        //没开启proot终端选项，或context非xserver activity，或命令中找不到box64，或安装wine时，使用原始代码启动进程。
        if(!isChecked(c) || !(c instanceof XServerDisplayActivity)
                || box64CmdIdx == -1 || command.endsWith("box64 wine --version") || command.endsWith("box64 wine64 --version")) {
            Log.d(TAG, "exec: 启动proot。运行的命令为："+ command);
            return ProcessHelper.exec(command, envp, workingDir, terminationCallback);
        }

        pid = -1;
        try {
            //初始命令，启动proot，设置环境变量，并启动shell（dash）
            String initialCmd = command.substring(0, box64CmdIdx);
            List<String> cmdList = new ArrayList<>(Arrays.asList(ProcessHelper.splitCommand(initialCmd)));
            cmdList.add("/usr/bin/dash");
            Log.d(TAG, "exec: 启动proot。运行的命令为："+ cmdList);

            ProcessBuilder builder = new ProcessBuilder()
                    .command(cmdList.toArray(new String[0])) //命令行
                    .directory(workingDir) //工作目录
                    .redirectErrorStream(true); //合并错误和输出流
            Map<String, String> envMap = builder.environment();
            //XServerDisplayActivity.setupXEnvironment中，会将WINEDEBUG设置为-all。需要手动在容器设置里添加环境变量 WINEDEBUG=err+all,fixme+all
            for(String oneEnv: envp) {
                int idxSplit = oneEnv.indexOf('=');
                envMap.put(oneEnv.substring(0, idxSplit), oneEnv.substring(idxSplit+1));
            }

            runningProcess = builder.start();
            //反射时，类要用实例.getClass()获取，而不能直接传入抽象类Process
            pid = UtilsReflect.getPid(runningProcess);
            Log.d(TAG, "exec: 获取到的pid="+pid);

            //创建dialog。因为当前并非ui线程，所以要等待一下
            Thread currThread = Thread.currentThread();
            handler.post(() -> {
                createDialog((XServerDisplayActivity) c);
                currThread.interrupt();
            });
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Log.d(TAG, "exec: dialog已在主线程创建完毕。接着运行exec函数中的部分。");
            }

            //将终端输出 加入callback数组，并调用 ProcessHelper.createDebugThread 处理输出流
            ProcessHelper.addDebugCallback(displayCallback = new DisplayCallback());
            Method method = UtilsReflect.getMethod(ProcessHelper.class, "createDebugThread", InputStream.class);
            UtilsReflect.invokeMethod(method, null, runningProcess.getInputStream());

            //启动dash成功后，再输入命令启动box64。
            //注意快捷方式启动时，使用winhandler.exe启动exe，其路径中的空格会替换成\+空格，所以还得先用winlator的函数正确分割参数，然后每个参数都加上引号
            StringBuilder box64CmdFinal = new StringBuilder();
            for(String str:ProcessHelper.splitCommand(command.substring(box64CmdIdx)))
                box64CmdFinal.append(str.startsWith("\"") ? "" : "\"").append(str).append(str.endsWith("\"") ? "" : "\"").append(" ");
//            box64CmdFinal = new StringBuilder("xfce4-session");
            sendInputToProcess(box64CmdFinal + " &\n");
//            sendInputToProcess(command.substring(box64CmdIdx) + " &\n");

            //ProcessHelper.createWaitForThread。在进程结束时调用callback，并清空成员变量的值
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    int status = runningProcess.waitFor();
                    Log.d(TAG, "exec: proot进程停止运行。");
                    runningProcess = null;
                    pid = -1;
                    dialog = null;
                    tv = null;
                    displayCallback = null; //停止时将变量置为null
                    if(terminationCallback!=null) terminationCallback.call(status);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            QH.showFatalErrorDialog(c, QH.string.proot终端_启动失败_请关闭选项重试);
        }
        return pid;
    }

    /**
     * 向进程中输入命令。若命令末尾不带换行则会补上。
     */
    private static void sendInputToProcess(String command) {
        if(runningProcess == null || !runningProcess.isAlive())
            return;
        String finalCmd = command.endsWith("\n") ? command : (command + '\n');
        try {
            Log.d(TAG, "inputToProcess: 向进程中输入命令："+finalCmd);
            runningProcess.getOutputStream().write(finalCmd.getBytes());
            runningProcess.getOutputStream().flush();
            if(displayCallback != null)
                displayCallback.call(finalCmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 在exec函数 创建进程时，顺带创建dialog和callback
     * <br/> 将dialog和tv存到成员变量上。
     */
    @UiThread
    public static void createDialog(AppCompatActivity a) {
        Point point = new Point();
        a.getWindowManager().getDefaultDisplay().getSize(point);
        int viewWidth = point.x/2 < QH.px(a,400) ? (int) (point.x * 0.9f) : point.x/2;

        ViewGroup root = (ViewGroup) a.getLayoutInflater().inflate(R.layout.zzz_proot_teriminal_dialog, null, false);
        root.setLayoutParams(new ViewGroup.LayoutParams(viewWidth, -1));

        //输入命令
        EditText editText = root.findViewById(R.id.edittext_input);
        editText.addTextChangedListener((SimpleTextWatcher) s -> {
            //如果为空不做任何处理
            if(s.length() == 0) return;
            //如果最后一个输入的字符为回车，则发送命令并清空输入
            if(s.charAt(s.length()-1) == '\n') {
                sendInputToProcess(s.toString());
                editText.setText("");
            }
        });

        //文字输出
        tv = root.findViewById(R.id.terminal_text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, QH.getPreference(a).getFloat(PREF_KEY_PROOT_TERMINAL_TEXT_SIZE, dp8*7/4f));
        //设置BufferType为editable，后续用getEditableText获取和编辑
        tv.setText("", TextView.BufferType.EDITABLE);

        //菜单显隐
        View groupMenu = root.findViewById(R.id.group_menu_items);
        groupMenu.setVisibility(GONE);
        root.findViewById(R.id.btn_menu).setOnClickListener(v2 -> groupMenu.setVisibility(
                groupMenu.getVisibility() == VISIBLE ? GONE : VISIBLE));

        //ctrl+c
//                    android.os.Process.sendSignal(pid, SIGKILL);
//                    sendInputToProcess("\003");
//                    Os.kill(pid, SIGINT);

        //自动滚动到底部
        isAutoScroll = QH.getPreference(a).getBoolean(PREF_KEY_PROOT_TERMINAL_AUTO_SCROLL, true);
        AppCompatImageButton btnAutoScroll = root.findViewById(R.id.btn_auto_scroll);
        btnAutoScroll.setOnClickListener(v -> {
            isAutoScroll = !isAutoScroll;
            btnAutoScroll.setColorFilter(isAutoScroll ? Color.BLACK : Color.GRAY);
            QH.getPreference(v.getContext()).edit().putBoolean(PREF_KEY_PROOT_TERMINAL_AUTO_SCROLL, isAutoScroll).apply();
            if(isAutoScroll)
                displayCallback.scrollToEnd();
        });
        btnAutoScroll.setTooltipText(QH.string.proot终端_自动滚动到底部);
        btnAutoScroll.setColorFilter(isAutoScroll ? Color.BLACK : Color.GRAY);

        //帮助
        root.findViewById(R.id.btn_help).setOnClickListener(v -> QH
                .showConfirmDialog(v.getContext(), QH.string.proot终端说明, null));

        //文字放大缩小，一次变2dp
        root.findViewById(R.id.btn_text_size_up).setOnClickListener(v -> changeTextSize(tv, true));
        root.findViewById(R.id.btn_text_size_down).setOnClickListener(v -> changeTextSize(tv, false));

        assert dialog == null;
        dialog = new AlertDialog.Builder(a)
                .setView(root)
                .setCancelable(false)
                .setOnDismissListener(dialog1 -> {})
                .create();

        //关闭
        root.findViewById(R.id.btn_close).setOnClickListener(v -> {
            groupMenu.setVisibility(GONE);
            //遇到了dismiss无效的问题 https://blog.csdn.net/qiujuer/article/details/121238845 。
            // 原因是在非ui线程创建了dialog，导致dialog.mHandler.mLooper != 主线程Looper
            dialog.dismiss();
        });
    }

    /**
     * 容器启动后，左侧栏点击选项后显示视图
     */
    public static boolean showTerminalDialog (AppCompatActivity a) {
        if(dialog == null) {
            QH.showConfirmDialog(a, QH.string.proot终端_请先开启选项, null);
            return true;
        }

        dialog.show();
        // 在show之前修改window宽高不生效。在show之后修改，外部轮廓生效，但内部视图还不行
        // 原因：即使传入的自定义视图高度设成match，但其父视图默认是wrap。只好给自定义视图里加一个撑满高度的空白view了
        WindowManager.LayoutParams attr = Objects.requireNonNull(dialog.getWindow()).getAttributes();
        attr.width = getDialogWidth(a);
        attr.height = -1;
        attr.gravity = Gravity.START;
        dialog.getWindow().setAttributes(attr);

        if(isAutoScroll)
            displayCallback.scrollToEnd();

        return true;
    }

    private static int getDialogWidth(AppCompatActivity a) {
        Point point = new Point();
        a.getWindowManager().getDefaultDisplay().getSize(point);
        return point.x/2 < QH.px(a,400) ? (int) (point.x * 0.9f) : point.x/2;
    }

    private static void changeTextSize(TextView tv, boolean up) {
        float currSize = tv.getTextSize();
        float newSize = currSize + dp8/4f * (up ? 1 : -1);
        float limitSize = Math.max(Math.min(newSize, dp8*5), dp8);
        //不指定单位设置的是dp？？离谱
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, limitSize);
        QH.getPreference(tv.getContext()).edit().putFloat(PREF_KEY_PROOT_TERMINAL_TEXT_SIZE, limitSize).apply();
    }

    private static void runTestThread(DisplayCallback callback, TextView tvOutput){
        new Thread(() -> {
            while (true) {
                long time = (long) (Math.random() * 200);
                try {
                    Thread.sleep(time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                tvOutput.post(() -> callback.call(""+time));
            }
        }).start();
    }

    /**
     * callback，加入到ProcessHelper的回调数组中。
     * <br/>新建实例时，应确保dialog和tv成员变量存在
     * //TODO 要不要考虑换成recyclerView？
     */
    private static class DisplayCallback implements Callback<String> {
        //TODO 好像跨线程写入的话不安全。要不试试 CopyOnWriteArrayList
        private List<String> allLines = new ArrayList<>(); //存储历史文本行，只会在ui线程被访问。
        private final StringBuilder caches = new StringBuilder(); //输出频率过快时的缓存，尚未加入allLines，可能在多线程被访问所以要加锁。
        private long lastReceiveTime = System.currentTimeMillis();
        private Thread delayRefreshThread = new Thread();
        @Override
        public void call(String line) {
            Log.d(TAG, "进程输出: "+line);
            synchronized (caches) {
                long currTime = System.currentTimeMillis();

                //如果输出频率过快，则限制一下。
                if (currTime - lastReceiveTime < MIN_RECEIVE_INTERVAL) {
                    //将本行输出存入cache而非allLines。并跳过本次tv更新。
                    boolean isInCaching = caches.length() > 0;
                    caches.append(isInCaching ? "\n" : "").append(line);

                    //新建线程，倒计时min_interval之后，再次检查。如果已经有线程正在倒计时，则取消，重新倒计时。
                    if(isInCaching)
                        delayRefreshThread.interrupt();
                    delayRefreshThread = new DelayRefreshThread(() -> {
                        //倒计时结束时，如果cache仍未被处理，则把cache内容作为line，调用call函数，同时清空cache
                        synchronized (caches) {
                            if(caches.length() == 0)
                                return;
                            String cacheLines = caches.toString();
                            caches.delete(0, caches.length());
                            call(cacheLines); //synchronized是可重入锁，可以对同一个对象多次加锁
                        }

                    });
                    delayRefreshThread.start();
                }
                //如果输出频率合适，则扔到ui线程处理。
                //可能有cache尚未处理（说明之前频率过快，但是开启的延迟检查线程尚未倒计时结束），
                // 此时应合并cache和当前行，然后清空cache，取消延迟检查
                else {
                    lastReceiveTime = currTime;
                    boolean hasCached = caches.length() == 0;

                    String tmpLine = hasCached ? caches.append('\n').append(line).toString() : line;;
                    if (tmpLine.endsWith("\n")) tmpLine = tmpLine.substring(0, tmpLine.length()-1);
                    if (tmpLine.startsWith("\n")) tmpLine = tmpLine.substring(1);

                    caches.delete(0, caches.length());
                    if(delayRefreshThread.isAlive())
                        delayRefreshThread.interrupt();

                    final String finalLine = tmpLine;
                    handler.post(() -> {
                        //对allLines的访问，应仅在在ui线程。
                        if (allLines.size() > MAX_LINE) {
                            allLines.subList(0, REDUCED_FRAGMENT).clear(); //这样可以移除指定范围的元素
                            setAllLinesToTv();
                        }
                        allLines.add(finalLine);

                        //TODO 参考一下termux怎么实现terminalView，缓存行，和文本显示（每行一个textview？）
                        // 还有ctrl+c 实现？多进程？（留一个rootProcess，用户可以操作的都是从此root分支出来的，保证用户关闭了进程还可以再新建）

                        tv.getEditableText().append(finalLine).append('\n'); //这个要在ui线程修改

                        if(isAutoScroll && dialog.isShowing())
                            scrollToEnd();
                    });
                }
            }
        }


        /**
         * 用于将一行新的文本显示在textview上。若短时间调用多次，可能会延迟几百毫秒后显示。因为涉及UI操作，必须在主线程中调用。
         * @param finalLine 新增加的一行，末尾不能带换行。
         */
        @UiThread
        private void callOnUIThread(String finalLine) {
            if (finalLine.endsWith("\n")) finalLine = finalLine.substring(0, finalLine.length()-1);
            if (finalLine.startsWith("\n")) finalLine = finalLine.substring(1);

            if (allLines.size() > MAX_LINE) {
                allLines.subList(0, REDUCED_FRAGMENT).clear(); //这样可以移除指定范围的元素
                setAllLinesToTv();
            }
            allLines.add(finalLine);

            //TODO 参考一下termux怎么实现terminalView，缓存行，和文本显示（每行一个textview？）
            // 还有ctrl+c 实现？多进程？（留一个rootProcess，用户可以操作的都是从此root分支出来的，保证用户关闭了进程还可以再新建）


            tv.getEditableText().append(finalLine).append('\n'); //这个要在ui线程修改

            if(!dialog.isShowing())
                return;

            if(isAutoScroll)
                scrollToEnd();
        }

        public void scrollToEnd() {
            //原来是fullScroll调用后会设置textview为焦点导致的，即使不再手动调用也会自动滚动。同时也会导致edittext输入到一半被清空焦点。。
            tv.post(() -> {
                ((NestedScrollView) tv.getParent().getParent()).fullScroll(FOCUS_DOWN);
                tv.clearFocus();
            });
        }

        /**
         * 清空tv，并将列表中全部行都添加到tv上. 用途
         * 1（不用了）. 尚未初始化dialog时，已经记录了一些输出。dialog初始化之后，应该将这些缓存的行添加到tv上。
         * 2. 行数超过最大限制时，删去前半段输出，此时应该清空tv并将剩下的输出显示到tv上。
         */
        @UiThread
        public void setAllLinesToTv() {
            Log.d(TAG, "setAllLinesToTv: 清空editableText，然后添加");
            tv.getEditableText().clear();
            StringBuilder sb = new StringBuilder();
            for (String line : allLines)
                sb.append(line).append('\n');
            tv.getEditableText().append(sb.toString());
        }
    }

    private static class DelayRefreshThread extends Thread {
        Runnable r;
        public DelayRefreshThread (Runnable r) {
            this.r = r;
        }
        @Override
        public void run() {
            try {
                Thread.sleep(MIN_RECEIVE_INTERVAL);
                r.run();
            } catch (InterruptedException e) {
                //如果被打断，说明取消该延迟检查了。
            } finally {
                // 资源回收
                r = null;
            }
        }
    }

}