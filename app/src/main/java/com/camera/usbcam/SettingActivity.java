package com.camera.usbcam;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.camera.utils.PreferencesUtils;

import static com.camera.usbcam.MainActivity.getSdCardWriteableState;


public class SettingActivity extends Activity {
    public static final String FBL_KEY = "fenbianlv";
    public static final String SDstorge_KEY = "cunchuxuanzhi";
    private static final String[] fenbianlv = {"480x640", "768x1024", "720x1280", "1080x1920", "1200x1600"};
    private static final String[] cunchu = {"话机", "SD卡"};
    private TextView fenbianview, cunchuview;
    private Spinner fenbianspinner, cunchuspinner;
    private ArrayAdapter<String> fenbianadapter, cunchuadapter;
    private final String TAG = "huansen";
    private boolean issdcardable = false;//SD是否可用

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        fenbianview = (TextView) findViewById(R.id.spinnerText);
        fenbianspinner = (Spinner) findViewById(R.id.Spinner01);
        cunchuview = (TextView) findViewById(R.id.cunchuText);
        cunchuspinner = (Spinner) findViewById(R.id.cunchuTextSpinner);
        //将可选内容与ArrayAdapter连接起来
        fenbianadapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fenbianlv);
        cunchuadapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, cunchu);
        //设置下拉列表的风格
        fenbianadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cunchuadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //将adapter 添加到spinner中
        fenbianspinner.setAdapter(fenbianadapter);
        cunchuspinner.setAdapter(cunchuadapter);
        //添加事件Spinner事件监听
        fenbianspinner.setOnItemSelectedListener(new FenbiaSelectedListener());
        cunchuspinner.setOnItemSelectedListener(new CunchuSelectedListener());
        //设置默认值
        getcamerasetttingdata();
        fenbianspinner.setVisibility(View.VISIBLE);
        cunchuspinner.setVisibility(View.VISIBLE);
    }

    public void getcamerasetttingdata() {
        existSDCard();//判断SD是否移除
        SharedPreferences sharedata = getSharedPreferences("data", 0);
        String data = PreferencesUtils.getString(this, FBL_KEY, "480x640");// sharedata.getString("usbcamerafenbianlv", null);
        if (data.equalsIgnoreCase("480x640")) {
            fenbianspinner.setSelection(0, true);
        } else if (data.equalsIgnoreCase("768x1024")) {
            fenbianspinner.setSelection(1, true);
        } else if (data.equalsIgnoreCase("720x1280")) {
            fenbianspinner.setSelection(2, true);
        } else if (data.equalsIgnoreCase("1080x1920")) {
            fenbianspinner.setSelection(3, true);
        } else if (data.equalsIgnoreCase("1200x1600")) {
            fenbianspinner.setSelection(4, true);
        }
        SharedPreferences sharedata1 = getSharedPreferences("data", 0);
        String data1 = PreferencesUtils.getString(this, SDstorge_KEY, "话机");
        if (data1.equalsIgnoreCase("话机")) {
            cunchuspinner.setSelection(0, true);
        }
        if (data1.equalsIgnoreCase("SD卡")) {
            if (issdcardable == true) {
                Log.v(TAG, "SD存在");
                cunchuspinner.setSelection(1, true);
            } else {
                Log.v(TAG, "SD不存在");
                cunchuspinner.setSelection(0, true);
            }
        }
    }

    /****
     * 判断SD是否移除
     *
     */
    private void existSDCard() {
        if (getSdCardWriteableState(this, MainActivity.External_Storage).equals(
                android.os.Environment.MEDIA_MOUNTED)) {
            issdcardable = true;
        } else
            issdcardable = false;
    }

    /**
     *
     */
    class FenbiaSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                                   long arg3) {
            //view.setText("你的血型是："+m[arg2]);
            fenbianview.setText("分辨率：" + fenbianlv[arg2]);
            SharedPreferences.Editor sharedata = getSharedPreferences("data", 0).edit();
            sharedata.putString("usbcamerafenbianlv", fenbianlv[arg2]);
            sharedata.commit();
            PreferencesUtils.putString(SettingActivity.this, FBL_KEY, fenbianlv[arg2] + "");
        }

        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }

    class CunchuSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                                   long arg3) {

            cunchuview.setText("存储选择：" + cunchu[arg2]);
            SharedPreferences.Editor sharedata = getSharedPreferences("data", 0).edit();
            sharedata.putString("usbcameracunchuxuanzhe", cunchu[arg2]);
            sharedata.commit();
            PreferencesUtils.putString(SettingActivity.this, SDstorge_KEY, cunchu[arg2] + "");
            if ((issdcardable == false) && (arg2 == 1)) {
                Toast.makeText(SettingActivity.this, "无法使用SD卡", Toast.LENGTH_SHORT).show();
                cunchuview.setText("存储选择：" + cunchu[0]);
                cunchuspinner.setSelection(0, true);
            }
        }
        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }
}
