# -*- coding: utf-8 -*-

__author__ = 'dddd'

import sqlite3

DB_NAME = 'rd.dat'

def createTable():


    conn = sqlite3.connect(DB_NAME)
    # try:
    create_tb_cmd='''
    CREATE TABLE IF NOT EXISTS Location
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

    );
    '''
    #主要就是上面的语句
    conn.execute(create_tb_cmd)
    # except:
    #     print 'Create table failed'
    #     return False
    conn.close()


def saveRecord(record ):

    # record = {'route_id': 0,
    #           'localtime':'2014-10-1 22:12:13',
    #            'lot':1111111,
    #           'lat':22222,
    #           'alt':11,
    #           'speed':222 ,
    #           'head':333.9,
    #           'accracy':55.9,
    #             'type':0,
    #             'seg_index': 0,
    #             'next_station': u'啊啊啊啊',
    #             'poi': u'呵呵'
    #           }




    conn=sqlite3.connect(DB_NAME)
    insert_dt_cmd='INSERT INTO Location (route_id, [localtime],lot, lat,alt,speed,head, accracy, type, seg_index, next_station, poi) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) '


    conn.execute(insert_dt_cmd,
                 (record['route_id'], record['localtime'], record['lot'], record['lat'],  record['alt'], record['speed'],
                  record['head'],record['accracy'],record['type'],record['seg_index'],  record['next_station'] ,record['poi'])
    )

    conn.commit()
    conn.close()


def showContents(num):
    conn = sqlite3.connect(DB_NAME)

    select_cmd = 'select * from Location order by rd_id desc limit 0, ' + str(num)
    cursor = conn.execute(select_cmd)



    strCSV  =  u'<table border="1">'
    initLen = len(strCSV)


    for row in cursor:

        #   取列名
        if len(strCSV) == initLen:

            strCSV += u'<tr>'

            for col in cursor.description:
                strCSV += u'<th>'
                strCSV += col[0]
                strCSV += u'</th>'
            strCSV += u'</tr>'


        strCSV += u'<tr>'
        for rec in row:
            strCSV += u'<td>'
            strCSV +=  (unicode(rec))
            strCSV += u'</td>'




        strCSV += u'</tr>'
        # print strCSV
        # strHead += '<br>'

    conn.close()

    return  strCSV


