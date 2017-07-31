package com.brynhild.track;


import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import android.content.Intent;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;


import static com.brynhild.track.MainActivity.KMSG_LOG_GPS;
import static com.brynhild.track.MainActivity.m_Handler;


public class LocatorController
{
	public static final String TAG = "LocatorController";
	
	protected LocationManager m_locMan = null;
	private volatile boolean m_locstatus = false;
	
	// 全局唯一instance
	private static LocatorController m_instance = null;
	
	
	String m_strProvider = null;
	
	boolean m_bAddListener = true;
	
	// 最后一次获取有效的GPS时刻
	long m_lGetGPSTicks  = 0;
	
	// 程序启动后同时监听network和gps
	// GPS 有效的时间间隔, 超过此间隔则使用Network定位
	public static final int MAX_GPS_VALID_TICKS= 60000;

	// 最小间隔距离500m
	public static final float MIN_DISTANCE = 100.0f;

	// 最小间隔时间30s
	public static final int MIN_TIME = 2000;

//
//	public static final float MIN_DISTANCE = 0.0f;
//	//	// 最小间隔时间30s
//	public static final int MIN_TIME = 1 * 1000;

	// Log文件位置	
	FileOutputStream m_logStream = null;

	
	// 使用外部的定位服务
	boolean m_bUseExLocation = true;
	
	
	// 发送Log
	private void addMessageLog(String strMsg)
	{

		Log.d(TAG, strMsg);
		m_Handler.obtainMessage(KMSG_LOG_GPS, strMsg).sendToTarget();
 		

	}
	
	private LocatorController(LocationManager locMan) {
		this.m_locMan = locMan;
        
		init();
	}
	
	/**
	 * 返回唯一实例
	 * @param locMan
	 * @return
	 */
	public static LocatorController getInstance(LocationManager locMan)
	{
		if(m_instance == null)
		{
			m_instance = new LocatorController(locMan);
		}
		return m_instance;
	}
	
	/**
	 * 返回实例
	 * @return
	 */
	public static LocatorController getInstance()
	{
		return m_instance;
	}
	
	
	private void init()
	{
		Criteria criteria = new Criteria();
		//criteria.setAccuracy(Criteria.ACCURACY_COARSE); // 把精确度设置为模糊，所有手机跑起来了
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(true);
        criteria.setBearingRequired(true);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        criteria.setSpeedRequired(true);

        // 加上这句就挂
//        m_locMan.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
        
        
        // BestProvider 并没有什么意义，network和gps都要获取...
        m_strProvider = m_locMan.getBestProvider(criteria, true);
        
        
//        mGpsEphemeris = new  JNGPSEPHEMERISINFO();

        
	}
	
	private void openGPSSetting() 
	{
		if (m_locMan.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) 
		{
			//Toast.makeText(this, "GPS模块正常", Toast.LENGTH_SHORT).show();
			return;
		}

	//	Toast.makeText(this, "请开启GPS！", Toast.LENGTH_SHORT).show();
		
		// 跳转到GPS的设置页面
//		Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//		startActivityForResult(intent, 0); // 此为设置完成后返回到获取界面
	}
	
	
    public boolean addListener(boolean bUseExLocation )
    {
    	
    	m_bUseExLocation = bUseExLocation;
    	
    	   


		
		

    	//获取系统所有的LocationProvider名称  
    	List<String> list = m_locMan.getAllProviders();
        String strAllProviders = "getAllProviders : ";  
        for (int i = 0; i < list.size(); i++) 
        {  
        	strAllProviders += list.get(i);  
        	strAllProviders += ", ";
        }  
        
        addMessageLog(strAllProviders);
        
 
    	// 设置GPS的监听
    	//	java.lang.IllegalArgumentException: provider=gps
    	try
    	{
        
	        m_locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE,locationGPSListener);
	    	if(!m_locMan.addGpsStatusListener(statusListener))
			{
	    		m_bAddListener = false;
	    		addMessageLog( "GpsStatusListener created false");
			
				return false;
			}
	    	
	    	
	    	addMessageLog("GPS_PROVIDER OK");
    	}
    	catch(Exception e)
    	{
    		m_bAddListener = false;
    		addMessageLog(Log.getStackTraceString(e));
    	}
    	
    	
    	if(!m_bUseExLocation)
    	{

		    // 不获取LastProvider,避免混乱
//	    	String strLastProvider = LocationManager.GPS_PROVIDER;
//	    	// 获取上次位置
//	    	Location lastlocate = m_locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//	    	if(lastlocate == null)
//	    	{
//	    		lastlocate = m_locMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//	    		strLastProvider = LocationManager.NETWORK_PROVIDER;
//	    	}
//
//	    	if(lastlocate == null)
//	    	{
//	    		lastlocate = m_locMan.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
//	    		strLastProvider = LocationManager.PASSIVE_PROVIDER;
//	    	}
//
//
//	    	if(lastlocate != null && locationNetworkListener != null)
//	    	{
//
//	    		addMessageLog("lastlocate " + strLastProvider + ":" + lastlocate.getLongitude() + "," + lastlocate.getLatitude() + ",Accuracy:" + lastlocate.getAccuracy() + ", Provider:"  + lastlocate.getProvider());
//	    		locationNetworkListener.onLocationChanged(lastlocate);
//	    	}
//	    	else
//	    	{
//	    		addMessageLog("lastlocate is null");
//	    	}
//
	
	    	// 设置network 的监听
	    	try
	    	{
	          
		        m_locMan.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE,locationNetworkListener );
		        
		        addMessageLog("NETWORK_PROVIDER requestLocationUpdates");
		        
	//	        Log.d(TAG, "NETWORK_PROVIDER requestLocationUpdates");
	    	}
	    	catch(Exception e)
	    	{
	    	//	m_bAddListener = false;
	    		addMessageLog(Log.getStackTraceString(e));
	    	}
    	}
    	
    	// 除了三星P1000 其他手机似乎都不OK
    	// 这个接口的此方法貌似是预留的一种规范，曾跟踪源码，发现在硬件访问层补充nmea_callback的代码，才能将数据传到应用上层
