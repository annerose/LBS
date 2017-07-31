package com.brynhild.track;

import android.content.Context;

import android.location.LocationManager;
import android.net.Uri;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;
import android.os.Handler;
import android.os.Message;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayDeque;



public class MainActivity extends AppCompatActivity implements OnClickListener {

	protected Button mBtnStart;
	protected EditText mEditIP;
	protected EditText mEditMsg;

	// gps
	protected LocationManager m_locMan = null;
	protected LocatorController m_locationCon = null;

	public static Handler m_Handler = null;

	public static final int KMSG_INIT_GPS = 1;
	public static final int KMSG_LOG_GPS = 2;
	public static final int KMSG_UP_URL = 3;
	public static final int KMSG_UP_LOC = 4;


	protected httpUploadThread mThreadUpload;


	protected ArrayDeque<String> mArrLog = new ArrayDeque<String>();


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

		mThreadUpload = new httpUploadThread();
		mThreadUpload.start();


		// 消息响应
		m_Handler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
					case KMSG_INIT_GPS: {
						// 移到start 按钮中


					}
					break;
					case KMSG_LOG_GPS: {
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
					case KMSG_UP_URL: {
						String str = (String) msg.obj;
						String strUrl = "http://" + mEditIP.getText();
						strUrl += "/test?";
						strUrl += str;

						Log.d("aaa", "nnn 22 KMSG_UP_URL ...");

						mThreadUpload.mThreadHandler.obtainMessage(KMSG_UP_LOC, msg.arg1, msg.arg2, strUrl).sendToTarget();


						break;
					}
					default:
						break;
				}

