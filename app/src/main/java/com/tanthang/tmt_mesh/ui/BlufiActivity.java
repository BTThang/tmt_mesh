package com.tanthang.tmt_mesh.ui;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.tanthang.tmt_mesh.R;
import com.tanthang.tmt_mesh.app.BaseActivity;
import com.tanthang.tmt_mesh.app.BlufiLog;
import com.tanthang.tmt_mesh.constants.BlufiConstants;
import com.tanthang.tmt_mesh.databinding.ActivityBlufiBinding;
import com.tanthang.tmt_mesh.databinding.ActivityMainBinding;
import com.tanthang.tmt_mesh.databinding.BlufiContentBinding;
import com.tanthang.tmt_mesh.databinding.BlufiMessageItemBinding;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import blufi.espressif.BlufiCallback;
import blufi.espressif.BlufiClient;
import blufi.espressif.params.BlufiConfigureParams;
import blufi.espressif.params.BlufiParameter;
import blufi.espressif.response.BlufiStatusResponse;

@SuppressLint("MissingPermission")

public class BlufiActivity extends BaseActivity {

    private static final int REQUEST_CONFIGURE = 0x20;
    private static String TAG;

    private final BlufiLog mLog = new BlufiLog(getClass());

    public static BluetoothDevice mDevice;
    private BlufiClient mBlufiClient;
    private volatile boolean mConnected;

    private List<Message> mMsgList;
    private MsgAdapter mMsgAdapter;

    private BlufiContentBinding mContent;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityBlufiBinding mBinding = ActivityBlufiBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        setSupportActionBar(mBinding.toolbar);
        setHomeAsUpEnable(true);

        mDevice = getIntent().getParcelableExtra(BlufiConstants.KEY_BLE_DEVICE);
        assert mDevice != null;

        String deviceName = mDevice.getName() == null ? getString(R.string.string_unknown) : mDevice.getName();
        setTitle(deviceName);

        mContent = mBinding.content;

        mMsgList = new LinkedList<>();
        mMsgAdapter = new MsgAdapter();
        mContent.recyclerView.setAdapter(mMsgAdapter);

        BlufiButtonListener clickListener = new BlufiButtonListener();

        mContent.blufiConnect.setOnClickListener(clickListener);
        mContent.blufiConnect.setOnLongClickListener(clickListener);

        mContent.blufiDisconnect.setOnClickListener(clickListener);
        mContent.blufiDisconnect.setOnLongClickListener(clickListener);
        mContent.blufiDisconnect.setEnabled(false);

        mContent.blufiConfig.setOnClickListener(clickListener);
        mContent.blufiConfig.setOnLongClickListener(clickListener);
        mContent.blufiConfig.setEnabled(false);

