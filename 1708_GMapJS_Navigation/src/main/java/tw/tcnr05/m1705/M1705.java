package tw.tcnr05.m1705;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class M1705 extends AppCompatActivity  implements LocationListener {
    //-----------------所需要申請的權限數組---------------
    private static final String[] permissionsArray = new String[]{
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION};
    private List<String> permissionsList = new ArrayList<String>();
    //申請權限後的返回碼
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String MAP_URL = "file:///android_asset/001.html";// 自建的html檔名
    private static final String MAP_URL1 = "file:///android_asset/GoogleMap.html";// 自建的html檔名

    //private static final String MAP_URL = "https://city2farmer.com/tcnr05/Mask_Map/";// 自建的html檔名
    private WebView webview;
    private static String[][] locations = {
            { "我的位置", "0,0" },
            { "中區職訓", "24.172127,120.610313" },
            { "東海大學路思義教堂", "24.179051,120.600610" },
            { "台中公園湖心亭", "24.144671,120.683981" },
            { "秋紅谷", "24.1674900,120.6398902" },
            { "台中火車站", "24.136829,120.685011" },
            { "國立科學博物館", "24.1579361,120.6659828" } };
    private Spinner mSpnLocation;
    private String Lat,Lon;
    private String jcontent;
    private String provider=null;
    private TextView txtOutput;
    //private LocationManager locationManager;
    private LocationManager locationMgr;
    private String TAG="tcnr05==>";
    private Button bNav;
    private String Navon="off";
    private String Navstart,Navend;
    private int iSelect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.m1705);
        checkRequiredPermission(this);     //  檢查SDK版本, 確認是否獲得權限.
        setupComponent();
    }