				msg = null;
			}
		};


		// 设置GPS
		m_locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		m_locationCon = LocatorController.getInstance(m_locMan);


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

	protected void onStartButtonClick() {

		// 开始监听GPS

		boolean bUseExLocation = false;
		boolean bGPSOK = m_locationCon.addListener(bUseExLocation);

		if (!bGPSOK) {
			Toast.makeText(getApplicationContext(), "No GPS Provider",
					Toast.LENGTH_SHORT).show();
		}
	}


	// 上传数据的线程
	class httpUploadThread extends Thread {
		public Handler mThreadHandler;

		@Override
		public void run() {
			Looper.prepare();
			mThreadHandler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					if (msg.what == KMSG_UP_LOC) {

						Pair<String, String> pNames = getBaiduPOIName(msg.arg1 / (1024 * 3600.0), msg.arg2 / (1024 * 3600.0));

						String strUrl = (String) msg.obj;

						try
						{
							// 中文必须转码 不能有空格

							strUrl = String.format("%s&next_station=%s&poi=%s",
									strUrl,
									URLEncoder.encode(pNames.first,  "UTF-8"),
									URLEncoder.encode(pNames.second,  "UTF-8"));
						}
						catch (Exception e) {
							e.printStackTrace();

						}

						connectUrl(strUrl);

						Log.d("aa","nnn 3 httpUploadThread KMSG_UP_LOC" );

					}

					msg = null;

				}
			};
			Looper.loop();
		}


		protected void connectUrl(String path) {
			// 通过http请求把图片获取下来。
			try {
				// 1.声明访问的路径， url 网络资源 http ftp rtsp, 中文必须转码

				URL url = new URL(path);

//				URL url = new URL("http://27.18.58.23:5000/show");

				// 2.通过路径得到一个连接 http的连接
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//				conn.setRequestMethod("GET");
				conn.setConnectTimeout(5000);
				conn.setReadTimeout(5000);

//				conn.setRequestProperty("Accept-Charset", "utf-8");
//				conn.setRequestProperty("Content-Type", "text/html; charset=utf-8");
//				conn.setDoOutput(true);

//				conn.setRequestProperty("Accept",
//						"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"); // 设置内容类型
//
//				conn.setRequestProperty("Accept-Encoding",
//						"gzip, deflate, sdch"); // 设置内容类型
//
//				conn.setRequestProperty("Accept-Language",
//						"zh-CN,zh;q=0.8"); // 设置内容类型
//
//				conn.setRequestProperty("User-Agent",
//						"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.116 Safari/537.36"); // 设置内容类型
//

				// 3.判断服务器给我们返回的状态信息。
				// 200 成功 302 从定向 404资源没找到 5xx 服务器内部错误
				int code = conn.getResponseCode();
				if (code == 200) {
					// 4.利用链接成功的 conn 得到输入流
					//InputStream is = conn.getInputStream();// png的图片

					// 5. ImageView设置Bitmap,用BitMap工厂解析输入流

					//mIvShow.setImageBitmap(BitmapFactory.decodeStream(is));

					m_Handler.obtainMessage(KMSG_LOG_GPS, "upload loc ok").sendToTarget();
				} else {
					// 请求失败
					//Toast.makeText(this, "请求失败", Toast.LENGTH_SHORT).show();
					m_Handler.obtainMessage(KMSG_LOG_GPS, "open server Connection faild " + Integer.toString(code) + " " + conn.getResponseMessage()).sendToTarget();
				}

				conn.disconnect();
			} catch (Exception e) {
				e.printStackTrace();
				//Toast.makeText(this, "发生异常，请求失败", Toast.LENGTH_SHORT).show();
				m_Handler.obtainMessage(KMSG_LOG_GPS, "server error " + e.toString()).sendToTarget();
			}
		}

		//  http://maps.google.cn/maps/api/geocode/json?latlng=114.442612,30.409053&sensor=true&language=zh-CN
		protected Pair<String, String> getGooglePOIName(double lat, double lng) {

			String strRouteName = "";
			String strPOI = "";
			try {
				// 1.声明访问的路径， url 网络资源 http ftp rtsp, 中文必须转码
				String strUrl = String.format("http://maps.google.cn/maps/api/geocode/json?latlng=%f,%f&sensor=true&language=zh-CN",
						lat, lng);
				URL url = new URL(strUrl);

				// 2.通过路径得到一个连接 http的连接
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();

				conn.setConnectTimeout(3000);
				conn.setReadTimeout(1000);



				// 3.判断服务器给我们返回的状态信息。
				// 200 成功 302 从定向 404资源没找到 5xx 服务器内部错误
				int code = conn.getResponseCode();
				if (code == 200) {
					// 4.利用链接成功的 conn 得到输入流
					InputStream is = conn.getInputStream();// png的图片

					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					byte[] buffer = new byte[2048];
					int len = 0;
					while(-1 != (len = is.read(buffer))){
						baos.write(buffer,0,len);
						baos.flush();
					}

					String strResult = baos.toString("utf-8");
					JSONObject jsonObject = new JSONObject(strResult);
					JSONArray arrays = (JSONArray) jsonObject.get("results");

					if(arrays.length() > 2)
					{
						strRouteName = arrays.getJSONObject(0).getString("formatted_address");
						strPOI = arrays.getJSONObject(1).getString("formatted_address");
					}



				} else {
					// 请求失败
					m_Handler.obtainMessage(KMSG_LOG_GPS, "openC maps.google.cn faild " + Integer.toString(code) + " " + conn.getResponseMessage()).sendToTarget();
 				}

				conn.disconnect();
			} catch (Exception e) {
				e.printStackTrace();


				m_Handler.obtainMessage(KMSG_LOG_GPS, "maps.google.cn faild " + e.toString()).sendToTarget();
			}

			m_Handler.obtainMessage(KMSG_LOG_GPS, strRouteName).sendToTarget();
			m_Handler.obtainMessage(KMSG_LOG_GPS, strPOI).sendToTarget();


			return    new Pair<String, String> (strRouteName, strPOI);
		}


		//  http://api.map.baidu.com/geocoder/v2/?coordtype=wgs84ll&location=33.4027019,114.112351888&output=json&pois=0&ak=96fd36a93d2f61e5cb517a6485b7ec8e
		protected Pair<String, String> getBaiduPOIName(double lat, double lng) {

			String strRouteName = "";
			String strPOI = "";
			try {
				// 1.声明访问的路径， url 网络资源 http ftp rtsp, 中文必须转码
				String strUrl = String.format("http://api.map.baidu.com/geocoder/v2/?coordtype=wgs84ll&location=%f,%f&output=json&pois=0&ak=96fd36a93d2f61e5cb517a6485b7ec8e",
						lat, lng);
				URL url = new URL(strUrl);

				// 2.通过路径得到一个连接 http的连接
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();

				conn.setConnectTimeout(5000);
				conn.setReadTimeout(5000);



				// 3.判断服务器给我们返回的状态信息。
				// 200 成功 302 从定向 404资源没找到 5xx 服务器内部错误
				int code = conn.getResponseCode();
				if (code == 200) {
					// 4.利用链接成功的 conn 得到输入流
					InputStream is = conn.getInputStream();// png的图片

					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					byte[] buffer = new byte[2048];
					int len = 0;
					while(-1 != (len = is.read(buffer))){
						baos.write(buffer,0,len);
						baos.flush();
					}


					/*
					{"status":0,"result":{"location":{"lng":114.1247622038887,"lat":33.40761375639118},"formatted_address":"河南省驻马店市西平县","business":"","addressComponent":{"country":"中国","country_code":0,"province":"河南省","city":"驻马店市","district":"西平县","adcode":"411721","street":"","street_number":"","direction":"","distance":""},"pois":[],"roads":[],"poiRegions":[],"sematic_description":"李湾南541米","cityCode":269}}
					* */

					String strResult = baos.toString("utf-8");
					JSONObject jsonObject = new JSONObject(strResult);
					int status  = jsonObject.getInt("status");

					if(status == 0)
					{
						JSONObject result = jsonObject.getJSONObject("result");
						strRouteName = result.getString("formatted_address");
						strPOI = result.getString("sematic_description");



					}





				} else {
					// 请求失败
					m_Handler.obtainMessage(KMSG_LOG_GPS, "open api.map.baidu.com faild " + Integer.toString(code) + " " + conn.getResponseMessage()).sendToTarget();
				}

				conn.disconnect();
			} catch (Exception e) {
				e.printStackTrace();


				m_Handler.obtainMessage(KMSG_LOG_GPS, "api.map.baidu.com faild " + e.toString()).sendToTarget();
			}

			m_Handler.obtainMessage(KMSG_LOG_GPS, strRouteName).sendToTarget();
			m_Handler.obtainMessage(KMSG_LOG_GPS, strPOI).sendToTarget();


			return    new Pair<String, String> (strRouteName, strPOI);
		}
	}


}
