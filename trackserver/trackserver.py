##################################!/usr/bin/python
# for python3.6             2018-3-14
# -*- coding: utf-8 -*-

__author__ = 'dddd'


from flask import Flask, request
import db

from gevent.wsgi import WSGIServer





app = Flask(__name__)
#创建一个名为app的Flask对象

@app.route('/')
#当有人访问网页服务器的根目录是，执行下面的代码
def hello():
    return 'Hello World!'
#向客户端发送“Hello WOrld!”字符串

# http://127.0.0.1:5000/test?route_id=0&localtime=2017-07-04%2014:37:33.200&lot=429486505&lat=148252177&alt=55.1&speed=44&head=0&accracy=10.1&type=1&seg_index=13&next_station=%E5%8D%8E%E5%A4%8F%E5%AD%A6%E9%99%A2&poi=%E5%85%B3%E5%B1%B1%E5%A4%A7%E9%81%93

@app.route('/test', methods=['GET', 'POST'])
def test():

    argsItems = request.args
    if argsItems and len(argsItems) >= 12 :
        record = {'route_id': int(request.args.get('route_id')),
        'localtime':request.args.get('localtime'),
        'lot':int(request.args.get('lot')),
        'lat':int(request.args.get('lat')),
        'alt':float(request.args.get('alt')) ,
        'speed':float(request.args.get('speed')) ,
        'head':float(request.args.get('head')),
        'accracy':float(request.args.get('accracy')),
        'type':int(request.args.get('type')),
        'seg_index': int(request.args.get('seg_index')),
        'next_station':request.args.get('next_station'),
        'poi':request.args.get('poi')
        }

        db.saveRecord(record)

        return str(record)


    return 'Invalid param'


# http://127.0.0.1:5000/show?num=3
@app.route('/show', methods=['GET'])
def show():

    num = request.args.get('num')

    if not num :
        num = 10

    return  db.showContents(num)




if __name__ == '__main__':
#判断是否这个脚本是从命令行直接运行
    db.createTable()
    # app.run(host = '0.0.0.0', port = 5000, debug = True)

    http_server = WSGIServer(('0.0.0.0', 5000), app)
    http_server.serve_forever()



    #db.showContents(10)
