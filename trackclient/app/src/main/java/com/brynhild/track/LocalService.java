package com.brynhild.track;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;



/**
 * Created by ddd on 2017/8/3.
 */

public class LocalService extends Service
{
	private NotificationManager mNM;

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private int NOTIFICATION = 100;

	public static final String TAG = "LocalService";

	protected httpUploadThread mThreadUpload;


	// Log文件位置
	FileOutputStream m_logStream = null;


	// gps
	protected LocationManager m_locMan = null;
	protected LocatorController m_locationCon = null;

	String mstrIP = "";

	private String mStrPass = "service runnning";


	private Callback mCallback;

	public static final int KMSG_LOG_REC = 2;
	public static final int KMSG_UP_LOC = 3;





	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "onBind");
		return new Binder();
	}

	public class Binder extends android.os.Binder{
		public void setData(String data){
			LocalService.this.mStrPass = data;
		}
		public LocalService getLocalService(){
			return LocalService.this;
		}
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}


	public void setCallback(Callback callback) {
		this.mCallback = callback;
	}

	public Callback getCallback() {
		return mCallback;
	}

	public static interface Callback{
		void onDataChange(String data);
	}



	// 创建Log文件
	protected void createLogFile()
	{
		String sDir = "/sdcard/atrack";
		File destDir = new File(sDir);

		if (!destDir.exists())
		{
			destDir.mkdirs();
		}

		SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		String strFile = sDateFormat.format(new java.util.Date());
		String strRecordFile = sDir +"/" + strFile + ".txt";
		File saveFile = new File(strRecordFile);
		try
		{
			m_logStream = new FileOutputStream(saveFile);

		}
		catch (FileNotFoundException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	// 保存到log文件
	protected void recordLog(String str)
	{

		Log.d(TAG, str);

		if (mCallback != null){
			mCallback.onDataChange(str);
		}

		if(m_logStream != null)
		{
			try
			{
				SimpleDateFormat sDateFormat = new SimpleDateFormat("[yyyy-MM-dd HH-mm-ss] ");
				String strTime = sDateFormat.format(new java.util.Date());

				m_logStream.write(strTime.getBytes());
				m_logStream.write(str.getBytes());
				m_logStream.write("\r\n".getBytes());
				m_logStream.flush();

			}
			catch (FileNotFoundException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			catch (IOException e2)
			{
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		}
	}



	@Override
	public void onCreate() {
		super.onCreate();


		mThreadUpload = new httpUploadThread();
		mThreadUpload.start();

		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);


		createLogFile();


		// 设置GPS
		m_locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		m_locationCon = LocatorController.getInstance(m_locMan);


		Log.i(TAG, "onCreate");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		String strIp = intent.getStringExtra("ip");

		if(strIp == null)
		{
			// oncreate
			return super.onStartCommand(intent, flags, startId);

		}

		if(mstrIP.compareTo("") != 0)
		{
			// 第二次点击按钮
			return super.onStartCommand(intent, flags, startId);

		}

		mstrIP = strIp;

		showNotification();


		// 开始监听GPS

		boolean bUseExLocation = false;
		boolean bGPSOK = m_locationCon.addListener(bUseExLocation, mThreadUpload.mThreadHandler);

		if (!bGPSOK) {
			Toast.makeText(getApplicationContext(), "No GPS Provider",
					Toast.LENGTH_SHORT).show();
		}



		Log.i(TAG, "onStartCommand start id " + startId + ": " + intent + ":" + mstrIP);
		return super.onStartCommand(intent, flags, startId);
//		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		// Cancel the persistent notification.
		mNM.cancel(NOTIFICATION);

		super.onDestroy();

		Log.i(TAG, "onDestroy");
		// Tell the user we stopped.
//		Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
	}



	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the expanded notification
//		CharSequence text = getText(R.string.local_service_started);

		// The PendingIntent to launch our activity if the user selects this notification
		// 不必使用,会从oncreate进入
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, MainActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		Notification notification = new Notification.Builder(this)
				.setSmallIcon(R.drawable.notification_icon_background)  // the status icon
				.setTicker("start")  // the status text
				.setWhen(System.currentTimeMillis())  // the time stamp
				.setContentTitle("LocalService")  // the label of the entry
				.setContentText("MyContent")  // the contents of the entry
				.setContentIntent(null)  // The intent to send when the entry is clicked
				.build();

		// Send the notification.
		mNM.notify(NOTIFICATION, notification);


        // 使app在后台长期运行...
		startForeground(NOTIFICATION, notification);
		Log.d(TAG, "showNotification");
	}




	// 上传数据的线程
	class httpUploadThread extends Thread {


		public Handler mThreadHandler = null;



		@Override
		public void run() {
			Looper.prepare();

			// mThreadHandler 处理必须放在线程中
			mThreadHandler = new Handler()
			{
				@Override
				public void handleMessage(Message msg)
				{
					if (msg.what == KMSG_UP_LOC)
					{

						TrackRecord record = (TrackRecord) msg.obj;
						record.mySrverUrl =  mstrIP;

						// 从baidu获取poi名称
						Pair<String, String> pNames = getBaiduPOIName(record.lat, record.lon);

						record.next_station = pNames.first;
						record.poi = pNames.second;

						// 上传自己的服务器
						upload2MyServer(record);


						// 上传到baidu鹰眼服务器
						upload2BaiduTrack(record);

						Log.d("aa", "nnn 3 httpUploadThread KMSG_UP_LOC");

					}
					else if(msg.what == KMSG_LOG_REC)
					{
						String str = (String) msg.obj;
						recordLog(str);
					}


					msg = null;

				}
			};

			Looper.loop();
		}

		protected String buildMyServerUrl(TrackRecord record) {

			String strTime = "";
			String strName1 = "";
			String strName2 = "";

			try {
				// 中文必须转码
				// 不能有空格
				strTime = URLEncoder.encode(record.localtime, "UTF-8");
				strName1 = URLEncoder.encode(record.next_station, "UTF-8");
				strName2 = URLEncoder.encode(record.poi, "UTF-8");

			} catch (Exception e) {
				e.printStackTrace();

			}

			int iLat = (int) (record.lat * 1024.0 * 3600.0);
			int iLon = (int) (record.lon * 1024.0 * 3600.0);


			String strUrl = String.format("http://%s/test?route_id=%d&localtime=%s&lot=%d&lat=%d&alt=%.1f&speed=%.1f&head=%.1f&" +
							"accracy=%.1f&type=%d&seg_index=%d&next_station=%s&poi=%s",
					record.mySrverUrl,
					record.route_id,
					strTime,
					iLon,
					iLat,
					record.alt,
					record.speed,
					record.head,
					record.accracy,
					record.type,
					record.seg_index,
					strName1,
					strName2);

			return strUrl;

		}


		// 上传数据到自己的服务器
		protected boolean upload2MyServer(TrackRecord record) {

			String strUrl = buildMyServerUrl(record);

			boolean ret = false;

			try {
				// 1.声明访问的路径， url 网络资源 http ftp rtsp, 中文必须转码

				URL url = new URL(strUrl);

//				URL url = new URL("http://27.18.58.23:5000/show");

				// 2.通过路径得到一个连接 http的连接
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//				conn.setRequestMethod("GET");
				conn.setConnectTimeout(5000);
				conn.setReadTimeout(5000);

				// 3.判断服务器给我们返回的状态信息。
				// 200 成功 302 从定向 404资源没找到 5xx 服务器内部错误
				int code = conn.getResponseCode();
				if (code == 200) {

					recordLog("upload loc 2 myServer ok");
					ret = true;
				} else {
					// 请求失败
					recordLog("open server Connection faild " + Integer.toString(code) + " " + conn.getResponseMessage());
				}

				conn.disconnect();
			} catch (Exception e) {
				e.printStackTrace();

				recordLog("server error " + e.toString());
			}

			return ret;
		}

		// discard,  google api 位置不是总是准
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
					while (-1 != (len = is.read(buffer))) {
						baos.write(buffer, 0, len);
						baos.flush();
					}

					String strResult = baos.toString("utf-8");
					JSONObject jsonObject = new JSONObject(strResult);
					JSONArray arrays = (JSONArray) jsonObject.get("results");

					if (arrays.length() > 2) {
						strRouteName = arrays.getJSONObject(0).getString("formatted_address");
						strPOI = arrays.getJSONObject(1).getString("formatted_address");
					}


				} else {
					// 请求失败
					recordLog("open  maps.google.cn faild " + Integer.toString(code) + " " + conn.getResponseMessage());
				}

				conn.disconnect();
			} catch (Exception e) {
				e.printStackTrace();


				recordLog("maps.google.cn faild " + e.toString());
			}

			recordLog(strRouteName);
			recordLog(strPOI);


			return new Pair<String, String>(strRouteName, strPOI);
		}

		// 根据baidu api获取指定坐标的poi名称
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
					while (-1 != (len = is.read(buffer))) {
						baos.write(buffer, 0, len);
						baos.flush();
					}


					/*
					{"status":0,"result":{"location":{"lng":114.1247622038887,"lat":33.40761375639118},"formatted_address":"河南省驻马店市西平县","business":"","addressComponent":{"country":"中国","country_code":0,"province":"河南省","city":"驻马店市","district":"西平县","adcode":"411721","street":"","street_number":"","direction":"","distance":""},"pois":[],"roads":[],"poiRegions":[],"sematic_description":"李湾南541米","cityCode":269}}
					* */

					String strResult = baos.toString("utf-8");
					JSONObject jsonObject = new JSONObject(strResult);
					int status = jsonObject.getInt("status");

					if (status == 0) {
						JSONObject result = jsonObject.getJSONObject("result");
						strRouteName = result.getString("formatted_address");
						strPOI = result.getString("sematic_description");


					}


				} else {
					// 请求失败
					recordLog("open api.map.baidu.com faild " + Integer.toString(code) + " " + conn.getResponseMessage());
				}

				conn.disconnect();
			} catch (Exception e) {
				e.printStackTrace();


				recordLog("api.map.baidu.com faild " + e.toString());
			}

			recordLog(strRouteName);
			recordLog(strPOI);


			return new Pair<String, String>(strRouteName, strPOI);
		}


		// 上传数据到百度鹰眼 http://lbsyun.baidu.com/index.php?title=yingyan/api/v3/trackupload
		// post data:   ak=96fd36a93d2f61e5cb517a6485b7ec8e&service_id=147154&entity_name=雄楚线&latitude=30.40915147569&longitude=114.442293565&
		// loc_time=1501462090&coord_type_input=wgs84&speed=4&direction=345&height=22&radius=17
		protected boolean upload2BaiduTrack(TrackRecord record) {

			boolean ret = false;

			String strUrl = "http://yingyan.baidu.com/api/v3/track/addpoint";

			try {

				String strEntity = URLEncoder.encode(record.entity, "UTF-8");

				String strPost = String.format("ak=96fd36a93d2f61e5cb517a6485b7ec8e&service_id=147154&entity_name=%s" +
								"&latitude=%f&longitude=%f&loc_time=%d&coord_type_input=wgs84&speed=%.1f&direction=%d&height=%.1f&radius=%.1f",
						strEntity,
						record.lat,
						record.lon,
						record.timestamps,
						record.speed,
						(int) record.head,
						record.alt,
						record.accracy);

				URL url = new URL(strUrl);

				// 2.通过路径得到一个连接 http的连接
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();

				conn.setRequestMethod("POST");
				conn.setConnectTimeout(5000);
				conn.setReadTimeout(5000);

				// 发送POST请求必须设置如下两行
				conn.setDoOutput(true);
				conn.setDoInput(true);
				// 获取URLConnection对象对应的输出流
				PrintWriter printWriter = new PrintWriter(conn.getOutputStream());
				// 发送请求参数
				printWriter.write(strPost);//post的参数 xx=xx&yy=yy
				// flush输出流的缓冲
				printWriter.flush();

				//开始获取数据
				BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				int len;
				byte[] arr = new byte[1024];
				while ((len = bis.read(arr)) != -1) {
					bos.write(arr, 0, len);
					bos.flush();
				}
				bos.close();
				String strResult = bos.toString("utf-8");

				JSONObject jsonObject = new JSONObject(strResult);
				int status = jsonObject.getInt("status");

				if (status == 0) {
					// ok
					ret = true;
					recordLog("upload loc to BaiduTrack ok");


				} else {
					// 上传失败
					recordLog("upload loc to BaiduTrack faild " + strResult + " " + conn.getResponseMessage());

				}

			} catch (Exception e) {
				e.printStackTrace();

				recordLog("upload loc to BaiduTrack faild " + e.toString());
			}

			return ret;

		}
	}

}
