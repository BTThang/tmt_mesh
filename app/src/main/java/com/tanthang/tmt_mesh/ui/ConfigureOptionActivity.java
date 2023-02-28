package com.tanthang.tmt_mesh.ui;

import androidx.annotation.NonNull;

import android.annotation.SuppressLint;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AlertDialogLayout;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.MacAddress;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.data.ApnSetting;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.codec.binary.StringUtils;
import com.tanthang.tmt_mesh.R;
import com.tanthang.tmt_mesh.app.BaseActivity;
import com.tanthang.tmt_mesh.app.BlufiLog;
import com.tanthang.tmt_mesh.constants.BlufiConstants;
import com.tanthang.tmt_mesh.databinding.ActivityConfigureOptionBinding;
import com.tanthang.tmt_mesh.databinding.BlufiMessageItemBinding;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import blufi.espressif.BlufiCallback;
import blufi.espressif.BlufiClient;
import blufi.espressif.params.BlufiConfigureParams;
import blufi.espressif.params.BlufiParameter;
import blufi.espressif.response.BlufiScanResult;
import blufi.espressif.response.BlufiStatusResponse;
import blufi.espressif.response.BlufiVersionResponse;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.collections.ArrayDeque;

public class ConfigureOptionActivity extends BaseActivity {

    private static final int OP_MODE_POS_STA = 0;

    private static final int OP_MODE_POS_SOFTAP = 1;

    private static final int OP_MODE_POS_STASOFTAP = 2;

    BlufiActivity mBlufiActivity;

    private static int meshtype;

    private volatile boolean mConnected;

    private List<Message> mMsgList;
    private MsgAdapter mMsgAdapter;

    private BlufiClient mBlufiClient;


    private static final int[] OP_MODE_VALUES = {
            BlufiParameter.OP_MODE_STA,
            BlufiParameter.OP_MODE_SOFTAP,
            BlufiParameter.OP_MODE_STASOFTAP
    };

    private static final int[] SOFTAP_SECURITY_VALUES = {
            BlufiParameter.SOFTAP_SECURITY_OPEN,
            BlufiParameter.SOFTAP_SECURITY_WEP,
            BlufiParameter.SOFTAP_SECURITY_WPA,
            BlufiParameter.SOFTAP_SECURITY_WPA2,
            BlufiParameter.SOFTAP_SECURITY_WPA_WPA2
    };


    private static final String PREF_AP = "blufi_conf_aps";

    private BlufiLog mLog = new BlufiLog(getClass());

    private WifiManager mWifiManager;

    private List<ScanResult> mWifiList;

    private boolean mScanning = false;

    private HashMap<String, String> mApMap;

    private List<String> mAutoCompleteSSIDs;

    private List<String> mAutoCompleteBSSIDs;
    private ArrayAdapter<String> mAutoCompleteSSIDAdapter;

    private SharedPreferences mApPref;

    private ActivityConfigureOptionBinding mBinding;

    TextInputLayout textInputLayout, textInputBssid;

    AppCompatAutoCompleteTextView idWifi, bSsidWifi;


    TextInputEditText passwordWifi, meshID, meshPa, mqttHost, mqttToken;

    RadioGroup radioGroup;
    RadioButton idle, root, node;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityConfigureOptionBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
//        setSupportActionBar(mBinding.toolbar);
        setHomeAsUpEnable(true);
        setContentView(R.layout.activity_configure_option);


//        String deviceName = mDevice.getName() == null ? getString(R.string.string_unknown) : mDevice.getName();
//        setTitle(deviceName);

//        Add by user

        textInputLayout = findViewById(R.id.station_wifi_ssid_form);
//        textInputBssid = findViewById(R.id.station_wifi_bssid_form);

        idWifi = findViewById(R.id.station_ssid);
        passwordWifi = findViewById(R.id.station_wifi_password);

        bSsidWifi = findViewById(R.id.station_bssid);
        meshID = findViewById(R.id.mesh_id);
        meshPa = findViewById(R.id.mesh_password);

