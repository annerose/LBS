package com.brynhild.track;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;

import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;

import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.EditText;

import android.os.Handler;
import android.os.Message;

import java.io.*;


import java.util.ArrayDeque;

/*
MIUI 后台5分钟就被关闭了

必须单独设置神隐模式，把Track App设为无作用，不使用手机系统的后台省电策略

[2017-08-02]

 */



public class MainActivity extends AppCompatActivity implements OnClickListener, ServiceConnection {

	protected Button mBtnStart;
	protected EditText mEditIP;
	protected EditText mEditMsg;

	protected static Handler m_Handler = null;

	public static final int KMSG_INIT_GPS = 1;
	public static final int KMSG_LOG_SHOW = 2;


	public static final String CACHE_FILE_NAME ="ip.txt";

	protected ArrayDeque<String> mArrLog = new ArrayDeque<String>();
	private LocalService.Binder myBinder = null;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);


		mBtnStart = (Button) findViewById(R.id.buttonStart);
		mEditIP = (EditText) findViewById(R.id.editTextIP);
		mEditMsg = (EditText) findViewById(R.id.editTextMsg);
		mBtnStart.setOnClickListener(this);

		// 禁止编辑
		mEditMsg.setKeyListener(null);

		// 载入上次的ip
		String strLastIP = loadLastIP();
		if(!strLastIP.isEmpty())
		{
			mEditIP.setText(strLastIP);
		}


		// 消息响应
		m_Handler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
					case KMSG_INIT_GPS: {
						// 移到start 按钮中


					}
					break;
					case KMSG_LOG_SHOW: {
						String str = (String) msg.obj;


						mArrLog.addFirst(str);
						int max_size = 100;

						if (mArrLog.size() > max_size) {
							for (int i = 0; i < max_size / 2; i++) {
								mArrLog.removeLast();
							}
						}

						StringBuilder sb = new StringBuilder();
						for (String strLine : mArrLog) {
							sb.append(strLine);
							sb.append("\n");
						}

						mEditMsg.setText(sb.toString());




					}
					break;

					default:
						break;
				}

				msg = null;
			}
		};




		// 为了能在后台长期运行，使用service
		Intent startIntent = new Intent(this, LocalService.class);
		startService(startIntent);
		bindService(startIntent, this, Context.BIND_AUTO_CREATE);



		// 延时1s启动
//		m_Handler.sendEmptyMessageDelayed(KMSG_INIT_GPS, 1000);

	}

//	@Override
//	protected void onDestroy() {
//		super.onDestroy();
// 	    m_locationCon.removeListener();
//
//	}

	//实现OnClickListener接口中的方法
	@Override
	public void onClick(View v) {

		switch (v.getId()) {
			case R.id.buttonStart:
				onStartButtonClick();
				break;

			default:
				break;

		}

	}

	protected void saveLastIP(String strIp)
	{
		FileOutputStream outputStream;

		try{
			outputStream = openFileOutput(CACHE_FILE_NAME, Context.MODE_PRIVATE);
			outputStream.write(strIp.getBytes());
			outputStream.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	protected String loadLastIP()
	{

		FileInputStream inputStream;
		byte[] buffer = null;
		try {
			inputStream = openFileInput(CACHE_FILE_NAME);
			try {
				// 获取文件内容长度
				int fileLen = inputStream.available();
				// 读取内容到buffer
				buffer = new byte[fileLen];
				inputStream.read(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		// 返回文本信息
		if (buffer != null)
			return  new String(buffer);
		else
			return "";

	}

	protected void onStartButtonClick() {

		// 要申请的权限 数组 可以同时申请多个权限
		String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

		// 如果超过6.0才需要动态权限，否则不需要动态权限
		if (Build.VERSION.SDK_INT >= 23)
		{
			int check = ContextCompat.checkSelfPermission(this, permissions[0]);
			if (check != PackageManager.PERMISSION_GRANTED)
			{
				requestPermissions(permissions, 1);
				return;
			}
		}

		String strIP = mEditIP.getText().toString();

		saveLastIP(strIP);

		// 启动service
		Intent startIntent = new Intent(this, LocalService.class);

		// 传送ip地址
		startIntent.putExtra("ip", mEditIP.getText().toString() );
		startService(startIntent);
	}



	//一旦绑定成功就会执行该函数
	@Override
	public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
		myBinder = (LocalService.Binder) iBinder;
		myBinder.getLocalService().setCallback(new LocalService.Callback(){
			@Override
			public void onDataChange(String data) {

				m_Handler.obtainMessage(KMSG_LOG_SHOW, data).sendToTarget();
			}
		});
	}

	@Override
	public void onServiceDisconnected(ComponentName componentName) {

	}




}