//	   if(!m_locMan.addNmeaListener(nmealistener)) {
//        	Log.d(TAG, "NmeaListener created false");
//        	return false;
//        } 
	   
    	m_bAddListener = true;
    	
        return m_bAddListener;
    }
	
    public void removeListener()
    {
    	if(m_bAddListener)
    	{
	    	m_locMan.removeUpdates(locationGPSListener);
	    	m_locMan.removeUpdates(locationNetworkListener);
	    	m_locMan.removeGpsStatusListener(statusListener);
	    	addMessageLog("remove gps Network Listener");
	    	
	    	m_bAddListener = false;
    	}
    	
    	//m_locMan.setTestProviderEnabled("gps", false);
    	//m_locMan.removeNmeaListener(nmealistener);
    	
    }
    
    
    private final LocationListener locationNetworkListener  = new LocationListener() {

        public void onLocationChanged(Location location) {
        	

              
        	// 仍处于GPS有效时间段，不使用network定位
        	if( System.currentTimeMillis() - m_lGetGPSTicks < MAX_GPS_VALID_TICKS)
        	{
        		return;
        	}

			// 有gps可以用
			if(m_locstatus)
			{
				return;
			}
        	
        	
         	String buff = convertLocation2String(location);

        	
            addMessageLog("Provider " + location.getProvider() +  ": onLocationChanged pos :" + location.getLongitude() + "," + location.getLatitude() + ",Accuracy:" + location.getAccuracy());
            
        }

        public void onProviderEnabled(String provider) {
        	addMessageLog("onProviderEnabled Network provider = " + provider);
        }

        public void onProviderDisabled(String provider) {
        	addMessageLog( "onProviderDisabled Network provider = " + provider);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        	
        	// 这个似乎从未进来过，不可靠的说 m_locstatus默认true
        	addMessageLog( "onStatusChanged Network provider : " + provider + " status changed " + status); 
          	
          	String STATUS_NAMES[] = {" OUT_OF_SERVICE", "TEMPORARILY_UNAVAILABLE", "AVAILABLE"};  
          	
          	if(status < STATUS_NAMES.length)
          	{
          		 
          		addMessageLog("onStatusChanged Network : " + STATUS_NAMES[status] ); 
          	}
          
            if(status == LocationProvider.AVAILABLE)
            {
            	m_locstatus = true;
            }
            else{
            	m_locstatus = false;
            }
        }
     };


	// route_id=0&
	// localtime=2017-07-04%2014:37:33.200&
	// lot=429486505&
	// lat=148252177&
	// alt=55.1&
	// speed=44&
	// head=0&
	// accracy=10.1&
	// type=1&
	// seg_index=13&
	// next_station=%E5%8D%8E%E5%A4%8F%E5%AD%A6%E9%99%A2&
	// poi=%E5%85%B3%E5%B1%B1%E5%A4%A7%E9%81%93
     
     // 把Location 序列化
     String convertLocation2String(Location location)
     {
    	 int routeid = 0;

	     int seg_index = 2;


	     Date date = new Date(location.getTime());
	     SimpleDateFormat myFmt  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	     String strTime = myFmt.format(date);



	     int iType = 0;
	     String strProvider = location.getProvider();
	     if(strProvider != null && strProvider.length() > 0)
	     {
		     iType = (byte)(strProvider.charAt(0));
	     }


	     try
	     {
		     // 中文必须转码
		     // 不能有空格
		     strTime  = URLEncoder.encode(strTime, "UTF-8");
//		     strStation = URLEncoder.encode(strStation, "UTF-8");
//		     strPoi = URLEncoder.encode(strPoi, "UTF-8");
	     }
	     catch (Exception e) {
		     e.printStackTrace();

	     }

	     int iLat =  (int)(location.getLatitude() * 1024.0 * 3600.0);
	     int iLon =  (int)(location.getLongitude() * 1024.0 * 3600.0);



	     String line = String.format("route_id=%d&localtime=%s&lot=%d&lat=%d&alt=%.1f&speed=%.1f&head=%.1f&" +
			     "accracy=%.1f&type=%d&seg_index=%d" ,
			     routeid, strTime,
			     iLon,
			     iLat,
			     location.getAltitude(),
			     location.getSpeed() * 3.6f,  // m/s -> km/h
			     location.getBearing(),
			     location.getAccuracy(),
			     iType,
			     seg_index);

	     Log.d(TAG, "nnn 1 convertLocation2String ...");

	     m_Handler.obtainMessage(MainActivity.KMSG_UP_URL, iLat, iLon,   line).sendToTarget();

        return line;
              
     }
     
    
	   private final LocationListener locationGPSListener = new LocationListener() {

	        public void onLocationChanged(Location location) {
	        	
	        	if(m_bUseExLocation)
	        	{
	        		return;
	        	}
	        	
	        	
	        	String buff = convertLocation2String(location);

	        	
	            // 获取有效的GPS信息的时间
	        	m_lGetGPSTicks = System.currentTimeMillis();
	            addMessageLog("Provider " + location.getProvider() +  ": onLocationChanged pos :" + location.getLongitude() + "," + location.getLatitude() + ",Accuracy:" + location.getAccuracy());
	              
	        }

	        public void onProviderEnabled(String provider) {
	        	addMessageLog( "GPS onProviderEnabled provider = " + provider);
	        }

	        public void onProviderDisabled(String provider) {
	        	addMessageLog("GPS onProviderDisabled provider = " + provider);
	        }

	        public void onStatusChanged(String provider, int status, Bundle extras) {
	        	
	        	// 这个似乎从未进来过，不可靠的说 m_locstatus默认true
//	        	addMessageLog("GPS onStatusChanged provider : " + provider + " status changed " + status);
	          	
	          	String STATUS_NAMES[] = {" OUT_OF_SERVICE", "TEMPORARILY_UNAVAILABLE", "AVAILABLE"};  
	          	
	          	if(status < STATUS_NAMES.length)
	          	{
	          		addMessageLog( "GPS onStatusChanged : " + STATUS_NAMES[status] ); 
	          	}
	          
	            if(status == LocationProvider.AVAILABLE)
	            {
	            	m_locstatus = true;
	            }
	            else{
	            	m_locstatus = false;
	            }
	        }
	     };
	
	     private final GpsStatus.Listener statusListener = new GpsStatus.Listener() {

		        public void onGpsStatusChanged(int event) {
		        	
		        	String ARR_NAME [] = {"", "GPS_EVENT_STARTED", "GPS_EVENT_STOPPED", "GPS_EVENT_FIRST_FIX", "GPS_EVENT_SATELLITE_STATUS"};
		        	
		        	if(event < ARR_NAME.length)
		        	{
		        		if(event != GpsStatus.GPS_EVENT_SATELLITE_STATUS)
		        		{
		        			// 避免Log过多
		        			addMessageLog("onGpsStatusChanged changed " + ARR_NAME[event]);
		        		}
		        	}
		        	

		             
		             GpsStatus status = m_locMan.getGpsStatus(null); // 取当前状态
		             switch(event)
		             {
		             case GpsStatus.GPS_EVENT_STARTED:
		            	 break;
		             case GpsStatus.GPS_EVENT_STOPPED:
		            	 break;
		             case GpsStatus.GPS_EVENT_FIRST_FIX:
		            	 status.getTimeToFirstFix();
		            	 break;
		             case GpsStatus.GPS_EVENT_SATELLITE_STATUS:


//		            		SetGpsSatellite(status.getSatellites());
//	            		addMessageLog("getMaxSatellites = " + status.getMaxSatellites());
		            	 
 
		            	 break;
		             }

		        } // GPS状态变化时的回调，如卫星数，信号强度等
			};

	private void SetGpsSatellite(Iterable<GpsSatellite> satellites)
	{


		short usSatCount = 0;
		short usSatCalcCount = 0;


		for (Iterator it = satellites.iterator(); it.hasNext(); )
		{
			GpsSatellite sat = (GpsSatellite)it.next();

//				 String strMsg = String.format("SAT[%d] Azimuth(%.2f), Elevation(%.2f), Prn(%d), Snr(%.2f), hasEphemeris(%b), hasAlmanac(%b), usedInFix (%b)",
//						 usSatCount, sat.getAzimuth(), sat.getElevation(), sat.getPrn(), sat.getSnr(),
//							 sat.hasEphemeris(), sat.hasAlmanac(),  sat.usedInFix());
//
//				 addMessageLog(strMsg);

//			mGpsEphemeris.AddStateInfo((short)sat.getAzimuth(), (short)sat.getElevation(), (short)sat.getSnr(),
//					sat.usedInFix(), (byte)sat.getPrn());

			usSatCount++;

			if(sat.usedInFix())
			{
				usSatCalcCount++;
			}
		}

//		mGpsEphemeris.usTotalStatCnts = usSatCount;
//		mGpsEphemeris.usCaclStateCns = usSatCalcCount;


		addMessageLog(String.format("satellites used count = %d / %d", usSatCalcCount, usSatCount));


	}



}