        radioGroup = findViewById(R.id.radio_group);
        idle = findViewById(R.id.idle_con);
        root = findViewById(R.id.root_con);
        node = findViewById(R.id.node_con);

        mqttHost = findViewById(R.id.mqtt_host);
        mqttToken = findViewById(R.id.mqtt_token);


        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        mApPref = getSharedPreferences(PREF_AP, MODE_PRIVATE);


        mApMap = new HashMap<>();
        mAutoCompleteSSIDs = new LinkedList<>();

        mAutoCompleteBSSIDs = new LinkedList<>();

        loadAps();
        mWifiList = new ArrayList<>();

//        Scan wifi from SmartPhone

//        mBinding.stationWifiSsidForm.setEndIconOnClickListener(v -> scanWifi());


        textInputLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
//            public void onClick(View v) {
//                scanWifi();
//            }
            public void onClick(View v) {
                scanBssid();
            }
        });

//        textInputBssid.setEndIconOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                scanBssid();
//            }
//        });

        mAutoCompleteSSIDAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line);
        idWifi.setAdapter(mAutoCompleteSSIDAdapter);
        idWifi.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String pwd = mApMap.get(s.toString());
                mBinding.stationWifiPassword.setText(pwd);
                idWifi.setTag(null);
            }
        });

        idWifi.setText(getConnectionSSID());

        WifiInfo info = mWifiManager.getConnectionInfo();
        if (info != null) {
            byte[] ssidBytes = getSSIDRawData(info);
            idWifi.setTag(ssidBytes);
        }

        findViewById(R.id.confirm).setOnClickListener(v -> configure());

        mBlufiClient = new BlufiClient(getApplication(), BlufiActivity.mDevice);
