package com.braodthinking.blesim.sdk;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.orhanobut.logger.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import cn.com.fmsh.base.BleBaseActivity;
import cn.com.fmsh.base.Const;
import cn.com.fmsh.bluetooth.sdk.DeviceScanInterfacer;
import cn.com.fmsh.bluetooth.sdk.ICallback;
import cn.com.fmsh.bluetooth.sdk.ICallbackStatus;
import cn.com.fmsh.bluetooth.sdk.ServiceStatusCallback;
import cn.com.fmsh.log.LogcatHelper;
import cn.com.fmsh.util.ApduUtil;

import static android.content.ContentValues.TAG;

public class MainActivity extends BleBaseActivity implements
        DeviceScanInterfacer, ServiceStatusCallback, ICallback, View.OnClickListener{
    private ListView lv;
    //日志
    private ArrayAdapter<String> mAdapterLog;
    private ArrayList<String> mListLog;
    private ListView mListViewLog;
    private BluetoothAdapter adapter;
    //搜索状态的标示
    private boolean mScanning;
    //扫描时长
    private static final long SCAN_PERIOD = 5000;  //5s 后自动关闭扫描
    //蓝牙适配器
    private BlueToothDeviceAdapter mBlueToothDeviceAdapter;
    //蓝牙适配器List
    private List<BluetoothDevice> mBlueList = new ArrayList<>();
    private Context context;
    private String  mStrProcessKey = "48ad10e64db3b448ebbd66fe546889bc";
    private String masterkey;

    public final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1: // Notify change
                    mBlueToothDeviceAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    private Button btn_start;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //日志输出
        LogcatHelper.getInstance(this).start();
        context = this;
        btn_start = findViewById(R.id.btn_start);
        btn_start.setOnClickListener(this);
        findViewById(R.id.btn_close).setOnClickListener(this);
        findViewById(R.id.btn_select).setOnClickListener(this);
        findViewById(R.id.btn_create_seed).setOnClickListener(this);
        findViewById(R.id.btn_getPublicKey).setOnClickListener(this);
        findViewById(R.id.btn_getSignature).setOnClickListener(this);
        findViewById(R.id.btn_backup).setOnClickListener(this);
        lv = findViewById(R.id.lv);

        //log输出
        mListLog = new ArrayList<>();
        mAdapterLog = new ArrayAdapter<>(this,
                R.layout.simple_list_item_1, mListLog);
        mListViewLog = findViewById(R.id.log);
        if (null != mListViewLog) mListViewLog.setAdapter(mAdapterLog);

        if (!bluetoothPermissions())
            return;

        //初始化蓝牙适配器
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager!=null) {
            adapter = bluetoothManager.getAdapter();
            if (adapter == null || !adapter.isEnabled()) {
                Toast.makeText(context, "请打开蓝牙连接", Toast.LENGTH_SHORT).show();
                return;
            }
            //初始化适配器
            mBlueToothDeviceAdapter = new BlueToothDeviceAdapter(mBlueList, context);
            mBLEServiceOperate.setServiceStatusCallback(this);
            mBLEServiceOperate.SetBroadcastReceiverCallBack( mDiscoveryResult );

            scanLeDevice(true);
        }else {
            Toast.makeText(context, "搜索不到设备的蓝牙适配器", Toast.LENGTH_SHORT).show();
        }

        lv.setOnItemClickListener((parent,view,position,id)-> {
            BluetoothDevice device = (BluetoothDevice) mBlueToothDeviceAdapter.getItem(position);
            mBLEServiceOperate.setMAC( device.getAddress() );
            int connect = mBLEServiceOperate.connect(device.getAddress(), mStrProcessKey, 0,"1");//初始保护密钥认证
            Log.d("connect","connect:"+connect);
            log("连接蓝牙卡状态:"+connect);
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogcatHelper.getInstance(this).stop();
    }


    private BroadcastReceiver mDiscoveryResult = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d( TAG, "onReceive" );
            BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            runOnUiThread(()-> {
                //获取到蓝牙设备
                if (!mBlueList.contains(remoteDevice)){
                    mBlueList.add(remoteDevice);
                }
                //List加载适配器
                if (mBlueToothDeviceAdapter.isEmpty()) {
                    Log.e("tag", "mLeDeviceListAdapter为空");
                } else {
                    lv.setAdapter(mBlueToothDeviceAdapter);
                }
                mHandler.sendEmptyMessage(1);
            });

        }
    };
    /**
     * 设备搜索
     * @param enable 是否正在搜索的标示
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(()-> {
                if (mScanning) {
                    mScanning = false;
                    mBLEServiceOperate.stopLeScan();
                }
                closeLoading();
            }, SCAN_PERIOD);

            mScanning = true;
            mHandler.sendEmptyMessage(1);
            mBLEServiceOperate.startLeScan();
            showLoading("正在扫描",true);
        }
    }

    // 定义获取基于地理位置的动态权限
    private boolean bluetoothPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return false;
        }else {
            return true;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_start:
                if (!bluetoothPermissions())
                    return;
                if (!this.mBLEServiceOperate.isSupportBLE()) {
                    showToast("该设备不支持蓝牙,应用不可用");
                    return;
                }

                final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                adapter = bluetoothManager.getAdapter();
                if (adapter == null || !adapter.isEnabled()){
                    Toast.makeText(context,"请打开蓝牙连接",Toast.LENGTH_SHORT).show();
                    return;
                }

                mBlueToothDeviceAdapter = new BlueToothDeviceAdapter(mBlueList,context);
                scanLeDevice(true);

                break;

            case R.id.btn_select:
                try {
                    String SelectApdu="00A404000e636f696e57616c6c657441707001";
                    log("发送指令: "+SelectApdu);
                    String command = mBLEServiceOperate.sendAPDUCommand(SelectApdu, 0);
                    Log.e("command:", "command:" + command);
                    log("返回命令：" + command);

                }catch (Exception e){
                    e.printStackTrace();
                    log("发生错误："+e.getMessage());
                }
                break;


            case R.id.btn_create_seed:
                try {
                    String apdu_create_seed=ApduUtil.CreatSeed("11223344556677881122334455667788");
                    log("发送指令："+apdu_create_seed);
                    String command = mBLEServiceOperate.sendAPDUCommand(apdu_create_seed, 0);
                    if (command.length()>=136){
                        masterkey = command.substring(4, 132);
                    }
                    Log.e("command:", "command:" + command);
                    log("返回命令：" + ApduUtil.ErrMessage(command));
                    log("masterkey: "+masterkey);
                }catch (Exception e){
                    e.printStackTrace();
                    log("发生错误："+e.getMessage());
                }
                break;

            case R.id.btn_getPublicKey:
                try {
                    String apdu_getPublicKey=ApduUtil.getPublicKey("3C", "00000002");
                    log("发送指令："+apdu_getPublicKey);
                    String command = mBLEServiceOperate.sendAPDUCommand(apdu_getPublicKey, 0);
                    Log.e("command:", "command:" + command);
                    log("返回命令:" + ApduUtil.ErrMessage(command));
                }catch (Exception e){
                    e.printStackTrace();
                    log("发生错误："+e.getMessage());
                }
                break;

            case R.id.btn_getSignature:
                try {
                    String apdu_btn_getSignature=ApduUtil.getSignature("3C",
                            "00000002",
                            "112233445566778811223344556677881122334455667788112233445566778811223344556677881122334455667788112233445566778811223344556677881122334455667788112233445566778811223344556677881122334455667788");
                    log("发送指令："+apdu_btn_getSignature);
                    String command = mBLEServiceOperate.sendAPDUCommand(apdu_btn_getSignature, 0);
                    Log.e("command:", "command:" + command);
                    log("返回命令：" + ApduUtil.ErrMessage(command));
                }catch (Exception e){
                    e.printStackTrace();
                    log("发生错误："+e.getMessage());
                }
                break;

            case R.id.btn_backup:
                try {
                    if(masterkey !=null){
                        if (masterkey.length()==128){
                            String apdu_backup=ApduUtil.recoveryKey("11223344556677881122334455667788", masterkey);
                            log("发送指令："+apdu_backup);
                            String command = mBLEServiceOperate.sendAPDUCommand(apdu_backup, 0);
                            Log.e("command:", "command:" + command);
                            log("返回命令：" + ApduUtil.ErrMessage(command));
                            log("成功标识：  "+ command.substring(4, 6)+ "     [00成功/01失败]");
                            return;
                        }
                    }
                    log("发生错误："+"masterkey值不正确");
                }catch (Exception e){
                    e.printStackTrace();
                    log("发生错误："+e.getMessage());
                }
                break;

            case R.id.btn_close:
                boolean flag = mBLEServiceOperate.disConnect();
                if (flag){
                    showToast("关闭成功");
                }else{
                    showToast("已关闭连接");
                }
                break;
        }
    }

    /**
     * 蓝牙扫描
     * @param device 蓝牙设备
     * @param i 数量
     */
    @Override
    public void LeScanCallback(BluetoothDevice device, int i) {
        runOnUiThread(()-> {
            //获取到蓝牙设备
            if (!mBlueList.contains(device)){
                mBlueList.add(device);
                Log.e("tag", "mLeScanCallback 搜索结果   " + device.getAddress());
            }
            //List加载适配器
            if (mBlueToothDeviceAdapter.isEmpty()) {
                Log.e("tag", "mLeDeviceListAdapter为空");
            } else {
                lv.setAdapter(mBlueToothDeviceAdapter);
            }
            mHandler.sendEmptyMessage(1);
        });
    }
    //结果
    @Override
    public void OnResult(boolean result, int action, String strKey) {
        Log.i(TAG, "OnResult-result:" + result + "|action:" + action);
        closeLoading();
        if (action == ICallbackStatus.CONNECTED_STATUS) {
            Log.i(TAG, "Status is CONNECTED_STATUS");
            Logger.d("Process Key is " + strKey);

            mStrProcessKey  =  strKey;                    //保存过程密钥
            saveProcessKeyConfig();

            if (Const.BLE_UI_MODE == Const.BLE_UI_SOCKET) {
                //startActivity(new Intent(this.getApplicationContext(), BlueToothSIM_BusinessActivity.class));
            } else if (Const.BLE_UI_MODE == Const.BLE_UI_UI) {
                //startActivity(new Intent(this.getApplicationContext(), BlueToothSIM_UIActivity.class));
            }
            toastInfo("连接成功", false);
            log("连接成功");
        } else if ( action == ICallbackStatus.CONNECTED_TIMEOUT ) {
            Logger.d( "连接超时", false );
            toastInfo( "连接超时", false );
        } else if ( action == ICallbackStatus.AUTHENTICATE_FAILURE ) {
            Logger.d( "安全认证失败 ！");
            toastInfo( "安全认证失败", false );
            log("安全认证失败");
        } else if (action == ICallbackStatus.DISCONNECT_STATUS) {
            Logger.d( "连接设备失败 请重试");
            toastInfo("连接设备失败 请重试", false);
            log("连接设备失败 请重试");
        } else if ( action == ICallbackStatus.APDUCOMMAND_TIMEOUT ) {
            Logger.d( "执行命令超时");
            toastInfo( "执行命令超时", false );
            log("执行命令超时");
        }

    }

    //服务状态
    @Override
    public void OnServiceStatus(int action) {
        Log.i(TAG, "OnServiceStatuslt-action:" + action);
        closeLoading();
        if (action == ICallbackStatus.BLE_SERVICE_START_OK) {
            // 蓝牙初始化
            mBLEServiceOperate.setDeviceScanListener(this);
            mBLEServiceOperate.registerCallback(this);
            //bleApduHandler = BleApduHandler.getInstance();
            //btnScanDevice.performClick();
            btn_start.performClick();
        } else {
            toastInfo("蓝牙服务开启失败,请重启应用", true);
            this.finish();
        }
    }

    public void  saveProcessKeyConfig( ) {
        String  str;
        str = mStrProcessKey;
        try {
            FileOutputStream fout = openFileOutput( "processKey.config", MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter( fout );
            osw.write(str);
            osw.flush( );
            osw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void log(String str) {
        if (null != mListViewLog) {
            if (mListLog.size() >= 100) mListLog.remove(0);
            mListLog.add(str);
            mAdapterLog.notifyDataSetChanged();
            mListViewLog.smoothScrollToPosition(mListLog.size() - 1);
        }
        Log.d("BLE", str);
    }

    public void showToast(String msg){
        runOnUiThread(()->{
            Toast.makeText(MainActivity.this,msg,Toast.LENGTH_SHORT).show();
        });
    }
}
