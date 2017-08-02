package com.brynhild.track;

/**
 * Created by hehe on 2017/8/1.
 */

/*
    ([rd_id] integer PRIMARY KEY AUTOINCREMENT,
    [route_id] int,
    [localtime] datetime,
    [lot] int,
    [lat] int,
    [alt] real,
    [speed] real,
    [head] real,
    [accracy] real,
    [type] int,
    [seg_index] int,
    [next_station] varchar(32),
    [poi] varchar(32)

*/


public class TrackRecord {

	public int route_id;
	public long timestamps;
	public String localtime;
	public double lon;
	public double lat;
	public double alt;
	public double speed;
	public double head;
	public double accracy;
	public int type;
	public int seg_index;
	public String next_station;
	public String poi;
	public String mySrverUrl;
	public String entity;


}
