package blufi.espressif.response;

import java.util.Locale;

public class BlufiScanResult {
    public static final int TYPE_WIFI = 0x01;

    private int mType;
    private String mSsid;
    private int mRssi;
    private byte[] mBssid;

    public void setType(int type) {
        mType = type;
    }

    public int getType() {
        return mType;
    }

    public void setSsid(String ssid) {
        mSsid = ssid;
    }

    public String getSsid() {
        return mSsid;
    }

    public void setRssi(int rssi) {
        mRssi = rssi;
    }

    public int getRssi() {
        return mRssi;
    }

    public void setBssid(byte[] Bssid) {
        mBssid = Bssid;
    }

    public byte[] getBssid() {
        return mBssid;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "Bssid: %02x:%02x:%02x:%02x:%02x:%02x, ssid: %s, rssi: %d",mBssid[0],mBssid[1],mBssid[2],mBssid[3],mBssid[4],mBssid[5], mSsid, mRssi);
    }
}

