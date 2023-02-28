package blufi.espressif.response;

import java.util.Locale;

public class BlufiCustomResult {
    public static final int TYPE_WIFI = 0x01;

    private int mType;
    private String mSsid;
    private int mRssi;
    private int mBssid;

    public void setmType(int type) {mType = type;}

    public int getmType() {return mType;}

    public void setmSsid(String ssid) {mSsid = ssid;}

    public String getmSsid(){return mSsid;}

    public void setmBssid(int Bssid) {mBssid = Bssid;}

    public int getmBssid() {return mBssid;}

    public void setmRssi(int rssi){mRssi = rssi;}

    public int getmRssi() {return mRssi;}


    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "ssid: %s, rssi: %d", mSsid, mRssi);
    }
}