//        mBlufiClient.setGattCallback(new BlufiActivity.GattCallback());
//        mBlufiClient.setBlufiCallback(new BlufiActivity.BlufiCallbackMain());
        mBlufiClient.setGattWriteTimeout(BlufiConstants.GATT_WRITE_TIMEOUT);
        mBlufiClient.connect();

        Observable.just(this)
                .subscribeOn(Schedulers.io())
                .doOnNext(ConfigureOptionActivity::updateWifi)
                .subscribe();
    }


    private boolean is5GHz(int freq) {
        return freq > 4900 && freq < 5900;
    }


    private byte[] getSSIDRawData(WifiInfo info) {
        try {
            Method method = info.getClass().getMethod("getWifiSsid");
            method.setAccessible(true);
            Object wifiSsid = method.invoke(info);
            if (wifiSsid == null) {
                return null;
            }

            method = wifiSsid.getClass().getMethod("getOctets");
            method.setAccessible(true);
            return (byte[]) method.invoke(wifiSsid);

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                 NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] getSSIDRawData(ScanResult scanResult) {
        try {
            Field field = scanResult.getClass().getField("wifiSsid");
            field.setAccessible(true);
            Object wifiSsid = field.get(scanResult);
            if (wifiSsid == null) {
                return null;
            }
            Method method = wifiSsid.getClass().getMethod("getOctects");
            method.setAccessible(true);
            return (byte[]) method.invoke(wifiSsid);
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getConnectionSSID() {
        if (!mWifiManager.isWifiEnabled()) {
            return null;
        }
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            return null;
        }

        String ssid = wifiInfo.getSSID();
        if (ssid.startsWith("\"") && ssid.endsWith("\"") && ssid.length() >= 2) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }
        return ssid;
    }

    private int getConnectionFrequency() {
        if (!mWifiManager.isWifiEnabled()) {
            return -1;
        }

        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            return -1;
        }
        return wifiInfo.getFrequency();
    }


    private void loadAps() {
        Map<String, ?> aps = mApPref.getAll();
        for (Map.Entry<String, ?> entry : aps.entrySet()) {
            mApMap.put(entry.getKey(), entry.getValue().toString());
            mAutoCompleteSSIDs.add(entry.getKey());
        }
    }
    /*
     *
     * Scan SSID Wifi
     *
     * */


    private void scanWifi() {
        if (!mWifiManager.isWifiEnabled()) {
            Toast.makeText(this, R.string.configure_wifi_disable_msg, Toast.LENGTH_SHORT).show();
            return;
        }

        if (mScanning) {
            return;
        }

        mScanning = true;

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        dialog.setMessage(getString(R.string.configure_station_wifi_scanning));
        dialog.show();

        Observable.just(mWifiManager)
                .subscribeOn(Schedulers.io())
                .doOnNext(wm -> {
                    wm.startScan();
                    try {
                        Thread.sleep(1500L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    updateWifi();
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
                    dialog.dismiss();
                    showWifiListDialog();
                    mScanning = false;
                })
                .subscribe();
    }

    private void updateWifi() {
        final List<ScanResult> scans = new LinkedList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Observable.fromIterable(mWifiManager.getScanResults())
                .filter(scanResult -> {
                    if (TextUtils.isEmpty(scanResult.SSID)) {
                        return false;
                    }

                    boolean contain = false;
                    for (ScanResult srScaned : scans) {
                        if (srScaned.SSID.equals(scanResult.SSID)) {
                            contain = true;
                            break;
                        }
                    }
                    return !contain;
                })
                .doOnNext(scans::add)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
                    mWifiList.clear();
                    mWifiList.addAll(scans);

                    mAutoCompleteSSIDs.clear();
                    Set<String> apDBSet = mApMap.keySet();
                    mAutoCompleteSSIDs.addAll(apDBSet);
                    Observable.fromIterable(mWifiList)
                            .filter(scanResult -> !apDBSet.contains(scanResult.SSID))
                            .doOnNext(scanResult -> mAutoCompleteSSIDs.add(scanResult.SSID))
                            .subscribe();
                    mAutoCompleteSSIDAdapter.notifyDataSetChanged();
                })
                .subscribe();
    }


    /*
     * Show wifilist dialog
     * */
    private void showWifiListDialog() {
        int count = mWifiList.size();
        if (count == 0) {
            Toast.makeText(this, R.string.configure_station_wifi_scanning_nothing, Toast.LENGTH_SHORT).show();
            return;
        }

        int checkedItem = -1;
        String inputSsid = idWifi.getText().toString();
        final String[] wifiSSIDs = new String[count];
        for (int i = 0; i < count; i++) {
            ScanResult sr = mWifiList.get(i);
//            ---------Fix scan BSSID-------
//            Scan SSID

            wifiSSIDs[i] = sr.SSID;
            if (inputSsid.equals(sr.SSID)) {
                checkedItem = i;
            }
        }

        new AlertDialog.Builder(this).setSingleChoiceItems(wifiSSIDs, checkedItem, (dialog, i) -> {
            idWifi.setText(wifiSSIDs[i]);
            ScanResult scanResult = mWifiList.get(i);
            byte[] ssidBytes = getSSIDRawData(scanResult);
            idWifi.setTag(ssidBytes);
            dialog.dismiss();
        }).show();
    }


    /*
     *
     * Scan BSSID Wifi
     *
     * */


    private void scanBssid() {
        if (!mWifiManager.isWifiEnabled()) {
            Toast.makeText(this, R.string.configure_wifi_disable_msg, Toast.LENGTH_SHORT).show();
            return;
        }

        if (mScanning) {
            return;
        }

        mScanning = true;

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        dialog.setMessage(getString(R.string.configure_station_wifi_scanning));
        dialog.show();

        Observable.just(mWifiManager)
                .subscribeOn(Schedulers.io())
                .doOnNext(wm -> {
                    wm.startScan();
                    try {
                        Thread.sleep(1500L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    updateBssid();
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
                    dialog.dismiss();
                    showBSSIDWifiDialog();
                    mScanning = false;
                })
                .subscribe();
    }


    private void updateBssid() {
        final List<ScanResult> scans = new LinkedList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Observable.fromIterable(mWifiManager.getScanResults())
                .filter(scanResult -> {
                    if (TextUtils.isEmpty(scanResult.BSSID)) {
                        return false;
                    }

                    boolean contain = false;
                    for (ScanResult srScaned : scans) {
                        if (srScaned.BSSID.equals(scanResult.BSSID)) {
                            contain = true;
                            break;
                        }
                    }
                    return !contain;
                })
                .doOnNext(scans::add)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
                    mWifiList.clear();
                    mWifiList.addAll(scans);

                    mAutoCompleteSSIDs.clear();
                    Set<String> apDBSet = mApMap.keySet();
                    mAutoCompleteSSIDs.addAll(apDBSet);
                    Observable.fromIterable(mWifiList)
                            .filter(scanResult -> !apDBSet.contains(scanResult.BSSID))
                            .doOnNext(scanResult -> mAutoCompleteSSIDs.add(scanResult.BSSID))
                            .subscribe();
                    mAutoCompleteSSIDAdapter.notifyDataSetChanged();
                })
                .subscribe();
    }


    /*
     * Show BSSID List dialog
     * */
    private void showBSSIDWifiDialog() {
        int count = mWifiList.size();
        if (count == 0) {
            Toast.makeText(this, R.string.configure_station_wifi_scanning_nothing, Toast.LENGTH_SHORT).show();
            return;
        }

        int checkedItem = -1;
        String inputSsid = bSsidWifi.getText().toString();
        String inputWifi = idWifi.getText().toString();

        final String[] wifiSSIDs = new String[count];
        final String[] bSSIDs = new String[count];
        final String[] idWi = new String[count];


        for (int i = 0; i < count; i++) {
            ScanResult sr = mWifiList.get(i);
//            ---------Fix scan BSSID-------
//            Scan SSID

            wifiSSIDs[i] = sr.BSSID + " " + sr.SSID + " " + "rssi:" + sr.level;
            bSSIDs[i] = sr.BSSID;
            idWi[i] = sr.SSID;

            if (inputSsid.equals(sr.BSSID)) {
                checkedItem = i;
            }
        }

        new AlertDialog.Builder(this).setSingleChoiceItems(wifiSSIDs, checkedItem, (dialog, i) -> {
            bSsidWifi.setText(bSSIDs[i]);
            idWifi.setText(idWi[i]);
            ScanResult scanResult = mWifiList.get(i);
            byte[] ssidBytes = getSSIDRawData(scanResult);
            bSsidWifi.setTag(ssidBytes);
            dialog.dismiss();
//
//            idWifi.setText(wifiSSIDs[i]);
//            ScanResult scanResult = mWifiList.get(i);
//            byte[] ssidBytes = getSSIDRawData(scanResult);
//            idWifi.setTag(ssidBytes);
            dialog.dismiss();

        }).show();
    }

    private BlufiConfigureParams checkInfo() {
        BlufiConfigureParams params = new BlufiConfigureParams();
        return params;
    }

    private boolean checkSta(BlufiConfigureParams params) {
        String ssid = mBinding.stationSsid.getText().toString();
        if (TextUtils.isEmpty(ssid)) {
            mBinding.stationSsid.setError(getString(R.string.configure_station_ssid_error));
            return false;
        }

        byte[] ssidBytes = (byte[]) mBinding.stationSsid.getTag();
        params.setStaSSIDBytes(ssidBytes != null ? ssidBytes : ssid.getBytes());
        String password = mBinding.stationWifiPassword.getText().toString();
        params.setStaPassword(password);

        int freq = -1;
        if (ssid.equals(getConnectionSSID())) {
            freq = getConnectionFrequency();
        }
        if (freq == -1) {
            for (ScanResult sr : mWifiList) {
                if (ssid.equals(sr.SSID)) {
                    freq = sr.frequency;
                    break;
                }
            }
        }
        if (is5GHz(freq)) {
            mBinding.stationSsid.setError(getString(R.string.configure_station_wifi_5g_error));
            new AlertDialog.Builder(this)
                    .setMessage(R.string.configure_station_wifi_5g_dialog_message)
                    .setPositiveButton(R.string.configure_station_wifi_5g_dialog_continue, (dialog, which) ->
                    {
                        finishWithParams(params);
                    })
                    .setNegativeButton(R.string.configure_station_wifi_5g_dialog_cancel, null)
                    .show();
            return false;


        }
        return true;

    }

    //    --------------------Task test-------------------

    private static class Message {
        String text;
        boolean isNotification;
    }

    private void updateMessage(String message, boolean isNotificaiton) {
        runOnUiThread(() -> {
            Message msg = new Message();
            msg.text = message;
            msg.isNotification = isNotificaiton;
            mMsgList.add(msg);
            mMsgAdapter.notifyItemInserted(mMsgList.size() - 1);
//            mContent.recyclerView.scrollToPosition(mMsgList.size() - 1);
        });
    }


    private class BlufiCallbackMain extends BlufiCallback {
        @Override
        public void onGattPrepared(
                BlufiClient client,
                BluetoothGatt gatt,
                BluetoothGattService service,
                BluetoothGattCharacteristic writeChar,
                BluetoothGattCharacteristic notifyChar
        ) {
            if (service == null) {
                mLog.w("Discover service failed");
//                gatt.disconnect();
                updateMessage("Discover service failed", false);
                return;
            }
            if (writeChar == null) {
                mLog.w("Get write characteristic failed");
//                gatt.disconnect();
                updateMessage("Get write characteristic failed", false);
                return;
            }
            if (notifyChar == null) {
                mLog.w("Get notification characteristic failed");
//                gatt.disconnect();
                updateMessage("Get notification characteristic failed", false);
                return;
            }

            updateMessage("Discover service and characteristics success", false);

//            int mtu = BlufiConstants.DEFAULT_MTU_LENGTH;
//            mLog.d("Request MTU " + mtu);
//            boolean requestMtu = gatt.requestMtu(mtu);
//            if (!requestMtu) {
//                mLog.w("Request mtu failed");
//                updateMessage(String.format(Locale.ENGLISH, "Request mtu %d failed", mtu), false);
//                onGattServiceCharacteristicDiscovered();
//            }
        }

        @Override
        public void onNegotiateSecurityResult(BlufiClient client, int status) {
            if (status == STATUS_SUCCESS) {
                updateMessage("Negotiate security complete", false);
            } else {
                updateMessage("Negotiate security failed， code=" + status, false);
            }

//            mContent.blufiSecurity.setEnabled(mConnected);
        }

        @Override
        public void onPostConfigureParams(BlufiClient client, int status) {
            if (status == STATUS_SUCCESS) {
                updateMessage("Post configure params complete", false);
            } else {
                updateMessage("Post configure params failed, code=" + status, false);
            }

//            mContent.blufiConfigure.setEnabled(mConnected);
        }

        @Override
        public void onDeviceStatusResponse(BlufiClient client, int status, BlufiStatusResponse response) {
            if (status == STATUS_SUCCESS) {
                updateMessage(String.format("Receive device status response:\n%s", response.generateValidInfo()),
                        true);
                mBlufiActivity.disconnectGatt();
            } else {
                updateMessage("Device status response error, code=" + status, false);
            }

//            mContent.blufiDeviceStatus.setEnabled(mConnected);
        }

        @Override
        public void onDeviceScanResult(BlufiClient client, int status, List<BlufiScanResult> results) {
            if (status == STATUS_SUCCESS) {
                StringBuilder msg = new StringBuilder();
                msg.append("Receive device scan result:\n");
                for (BlufiScanResult scanResult : results) {
                    msg.append(scanResult.toString()).append("\n");
                }
                updateMessage(msg.toString(), true);
            } else {
                updateMessage("Device scan result error, code=" + status, false);
            }

//            mContent.blufiDeviceScan.setEnabled(mConnected);
        }

        @Override
        public void onDeviceVersionResponse(BlufiClient client, int status, BlufiVersionResponse response) {
            if (status == STATUS_SUCCESS) {
                updateMessage(String.format("Receive device version: %s", response.getVersionString()),
                        true);
            } else {
                updateMessage("Device version error, code=" + status, false);
            }

//            mContent.blufiVersion.setEnabled(mConnected);
        }

        @Override
        public void onPostCustomDataResult(BlufiClient client, int status, byte[] data) {
            String dataStr = new String(data);
            String format = "Post data %s %s";
            if (status == STATUS_SUCCESS) {
                updateMessage(String.format(format, dataStr, "complete"), false);
            } else {
                updateMessage(String.format(format, dataStr, "failed"), false);
            }
        }

        @Override
        public void onReceiveCustomData(BlufiClient client, int status, byte[] data) {
            if (status == STATUS_SUCCESS) {
                String customStr = new String(data);
                updateMessage(String.format("Receive custom data:\n%s", customStr), true);
            } else {
                updateMessage("Receive custom data error, code=" + status, false);
            }
        }

//        @Override
//        public void onError(BlufiClient client, int errCode) {
//            updateMessage(String.format(Locale.ENGLISH, "Receive error code %d", errCode), false);
//            if (errCode == CODE_GATT_WRITE_TIMEOUT) {
//                updateMessage("Gatt write timeout", false);
//                client.close();
//                onGattDisconnected();
//            } else if (errCode == CODE_WIFI_SCAN_FAIL) {
//                updateMessage("Scan failed, please retry later", false);
//                mContent.blufiDeviceScan.setEnabled(true);
//            }
//        }
    }


    private void saveAP(BlufiConfigureParams params) {
        String ssid = new String(params.getStaSSIDBytes());
        String pwd = params.getStaPassword();
        mApPref.edit().putString(ssid, pwd).apply();

    }

    private void finishWithParams(BlufiConfigureParams params) {
        Intent intent = new Intent();
        intent.putExtra(BlufiConstants.KEY_CONFIGURE_PARAM, params);

        saveAP(params);

        setResult(RESULT_OK, intent);
        finish();
    }

    private static class MsgHolder extends RecyclerView.ViewHolder {
        TextView text1;

        MsgHolder(BlufiMessageItemBinding binding) {
            super(binding.getRoot());

            text1 = binding.text1;
        }
    }

    private class MsgAdapter extends RecyclerView.Adapter<MsgHolder> {

        @NonNull
        @Override
        public MsgHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            BlufiMessageItemBinding binding = BlufiMessageItemBinding.inflate(
                    getLayoutInflater(),
                    parent,
                    false
            );
            return new MsgHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull MsgHolder holder, int position) {
            Message msg = mMsgList.get(position);
            holder.text1.setText(msg.text);
            holder.text1.setTextColor(msg.isNotification ? Color.RED : Color.BLACK);
        }

        @Override
        public int getItemCount() {
            return mMsgList.size();
        }
    }


    public static String padRight(String s, int n) {
        return String.format("%0$-" + n + "s", s);
    }

    public static String padLeft(String s){
//       return String.format("d%s" + (8-s.length()) + "%0",s,0);
//        return String.format("%08d", s);

        return String.format("%0"+ (8 - s.length() )+"d%s",0 ,s);
    }


    private void configure() {
//        mBlufiActivity.connect();
        byte[] macByteBssid = new byte[6];


        if (idle.isChecked()) {
            meshtype = 0;
//                        Toast.makeText(this, "Select Idle", Toast.LENGTH_SHORT).show();
        } else if (root.isChecked()) {
            meshtype = 1;
//                        Toast.makeText(this, "Select Root", Toast.LENGTH_SHORT).show();
        } else if (node.isChecked()) {
            meshtype = 2;
//                        Toast.makeText(this, "Select Node", Toast.LENGTH_SHORT).show();
        }

        String dataRouterID = idWifi.getText().toString();
        String passwordRouterID = passwordWifi.getText().toString();
        String bSSID = bSsidWifi.getText().toString();
        String dataMeshID = meshID.getText().toString();
        String dataMeshPa = meshID.getText().toString();
        String host = mqttHost.getText().toString();
        String token = mqttToken.getText().toString();

        String[] macAddr = bSSID.split(":");


        for (int i = 0; i < 6; i++) {
            Integer hex = Integer.parseInt(macAddr[i], 16);
            macByteBssid[i] = hex.byteValue();
        }

//        Kiem tra dieu kien MESHID
        if (dataMeshID.isEmpty()) {
            Toast.makeText(this, "Chưa điền giá trị Mesh ID. Vui lòng cấu hình lại???", Toast.LENGTH_LONG).show();
            return;
        }
        if (dataMeshID.length() > 6) {
            meshID.selectAll();
            meshID.requestFocus();
            Toast.makeText(this, "Mesh ID phải ít hơn hoặc bằng 6 kí tự", Toast.LENGTH_LONG).show();
            return;
        }

        if (dataMeshID.length() < 6 && dataMeshID.length() > 0) {
            dataMeshID = padRight(dataMeshID, 6).replace(" ", "\0");
//
//            Log.d(TAG, dataMeshID);

        }


        if (dataMeshPa.length() < 9 && dataMeshPa.length() > 0) {
            dataMeshPa = padLeft(dataMeshPa);
        }

        //                    Kiem tra dieu kien MQTT_Token
        if (token.isEmpty()) {
            Toast.makeText(this, "Chưa điền giá trị MQTT Token. Vui lòng cấu hình lại???", Toast.LENGTH_LONG).show();
            return;
        }
        if (token.length() > 12) {
            mqttHost.selectAll();
            mqttHost.requestFocus();
            Toast.makeText(this, "MQTT Token phải ít hơn hoặc bằng 12 kí tự", Toast.LENGTH_LONG).show();
            return;
        }

        if (token.length() < 12 && token.length() > 0) {
            token = padRight(token, 12).replace(" ", "\0");
//            Log.d(TAG, token);
        }


//        System.out.println("BSSID-------" + bSSID);


        int length_ssid = dataRouterID.length();
        int length_password = passwordRouterID.length();
        int length_mesh_id = dataMeshID.length();
        int length_mesh_password = dataMeshPa.length();
        int length_mqttHost = host.length();
        int length_mqttToken = token.length();


        byte[] byte1 = {1, (byte) length_ssid};
        byte[] byte2_type = {2};
        byte[] byte2_length = {(byte) length_password};
        byte[] byte3_type = {3, 6};

        byte[] byte4_type = {4};
        byte[] byte4_length = {(byte) length_mesh_id};

        byte[] byte5_type = {5};
        byte[] byte5_length = {(byte)length_mesh_password};

        byte[] byte6_type = {6, 1, (byte) meshtype};

        byte[] byte30_type = {0x30};
        byte[] byte30_length = {(byte) length_mqttHost};

        byte[] byte31_type = {0x31};
        byte[] byte31_length = {(byte) length_mqttToken};


        byte[] allByteArray = new byte[byte1.length + dataRouterID.getBytes().length
                + byte2_type.length + byte2_length.length + passwordRouterID.getBytes().length
                + byte3_type.length + macByteBssid.length
                + byte4_type.length + byte4_length.length + dataMeshID.getBytes().length
                + byte5_type.length + byte5_length.length + dataMeshPa.getBytes().length
                + byte6_type.length
                + byte30_type.length + byte30_length.length + host.getBytes().length
                + byte31_type.length + byte31_length.length + token.getBytes().length];

        ByteBuffer buff = ByteBuffer.wrap(allByteArray);
        buff.put(byte1);
        buff.put(dataRouterID.getBytes());
        buff.put(byte2_type);
        buff.put(byte2_length);
        buff.put(passwordRouterID.getBytes());
        buff.put(byte3_type);
        buff.put(macByteBssid);
        buff.put(byte4_type);
        buff.put(byte4_length);
        buff.put(dataMeshID.getBytes());
        buff.put(byte5_type);
        buff.put(byte5_length);
        buff.put(dataMeshPa.getBytes());
        buff.put(byte6_type);
        buff.put(byte30_type);
        buff.put(byte30_length);
        buff.put(host.getBytes());
        buff.put(byte31_type);
        buff.put(byte31_length);
        buff.put(token.getBytes());


        byte[] combined = buff.array();


//        mBlufiClient.postCustomData(combined);
//        mBlufiClient.postCustomData(bSSID.getBytes());
        mBlufiClient.postCustomData(combined);

        Toast.makeText(this, "Finish--------", Toast.LENGTH_LONG).show();
//        mBlufiActivity.disconnectGatt();

    }


}