        mContent.blufiSecurity.setOnClickListener(clickListener);
        mContent.blufiSecurity.setOnLongClickListener(clickListener);
        mContent.blufiSecurity.setEnabled(false);

//        setContentView(R.layout.activity_blufi);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBlufiClient != null) {
            mBlufiClient.close();
            mBlufiClient = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CONFIGURE) {
            if (!mConnected) {
                return;
            }

            if (resultCode == RESULT_OK) {
                BlufiConfigureParams params = (BlufiConfigureParams) data.getSerializableExtra(BlufiConstants.KEY_CONFIGURE_PARAM);
                configure(params);

            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateMessage(String message, boolean isNotification) {
        runOnUiThread(() -> {
            Message msg = new Message();
            msg.text = message;
            msg.isNotification = isNotification;
            mMsgList.add(msg);
            mMsgAdapter.notifyItemInserted(mMsgList.size() - 1);
            mContent.recyclerView.scrollToPosition(mMsgList.size() - 1);
        });
    }

    /**
     * Try to connect device*
     */
    public void connect() {
        mContent.blufiConnect.setEnabled(false);

        if (mBlufiClient != null) {
            mBlufiClient.close();
            mBlufiClient = null;
        }

        mBlufiClient = new BlufiClient(getApplicationContext(), mDevice);
        mBlufiClient.setGattCallback(new GattCallback());
        mBlufiClient.setBlufiCallback(new BlufiCallbackMain());
        mBlufiClient.setGattWriteTimeout(BlufiConstants.GATT_WRITE_TIMEOUT);
        mBlufiClient.connect();
    }

    /**
     * Request device disconnect the connection
     */
    public void disconnectGatt() {
        mContent.blufiDisconnect.setEnabled(false);

        if (mBlufiClient != null) {
            mBlufiClient.requestCloseConnection();
        }
    }

    /**
     * If negotiate security success, the continue communication data will be encrypted.
     */

    private void negotiateSecurity() {
        mContent.blufiSecurity.setEnabled(false);
        mBlufiClient.negotiateSecurity();
    }

    /**
     * Go to configure options
     */
    private void configureOptions() {
        Intent intent = new Intent(BlufiActivity.this, ConfigureOptionActivity.class);
        startActivityForResult(intent, REQUEST_CONFIGURE);
    }

    /**
     * Request to configure station or softap
     *
     * @param params configure params
     */
    private void configure(BlufiConfigureParams params) {
        mContent.blufiConfig.setEnabled(false);

        mBlufiClient.configure(params);
    }

    private void onGattConnected() {
        mConnected = true;
        runOnUiThread(() -> {
            mContent.blufiConnect.setEnabled(false);

            mContent.blufiDisconnect.setEnabled(true);
        });
    }

    private void onGattServiceCharacteristicDiscovered() {
        runOnUiThread(() -> {
            mContent.blufiSecurity.setEnabled(true);
            mContent.blufiConfig.setEnabled(true);
        });
    }

    private void onGattDisconnected() {
        mConnected = false;
        runOnUiThread(() -> {
            mContent.blufiConnect.setEnabled(true);


            mContent.blufiDisconnect.setEnabled(false);
            mContent.blufiConfig.setEnabled(false);
            mContent.blufiSecurity.setEnabled(false);
        });
    }

    /**
     * mBlufiClient call onCharacteristicWrite and onCharacteristicChanged is required
     */

    private class GattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String devAddr = gatt.getDevice().getAddress();
            mLog.d(String.format(Locale.ENGLISH, "onConnectionStateChange addr=%s, status=%s, newState = %d", devAddr, status, newState));

            if (status == BluetoothGatt.GATT_SUCCESS) {
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        onGattConnected();
                        updateMessage(String.format("Connected %s", devAddr), false);
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        gatt.close();
                        onGattDisconnected();
                        updateMessage(String.format("Disconnected %s", devAddr), false);
                        break;
                }
            } else {
                gatt.close();
                onGattDisconnected();
                updateMessage(String.format(Locale.ENGLISH, "Disconnect %s, status = %d", devAddr, status), false);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            mLog.d(String.format(Locale.ENGLISH, "onMtuChanged status=%d, mtu= %d", status, mtu));
            if (status == BluetoothGatt.GATT_SUCCESS) {
                updateMessage(String.format(Locale.ENGLISH, "Set mtu complete, mtu=%d", mtu), false);
            } else {
                mBlufiClient.setPostPackageLengthLimit(20);
                updateMessage(String.format(Locale.ENGLISH, "Set mtu failed, mtu = %d, status = %d", mtu, status), false);
            }

            onGattServiceCharacteristicDiscovered();
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            mLog.d(String.format(Locale.ENGLISH, "onServicesDiscovered status = %d", status));
            if (status != BluetoothGatt.GATT_SUCCESS) {
                gatt.disconnect();
                updateMessage(String.format(Locale.ENGLISH, "Discover services error status %d", status), false);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            mLog.d(String.format(Locale.ENGLISH, "onDescriptorWrite status=%d", status));
            if (descriptor.getUuid().equals(BlufiParameter.UUID_NOTIFICATION_DESCRIPTOR) &&
                    descriptor.getCharacteristic().getUuid().equals(BlufiParameter.UUID_NOTIFICATION_CHARACTERISTIC)) {
                String msg = String.format(Locale.ENGLISH, "Set notification enable %s", (status == BluetoothGatt.GATT_SUCCESS ? " complete" : " failed"));
                updateMessage(msg, false);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                gatt.disconnect();
                updateMessage(String.format(Locale.ENGLISH, "WriteChar error status %d", status), false);
            }
        }


    }


    public class BlufiCallbackMain extends BlufiCallback {
        @Override
        public void onGattPrepared(
                BlufiClient client,
                BluetoothGatt gatt,
                BluetoothGattService service,
                BluetoothGattCharacteristic writeChar,
                BluetoothGattCharacteristic notifyChar
        ){
            if (service == null) {
                mLog.w("Discover service failed");
                gatt.disconnect();
                updateMessage("Discover service failde", false);
                return;
            }
            if (writeChar == null) {
                mLog.w("Get write characteristic failed");
                gatt.disconnect();
                updateMessage("Get write characteristic failed", false);
                return;
            }
            if (notifyChar == null) {
                mLog.w("Get notification characteristic failed");
                gatt.disconnect();
                updateMessage("Get notification characteristic failed", false);
                return;
            }

            updateMessage("Discover service and characteristics success", false);

            int mtu = BlufiConstants.DEFAULT_MTU_LENGTH;
            mLog.d("Request MTU" + mtu);
            boolean requestMtu = gatt.requestMtu(mtu);
            if (!requestMtu) {
                mLog.w("Request mtu failed");
                updateMessage(String.format(Locale.ENGLISH, "Request mtu %d failed", mtu), false);
                onGattServiceCharacteristicDiscovered();
            }
        }

        @Override
        public void onNegotiateSecurityResult(BlufiClient client, int status) {
            if (status == STATUS_SUCCESS) {
                updateMessage("Negotiate security complete", false);
            } else {
                updateMessage("Negotiate security failed, code= " + status, false);
            }

            mContent.blufiSecurity.setEnabled(mConnected);
        }

        @Override
        public void onPostConfigureParams(BlufiClient client, int status) {
            if (status == STATUS_SUCCESS) {
                updateMessage("Post configure params complete", false);
            } else {
                updateMessage("Post configure params failed, code=" + status, false);
            }

            mContent.blufiConfig.setEnabled(mConnected);
        }

        @Override
        public void onDeviceStatusResponse(BlufiClient client, int status, BlufiStatusResponse response) {
            if (status == STATUS_SUCCESS) {
                updateMessage(String.format("Receive device status response:\n%s", response.generateValidInfo()),
                        true);
                disconnectGatt();
            } else {
                updateMessage("Device status response error, code=" + status, false);
            }

//            mContent.blufiDeviceStatus.setEnabled(mConnected);
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

        @Override
        public void onError(BlufiClient client, int errCode) {
            updateMessage(String.format(Locale.ENGLISH, "Receive error code %d", errCode), false);
            if (errCode == CODE_GATT_WRITE_TIMEOUT) {
                updateMessage("Gatt write timeout", false);
                client.close();
                onGattDisconnected();
            }
        }
    }


    private class BlufiButtonListener implements View.OnClickListener, View.OnLongClickListener {
        private Toast mToast;

        @Override
        public void onClick(View v) {
            if (v == mContent.blufiConnect) {
                connect();
            } else if (v == mContent.blufiDisconnect) {
                disconnectGatt();
            } else if (v == mContent.blufiConfig) {
                configureOptions();
            } else if (v == mContent.blufiSecurity) {
                negotiateSecurity();
            }
        }

        @Override
        public boolean onLongClick(View v) {

            if (mToast != null) {
                mToast.cancel();
            }

            int msgRes = 0;
            if (v == mContent.blufiConnect) {
                msgRes = R.string.blufi_function_connect_msg;
            } else if (v == mContent.blufiDisconnect) {
                msgRes = R.string.blufi_function_disconnect_msg;
            } else if (v == mContent.blufiConfig) {
                msgRes = R.string.blufi_function_configure_msg;
            } else if (v == mContent.blufiSecurity) {
                msgRes = R.string.blufi_function_security_msg;
            }

            mToast = Toast.makeText(BlufiActivity.this, msgRes, Toast.LENGTH_LONG);
            mToast.show();

            return true;
        }
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
            BlufiMessageItemBinding binding = BlufiMessageItemBinding.inflate(getLayoutInflater(), parent, false);

            return new MsgHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull MsgHolder holder, int position) {
            BlufiActivity.Message msg = mMsgList.get(position);
            holder.text1.setText(msg.text);
            holder.text1.setTextColor(msg.isNotification ? Color.RED : Color.BLACK);
        }

        @Override
        public int getItemCount() {
            return mMsgList.size();
        }
    }

    private static class Message {
        String text;
        boolean isNotification;
    }
}