//==========檢查定位==================================================
    private void checkRequiredPermission(Activity activity) {
        for (String permission : permissionsArray) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(permission);
            }
        }
        if (permissionsList.size() != 0) {
            ActivityCompat.requestPermissions(activity, permissionsList.toArray(new
                    String[permissionsList.size()]), REQUEST_CODE_ASK_PERMISSIONS);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(getApplicationContext(), permissions[i] + "權限申請成功!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "權限被拒絕： " + permissions[i], Toast.LENGTH_LONG).show();
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    private void setupComponent() {
        webview = (WebView)findViewById(R.id.webview);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.addJavascriptInterface(M1705.this, "AndroidFunction");
        webview.loadUrl(MAP_URL1);
        txtOutput = (TextView) findViewById(R.id.txtOutput);

        mSpnLocation = (Spinner) this.findViewById(R.id.spnLocation);
        mSpnLocation.getBackground().setAlpha(150);//0-255

        //		--導航監聽--
        bNav = (Button) findViewById(R.id.Navigation);
        bNav.setOnClickListener(bNavselectOn);

        // ----Location-----------
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.spinner_style);
        for (int i = 0; i < locations.length; i++)
            adapter.add(locations[i][0]);

        adapter.setDropDownViewResource(R.layout.spinner_style);
        mSpnLocation.setAdapter(adapter);
        mSpnLocation.setOnItemSelectedListener(mSpnLocationOnItemSelLis);
    }

    //	--導航監聽--
    private Button.OnClickListener bNavselectOn = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(Navon == "off"){
                bNav.setTextColor(getColor(R.color.Blue));
                Navon = "on";
                bNav.setText(R.string.b_off);
                setMapLocation();
            }else{
                bNav.setTextColor(getColor(R.color.Red));
                Navon = "off";
                bNav.setText(R.string.b_on);
                setMapLocation();
            }

        }
    };

    private AdapterView.OnItemSelectedListener mSpnLocationOnItemSelLis = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            setMapLocation();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private void setMapLocation() {
        iSelect = mSpnLocation.getSelectedItemPosition();
        String[] sLocation = locations[iSelect][1].split(",");
        Lat = sLocation[0]; // 南北緯
        Lon = sLocation[1]; // 東西經
        jcontent = locations[iSelect][0];  //地名
        //===========判斷是否導航===========================================
        if(Navon == "on" && iSelect != 0){
            Navstart = locations[0][1];
            Navend = locations[iSelect][1];
            final String deleteOverlays = "javascript: RoutePlanning()"; //=在html寫入走這個方法
            webview.loadUrl(deleteOverlays);
        }else{
            //===重新寫入==MAP.clean意思=====================
            webview.getSettings().setJavaScriptEnabled(true);                         //
            webview.addJavascriptInterface(M1705.this, "AndroidFunction");//
            webview.loadUrl(MAP_URL1);

        }
        //================================================================
    }

    //    private JSONArray ArryToJson() {
    private String ArryToJson() {
        JSONArray jArry = new JSONArray();

        for (int i = 1; i < locations.length; i++) {
            JSONObject jObj = new JSONObject();// 一定要放在這裡
            String[] arr = locations[i][1].split(",");

            try {
                jObj.put("title", locations[i][0]);
                jObj.put("jlat", arr[0]);
                jObj.put("jlon", arr[1]);
                jArry.put(jObj);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        String string_jArry=jArry.toString();
        return string_jArry;
//            return jArry;
    }


    private void updateWithNewLocation(Location location) {
        String where = "";
        if (location != null) {
            double lng = location.getLongitude();// 經度
            double lat = location.getLatitude();// 緯度
            float speed = location.getSpeed();// 速度
            long time = location.getTime();// 時間
            String timeString = getTimeString(time);
            where = "經度: " + lng + "\n緯度: " + lat + "\n速度: " + speed + "\n時間: " + timeString + "\nProvider: "
                    + provider;
            Lat = Double.toString(lat);
            Lon = Double.toString(lng);
            // 標記"我的位置"
            locations[0][1] = lat + "," + lng; // 用GPS找到的位置更換 陣列的目前位置
            // --- 呼叫 Map JS
            webview.loadUrl(MAP_URL1);
            // 位置改變顯示
            txtOutput.setText(where);

            //showMarkerMe(lat, lng);
            //cameraFocusOnMe(lat, lng);
        } else {
            where = "*位置訊號消失*";
        }
        // 位置改變顯示
        //txtOutput.setText(where);
    }

    //-------------------
    private void nowaddress() {
// 取得上次已知的位置
//        if (ActivityCompat.checkSelfPermission(this,
//                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
//                ActivityCompat.checkSelfPermission(this,
//                        android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            Location location = locationManager.getLastKnownLocation(provider);
//            updateWithNewLocation(location);
//            return;
//        }
        //檢查是否有權限-------------------------------------------
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = locationMgr.getLastKnownLocation(provider);
        updateWithNewLocation(location);
        // 監聽 GPS Listener
        locationMgr.addGpsStatusListener(gpsListener);
        // Location Listener
        long minTime = 5000;// ms
        float minDist = 5.0f;// meter
        locationMgr.requestLocationUpdates(provider, minTime, minDist,
                this); //開始座標移動
    }
    /***********************************************
     * timeInMilliseconds
     ***********************************************/
    private String getTimeString(long timeInMilliseconds) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(timeInMilliseconds);
    }

    /* 檢查GPS 是否開啟 */
    private boolean initLocationProvider() {
        locationMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
            return true;
        }
        return false;
    }

    // -------------------------------
    GpsStatus.Listener gpsListener = new GpsStatus.Listener() {
        /* 監聽GPS 狀態 */
        @Override
        public void onGpsStatusChanged(int event) {
            switch (event) {
                case GpsStatus.GPS_EVENT_STARTED:
                    Log.d(TAG, "GPS_EVENT_STARTED");
                    break;

                case GpsStatus.GPS_EVENT_STOPPED:
                    Log.d(TAG, "GPS_EVENT_STOPPED");
                    break;

                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    Log.d(TAG, "GPS_EVENT_FIRST_FIX");
                    break;

                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    Log.d(TAG, "GPS_EVENT_SATELLITE_STATUS");
                    break;
            }
        }
    };



    @Override
    protected void onStart() {
        super.onStart();
        if (initLocationProvider()) {
            nowaddress();
        } else {
            txtOutput.setText("GPS未開啟,請先開啟定位！");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        locationMgr.removeUpdates(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        Toast.makeText(getApplicationContext(), "請按右上結束", Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.m_return,menu);
        return (true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.Item01:
                webview.loadUrl(MAP_URL1);
            break;

            case R.id.Item02:
                webview.loadUrl(MAP_URL);
                break;

            case R.id.action_settings:
                this.finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }
    //===================================================
    @JavascriptInterface
    public String GetLat(){
        return Lat;
    }

    @JavascriptInterface
    public String GetLon(){ //GetLon對javascript而言(html)
        return Lon;
    }

    @JavascriptInterface
    public String Getjcontent(){
        return jcontent;
    }

    @JavascriptInterface
    public String GetJsonArry(){
        return ArryToJson();
    }
    //=====傳送導航資訊=========
    @JavascriptInterface
    public String Navon() {
        return Navon;
    }

    @JavascriptInterface
    public String Getstart() {
        return Navstart;
    }

    @JavascriptInterface
    public String Getend() {
        return Navend;
    }

    //===============================================================
    @Override
    public void onLocationChanged(Location location) {
        // 定位改變時
        updateWithNewLocation(location);
        // --- 呼叫 Map JS
        Navstart = locations[0][1];
        //---------增加判斷是否規畫路徑------------------
        if (Navon == "on" && iSelect != 0) {
            final String deleteOverlays = "javascript: RoutePlanning()";
            webview.loadUrl(deleteOverlays);
        }else{
            webview.getSettings().setJavaScriptEnabled(true);
            webview.addJavascriptInterface(M1705.this, "AndroidFunction");
            webview.loadUrl(MAP_URL1);
        }
        // ---
        Log.d(TAG, "onLocationChanged");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    //========================================================
}