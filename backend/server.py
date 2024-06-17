import random
import threading
from datetime import date, datetime

import pymysql
from flask import Flask, app, jsonify, request, send_from_directory, Response

import json
from pathlib import Path
import os
from flaskext.mysql import MySQL
import uuid
import cv2
import subprocess
import jwt
from middleware import middleware
from sql_init import SqlInit
from flask.json.provider import DefaultJSONProvider


# turn dates to ISO format for easier manipulation
class UpdatedJSONProvider(DefaultJSONProvider):
    def default(self, o):
        if isinstance(o, date) or isinstance(o, datetime):
            return o.isoformat()
        return super().default(o)


app = Flask(__name__)
app.wsgi_app = middleware(app.wsgi_app)
app.json = UpdatedJSONProvider(app)

mysql = MySQL()
cursor = pymysql.cursors.DictCursor

videos_dir = "videos/"
images_dir = "images/"
logs_dir = "logs/"

# Configure database
def init():
    app.config['MYSQL_DATABASE_USER'] = 'root'
    app.config['MYSQL_DATABASE_PASSWORD'] = 'olli'
    app.config['MYSQL_DATABASE_DB'] = 'videoapp'
    app.config['MYSQL_DATABASE_HOST'] = 'localhost'
    mysql.init_app(app)


@app.before_request
def log_request_info():
    app.logger.debug('Headers: %s', request.headers)
    app.logger.debug('Body: %s', request.get_data())

@app.route('/files/<string:name>/<path:path>', methods=['GET'])
@app.route('/files/<name>', methods=['GET'])
def files(name, path=None):
    if path is None:
        prefix = name.split('.')
        prefix.reverse()
        prefix = prefix[0]
        return send_from_directory(images_dir if prefix == 'jpg' else logs_dir, name)
    else:
        parts = path.split('/')
        folder = os.path.join(videos_dir, name)
        if len(parts) == 1:
            return send_from_directory(folder, parts[0])
        else:
            folder = os.path.join(folder, parts[0])
            return send_from_directory(folder, parts[1])


@app.route('/login', methods=['POST'])
def login():
    data = request.json
    name, password = data['name'], data['password']
    conn = mysql.connect()
    cur = conn.cursor(cursor)
    cur.execute(f"select * from user where name = '{name}' and password = '{password}'")
    rows = cur.fetchall()
    if len(rows) > 0:
        id = rows[0]['id']
        token = jwt.encode({'id':id},"secret", "HS256")
        return jsonify({'token': token, 'id':id})
    else:
        return jsonify({'token': '', 'id':-1})

@app.route('/register', methods=['POST'])
def register():
    data = request.form
    username = data['username']
    password = data['password']
    bio = data['bio']
    pp = str(uuid.uuid4())
    pp_file = request.files['pp'] if 'pp' in request.files else None

    conn = mysql.connect()
    cur = conn.cursor(cursor)
    cur.execute(f"select * from User where name = '{username}'")
    rows = cur.fetchall()
    if len(rows) > 0:
        return jsonify({'user_id': -1})
    else:
        if pp_file is not None:
            pp_file.save(os.path.join(images_dir, f"{pp}.jpg"))

        sql = f"insert into User (name, password, profile_picture, bio) values (%s, %s, %s, %s);"
        values = (username, password, pp, bio)
        cur.execute(sql, values)
        conn.commit()
        id = cur.lastrowid
        token = jwt.encode({'id', id}, "secret", "HS256")
        return jsonify({'token': token, 'id': id})

@app.route('/profile', methods=['PUT', 'GET', 'DELETE'])
def profile():
    conn = mysql.connect()
    cur = conn.cursor(cursor)

    if request.method == 'PUT':
        data = request.form
        uid = request.environ.get('user_id')
        bio = data['bio']
        pp = request.files['pp'] if 'pp' in request.files else None
        if pp is not None:
            cur.execute("select profile_picture as pp from user where id = %s", uid)
            pp_resource = cur.fetchone()['pp']
            pp.save(os.path.join(images_dir, f"{pp_resource}.jpg"))

        sql = "update User set bio = %s, password = %s where id = %s"
        values = (bio, uid)
        cur.execute(sql, values)
        conn.commit()
        return jsonify({'status': 'ok'})
    elif request.method == 'GET':
        uid = request.environ.get('user_id')
        pid = request.args.get('profile_id', default=uid)
        sql = """select u.name, u.profile_picture, u.bio, u.id,
        (select case when count(*) >0 then 'true' else 'false' end from subscription s where s.subscribed_id = u.id and s.subscriber_id = %s) as is_subscribed,
        (select count(*) from view v where v.user_id = u.id) as view_count,
        (select count(*) from subscription s where s.subscribed_id = u.id) as subscriber_count,
        (select count(*) from video v where v.user_id = u.id) as video_count,
        (select count(*) from poll p where p.user_id = u.id) as poll_count,
        (select count(*) from post p where p.user_id = u.id) as post_count
        from user u where u.id = %s"""
        cur.execute(sql, (uid, pid))
        resp = cur.fetchone()
        sql2 = """ select id, 'video' as type, (select count(*) from likes l where l.type = type and l.type_id = id and l.is_like = 'true') as likes, (select count(*) from likes l where l.type = type and l.type_id = id and l.is_like = 'false') as dislikes, (select count(*) from comment c where c.type = type and c.type_id = id) as comments ,create_time from video where user_id = %s and visibility = 'public' union all
            select id, 'poll' as type, (select count(*) from likes l where l.type = type and l.type_id = id and l.is_like = 'true') as likes, (select count(*) from likes l where l.type = type and l.type_id = id and l.is_like = 'false') as dislikes, (select count(*) from comment c where c.type = type and c.type_id = id) as comments, create_time from poll where user_id = %s union all
            select id, 'post' as type, (select count(*) from likes l where l.type = type and l.type_id = id and l.is_like = 'true') as likes, (select count(*) from likes l where l.type = type and l.type_id = id and l.is_like = 'false') as dislikes, (select count(*) from comment c where c.type = type and c.type_id = id) as comments, create_time from post where user_id = %s 
            order by create_time desc
        """
        cur.execute(sql2, (pid, pid, pid))
        resp['media'] = cur.fetchall()
        print(resp)
        return jsonify(resp)
    elif request.method == 'DELETE':
        uid = request.environ.get('user_id')
        cur.execute("delete from user where id = %s", uid)
        #cur.execute("delete from tag inner join video on video.id = tag.video_id where video.user_id = %s", uid)
        cur.execute("delete from likes where user_id = %s", uid)
        #get videos and pp files and delete---------------------------------------
        cur.execute("delete from video where user_id = %s", uid)
        cur.execute("delete from subscription where subscriber_id = %s or subscribed_id = %s", (uid, uid))
        cur.execute("delete from comment where user_id = %s", uid)
        conn.commit()
        return jsonify({'status': 'ok'})

    return jsonify({'status': 'err'})

#"""select v.*, u.name, u.profile_picture, u.id as user_id,
 #       (select count(*) from view vi where vi.video_id = v.id) as views,
  #      (select count(*) from likes l where l.video_id = v.id and l.is_like = 'true') as likes,
   #     (select count(*) from likes l where l.video_id = v.id and l.is_like = 'false') as dislikes
    #    from video v inner join user u on v.user_id = u.id where v.user_id = %s && visibility = 'public'"""

@app.route('/subscriptions', methods=['GET'])
def subscriptions():
    uid = request.environ.get('user_id')
    conn = mysql.connect()
    cur = conn.cursor(cursor)
    sql1 = "select u.id as user_id, u.profile_picture, u.name from user u inner join subscription s on u.id = s.subscribed_id where s.subscriber_id =%s order by u.name"

    sql2 = """select video.id, 'video' as type, (select count(*) from likes l where l.type = type and l.type_id = id and l.is_like = 'true') as likes, (select count(*) from likes l where l.type = type and l.type_id = id and l.is_like = 'false') as dislikes, (select count(*) from comment c where c.type = type and c.type_id = id) as comments, create_time from video inner join subscription on user_id = subscribed_id where subscriber_id = %s and visibility = 'public' union all
            select poll.id, 'poll' as type, (select count(*) from likes l where l.type = type and l.type_id = id and l.is_like = 'true') as likes, (select count(*) from likes l where l.type = type and l.type_id = id and l.is_like = 'false') as dislikes, (select count(*) from comment c where c.type = type and c.type_id = id) as comments, create_time from poll inner join subscription on user_id = subscribed_id where subscriber_id = %s union all
            select post.id, 'post' as type, (select count(*) from likes l where l.type = type and l.type_id = id and l.is_like = 'true') as likes, (select count(*) from likes l where l.type = type and l.type_id = id and l.is_like = 'false') as dislikes, (select count(*) from comment c where c.type = type and c.type_id = id) as comments, create_time from post inner join subscription on user_id = subscribed_id where subscriber_id = %s
            order by create_time"""
    cur.execute(sql1, uid)
    resp = {'user_list': cur.fetchall()}
    cur.execute(sql2, (uid, uid, uid))
    resp['media_list'] = cur.fetchall()
    print(resp)
    return jsonify(resp)


@app.route('/subscribe', methods=['POST', 'DELETE'])
def subscribe():
    my_id = request.environ.get('user_id')
    subscribed_id = request.args.get('subscribed_id')

    conn = mysql.connect()
    cur = conn.cursor(cursor)

    sql = "select * from subscription where subscriber_id =  %s and subscribed_id = %s"
    values = (my_id, subscribed_id)
    cur.execute(sql, values)

    exists = True if len(cur.fetchall()) > 0 else False

    if request.method == 'POST' and not exists:
        sql = "insert into subscription (subscriber_id ,subscribed_id) values (%s, %s)"
        values = (my_id, subscribed_id)
        cur.execute(sql, values)
        conn.commit()
        return jsonify({'status': 'ok'})

    elif request.method == 'DELETE' and exists:
        sql = "delete from subscription where subscriber_id = %s and subscribed_id = %s"
        values = (my_id, subscribed_id)
        cur.execute(sql, values)
        conn.commit()
        return jsonify({'status': 'ok'})

    return jsonify({'status': 'err'})

@app.route('/like', methods=['POST', 'DELETE'])
def like():
    like_type = request.args.get('type')
    is_like = request.args.get('is_like')
    user_id = request.environ.get('user_id')
    like_id = request.args.get('like_id')
    conn = mysql.connect()
    cur = conn.cursor(cursor)

    sql1 = f"delete from likes where user_id = %s and type = %s and type_id = %s"
    cur.execute(sql1, (user_id, like_type, like_id))
    conn.commit()

    if request.method == 'POST':

        sql2 = "insert into likes (type, type_id, user_id, is_like) values (%s, %s, %s, %s)"
        values = (like_type, like_id, user_id, is_like)
        cur.execute(sql2, values)
        conn.commit()

    return jsonify({'status': 'ok'})


@app.route('/comment', methods=['POST', 'DELETE', 'GET'])
def comment():
    conn = mysql.connect()
    cur = conn.cursor(cursor)
    if request.method == 'POST':
        data = request.json
        uid = request.environ.get('user_id')
        pid = None if 'parent_id' not in data else data['parent_id']
        content = data['content']
        type = data['type']
        tid = data['type_id']
        sql = "insert into comment (parent_id, content, type, type_id, user_id) values (%s, %s, %s, %s, %s)"
        values = (pid, content, type, tid, uid)
        cur.execute(sql, values)
        conn.commit()
        comment_id = cur.lastrowid
        sql2 = """select c.*, u.profile_picture, u.name,
        (select case when count(*) >0 then 'true' else 'false' end from likes where likes.user_id = %s and likes.type_id = c.id and likes.type = 'comment' and is_like = 'true') as is_liked,
        (select case when count(*) >0 then 'true' else 'false' end from likes where likes.user_id = %s and likes.type_id = c.id and likes.type = 'comment' and is_like = 'false') as is_disliked,
        (select count(*) from likes where type='comment' and type_id = c.id and is_like = 'true') as likes,
        (select count(*) from likes where type='comment' and type_id = c.id and is_like = 'false') as dislikes
        from comment c inner join user u on c.user_id = u.id where c.id = %s
        """
        cur.execute(sql2, (uid,uid,comment_id))
        return jsonify(cur.fetchone())
    elif request.method == 'DELETE':
        cid = request.args.get('comment_id')
        sql = "delete from comment where id = %s"
        cur.execute(sql, cid)
        conn.commit()
        return jsonify({'status': 'ok'})
    elif request.method == 'GET':
        cid = request.args.get('comment_id')
        uid = request.environ.get('user_id')
        sql = """select c.content, c.create_time, u.id, u.name, u.profile_picture,
                (select case when count(*) >0 then 'true' else 'false' end from likes where user_id = %s and type = 'comment' and type_id = c.id and is_like = 'true') as is_liked,
                (select case when count(*) >0 then 'true' else 'false' end from likes where user_id = %s and type = 'comment' and type_id = c.id and is_like = 'false') as is_disliked,
                (select count(*) from likes where type = 'comment' and type_id = %s and is_like = 'true') as likes,
                (select count(*) from likes where type = 'comment' and type_id = %s and is_like = 'false') as dislikes,
                (select count(*) from comment where parent_id = c.id) as replies
                from comment c inner join user u on c.user_id = u.id where c.id = %s"""
        cur.execute(sql, (uid, uid, cid, cid, cid))
        return jsonify(cur.fetchone())


def splice_video(video_resource, vid, width, height):
    lower_width = int(width * 0.7)
    lower_height = int(height * 0.7)
    lower_height = lower_height + 1 if lower_height % 2 != 0 else lower_height + 0
    lower_width = lower_width + 1 if lower_width % 2 != 0 else lower_width + 0
    subprocess.call([
        'ffmpeg',
        '-i', 'original.mp4',
        '-filter_complex',
        f'[v:0]split=2[temp1][temp2];[temp1]scale=w={lower_width}:h={lower_height},fps=30[v1];[temp2]scale=w={width}:h={height},fps=30[v2];',
        '-sc_threshold', '0', '-g', '30',
        '-map', '[v1]', '-c:v:0', 'libx264', '-b:v:0', '1500k',
        '-map', '[v2]', '-c:v:1', 'libx264', '-b:v:1', '3000k',
        '-map', 'a:0', '-map', 'a:0', '-c:a', 'aac', '-b:a', '128k', '-ac', '2',
        '-f', 'hls', '-hls_segment_type', 'fmp4', '-hls_time', '4',
        '-hls_playlist_type', 'vod', '-master_pl_name', 'master.m3u8',
        '-hls_segment_filename', f'stream_%v/segment%03d.m4s', '-strftime_mkdir', '1',
        '-var_stream_map', 'v:0,a:0,name:360p v:1,a:1,name:720p',
        f'stream_%v.m3u8'
    ], cwd=os.path.join(videos_dir, video_resource) )
    conn = mysql.connect()
    cur = conn.cursor(cursor)
    cur.execute("update video set status = 'done' where id = %s", vid)
    conn.commit()
    #path = os.path.join(logs_dir, f'{video_resource}.txt')
    #with open(path) as file:
     #   lines = file.readlines()
     #   lines.reverse()
      #  if lines[0].split() == '[aac':
       #     cur.execute("update video set status = 'uploaded' where id = %s", vid)
       # else:
        #    cur.execute("delete from video where id = %s", vid)


@app.route('/send_chunk/<resource>', methods=['POST'])
def send_chunk(resource):
    data = bytes(request.data)
    path = os.path.join(videos_dir, resource, 'original.mp4')
    with open(path, 'ab') as file:
        file.write(data)
    return Response("{'status':'ok'}", status=200, mimetype='application/json')

@app.route('/prepare_video/<resource>', methods=['POST'])
def prepare_video(resource):
    conn = mysql.connect()
    cur = conn.cursor(cursor)
    cur.execute("select thumbnail_resource as t, id from video where video_resource = %s", resource)
    v = cur.fetchone()
    video_folder = os.path.join(videos_dir, resource)
    video_path = os.path.join(video_folder, "original.mp4")
    thumbnail_path = os.path.join(images_dir, f"{v['t']}.jpg")

    cap = cv2.VideoCapture(video_path)
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    fps = cap.get(cv2.CAP_PROP_FPS)
    frame_count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    duration = frame_count / fps
    cur.execute('update video set duration = %s where id = %s', (duration, v['id']))
    conn.commit()

    if not os.path.exists(thumbnail_path):
        time = duration / 2 * 1000
        cap.set(cv2.CAP_PROP_POS_MSEC, time)
        success, img = cap.read()
        cv2.imwrite(thumbnail_path, img)

    threading.Thread(target=splice_video, args=(resource, v['id'], width, height)).start()
    return jsonify({'status': 'ok'})



@app.route('/video', methods=['POST', 'DELETE', 'GET', 'PUT'])
def video():
    conn = mysql.connect()
    cur = conn.cursor(cursor)
    Path(videos_dir).mkdir(parents=True, exist_ok=True)
    if request.method == 'POST':
        Path(logs_dir).mkdir(parents=True, exist_ok=True)
        data = request.form
        title = data['title']
        description = data['description']
        visibility = data['visibility']
        uid = request.environ.get('user_id')
        #print(request.files)
        #----video_file = request.files['video']
        thumbnail_file = None if not 'thumbnail' in request.files else request.files['thumbnail']

        video_resource = str(uuid.uuid4())

        video_folder = os.path.join(videos_dir, video_resource)
        Path(video_folder).mkdir(parents=True, exist_ok=True)


        thumbnail_resource = str(uuid.uuid4())
        thumbnail_path = os.path.join(images_dir, f"{thumbnail_resource}.jpg")
        if thumbnail_file is not None:
            thumbnail_file.save(thumbnail_path)

        sql = "insert into video (title, description, video_resource, thumbnail_resource, visibility, duration, user_id, status) values (%s, %s, %s, %s, %s, null, %s, 'uploading')"
        values = (title, description, video_resource, thumbnail_resource, visibility, uid)
        cur.execute(sql, values)
        vid = cur.lastrowid
        conn.commit()

        #return jsonify({'video_resource': video_resource, 'frame_count': frame_count})
        return jsonify({'video_resource': video_resource})

    elif request.method == 'DELETE':
        video_id = request.args.get('video_id')
        cur.execute("select thumbnail_resource as tr, video_resource as vr from video where id = %s", video_id)
        video = cur.fetchone()
        cur.execute("delete from video where id = %s", video_id)
        cur.execute("delete from tag where video_id = %s", video_id)
        conn.commit()
        video_path = os.path.join(videos_dir, video['vr'])
        Path(video_path).unlink(missing_ok=True)
        thumbnail_path = os.path.join(images_dir, video['tr'])
        Path(thumbnail_path).unlink(missing_ok=True)
        return jsonify({'status': 'ok'})
    elif request.method == 'GET':
        conn = mysql.connect()
        cur = conn.cursor(cursor)

        vid = request.args.get('video_id')
        uid = request.environ.get('user_id')
        sql1 = """select
                    (select count(*) from likes where type = 'video' and type_id = v.id and is_like = 'true') as likes,
                    (select count(*) from likes where type = 'video' and type_id = v.id and is_like = 'false') as dislikes,
                    (select case when count(*) >0 then 'true' else 'false' end from likes where type = 'video' and type_id = v.id and user_id = %s and is_like = 'true') as is_liked,
                    (select case when count(*) >0 then 'true' else 'false' end from likes where type = 'video' and type_id = v.id and user_id = %s and is_like = 'false') as is_disliked,
                    (select case when count(*) >0 then 'true' else 'false' end from subscription inner join video on video.user_id = subscription.subscribed_id where subscribed_id = v.user_id and subscriber_id = %s) as is_subscribed,
                    (select count(*) from subscription inner join video on video.user_id = subscription.subscribed_id where video.id = v.id) as subscriptions,
                    (select count(*) from view where video_id = v.id) as views,
                    v.*, u.name, u.profile_picture from video v inner join user u on v.user_id = u.id where v.id = %s;"""
        values = (uid, uid, uid, vid)
        cur.execute(sql1, values)
        resp = cur.fetchall()[0]
        sql2 = "select v.id, 'video' as type from video v where visibility = 'public' and not v.id = %s order by rand() limit 5"
        cur.execute(sql2, vid)
        resp['recommendations'] = cur.fetchall()
        cur.execute("insert into view (video_id, user_id) values (%s, %s)", (vid, uid))
        conn.commit()
        return jsonify(resp)
    elif request.method == 'PUT':
        data = request.form
        video_id = data['video_id']
        title = data['title']
        description = data['description']
        visibility = data['visibility']
        tags = data['tags']
        print(request.files)
        thumbnail_file = request.files['thumbnail']
        if thumbnail_file is not None:
            cur.execute("select thumbnail_resource as tr from video where id = %s", video_id)
            tr = cur.fetchone()['tr']
            thumbnail_file.save(os.path.join(images_dir, f"{tr}.jpg"))

        sql = "update video set title = %s, description = %s, visibility = %s where id = %s"
        values = (title, description, visibility, video_id)
        cur.execute(sql, values)
        cur.execute("delete from tag where video_id = %s", video_id)
        tags = [(tag, video_id) for tag in tags]
        print(tags)
        cur.executemany("insert into tag (content, video_id) values (%s, %s)", tags)
        conn.commit()
        return jsonify({'status': 'ok'})

    return jsonify({'status': 'err'})

@app.route('/comments/<type>/<type_id>', methods=['GET'])
def comments(type, type_id):
    conn = mysql.connect()
    cur = conn.cursor(cursor)
    uid = request.environ.get('user_id')
    sql = """select c.*, u.profile_picture, u.name,
    (select case when count(*) >0 then 'true' else 'false' end from likes where likes.user_id = %s and likes.type = 'comment' and likes.type_id = c.id and is_like = 'true') as is_liked,
    (select case when count(*) >0 then 'true' else 'false' end from likes where likes.user_id = %s and likes.type = 'comment' and likes.type_id = c.id and is_like = 'false') as is_disliked,
    (select count(*) from likes where type_id = c.id and type = 'comment' and is_like = 'true') as likes,
    (select count(*) from likes where type_id = c.id and type = 'comment' and is_like = 'false') as dislikes,
    (select count(*) from comment where parent_id = c.id) as replies
    from comment c inner join user u on c.user_id = u.id where parent_id is null and type_id = %s and type = %s
    order by c.create_time desc"""
    values = (uid, uid, type_id, type)
    cur.execute(sql, values)
    rows = cur.fetchall()
    print(rows)
    return jsonify(rows)

@app.route('/replies/<comment_id>', methods=['GET'])
def replies(comment_id):
    conn = mysql.connect()
    cur = conn.cursor(cursor)
    uid = request.environ.get('user_id')
    sql = """select c.*, u.profile_picture, u.name,
    (select case when count(*) >0 then 'true' else 'false' end from likes where likes.user_id = %s and likes.type_id = c.id and likes.type = 'comment' and is_like = 'true') as is_liked,
    (select case when count(*) >0 then 'true' else 'false' end from likes where likes.user_id = %s and likes.type_id = c.id and likes.type = 'comment' is_like = 'false') as is_disliked,
    (select count(*) from likes where type_id = c.id and type = 'comment' and is_like = 'true') as likes,
    (select count(*) from likes where type_id = c.id and type = 'comment' and is_like = 'false') as dislikes,
    (select count(*) from comment where parent_id = %s) as replies,
    from comment c inner join user u on c.user_id = u.id where c.parent_id = %s or c.id = %s
    order by c.create_date"""
    values = (uid, uid, comment_id, comment_id, comment_id)
    cur.execute(sql, values)
    resp = cur.fetchall()
    return jsonify(resp)

@app.route('/feed', methods=['GET'])
def feed():
    conn = mysql.connect()
    cur = conn.cursor(cursor)
    uid = request.environ.get('user_id')
    page_size = request.args.get('page_size', default=5, type=int)
    page_number = request.args.get('page_number', default=1, type=int)
    seed_value = request.args.get('seed_value', default=random.uniform(0, 1), type=float)
    sql = """SELECT v.*, u.name, u.profile_picture,(select count(*) from view where video_id = v.id) as views,(select count(*) from likes where type = 'video' and type_id = v.id and is_like = 'true')as likes, (select count(*) from likes where type = 'video' and type_id = v.id and is_like = 'false')as dislikes
    FROM video v inner join user u on v.user_id = u.id
    WHERE u.id != %s AND v.visibility = 'public'
    order by rand(%s)
    LIMIT %s
    offset %s"""
    offset = (page_number - 1) * page_size
    values = (uid, seed_value, page_size, offset)
    cur.execute(sql, values)
    resp = cur.fetchall()
    print(resp)
    return jsonify({'results': resp, 'seed_value': seed_value, 'page_number':page_number})

@app.route('/search/<search>', methods=['GET'])
def search(search):
    conn = mysql.connect()
    cur = conn.cursor(cursor)
    sql = "select title from video where title like concat('%%', %s, '%%') order by rand() limit 5"
    cur.execute(sql, search)
    resp = cur.fetchall()
    return jsonify(resp)

@app.route('/search/videos', methods=['GET'])
def search_videos():
    conn = mysql.connect()
    cur = conn.cursor(cursor)
    search = request.args.get('search')
    page_size = request.args.get('page_size', default=5, type=int)
    page_number = request.args.get('page_number', default=1, type=int)
    seed_value = request.args.get('seed_value', default=random.uniform(0, 1), type=float)
    sql = """
        SELECT v.*, u.name, u.profile_picture, (select count(*) from view where video_id = video.id) as views, (select count(*) from likes where type = 'video' and type_id = v.id and is_like = 'true')as likes, (select count(*) from likes where type = 'video' and type_id = v.id and is_like = 'false')as dislikes
        FROM video v inner join user u on v.user_id = u.id
        WHERE v.visibility = 'public'
        MATCH(v.title, v.description)
        AGAINST(%s IN NATURAL LANGUAGE MODE)
        order by rand(%s)
        limit %s
        offset %s
        """
    offset = (page_number - 1) * page_size
    values = (search, seed_value, page_size, offset)
    cur.execute(sql, values)
    resp = {'results':cur.fetchall(), 'seed_value':seed_value, 'page_number':page_number}
    print(resp)
    return jsonify(resp)

#cahnge later to token based auth.
@app.route('/my_medias', methods=['GET'])
def my_medias():
    conn = mysql.connect()
    cur = conn.cursor(cursor)
    uid = request.environ.get('user_id')
    sql = """
        select id, 'video' as type, (select count(*) from likes l where l.type = type and l.type_id = id and l.is_like = 'true') as likes, (select count(*) from likes l where l.type = type and l.type_id = id and l.is_like = 'false') as dislikes, (select count(*) from comment c where c.type = type and c.type_id = id) as comments, create_time from video where user_id = %s union all
        select id, 'poll' as type, (select count(*) from likes l where l.type = type and l.type_id = id and l.is_like = 'true') as likes, (select count(*) from likes l where l.type = type and l.type_id = id and l.is_like = 'false') as dislikes, (select count(*) from comment c where c.type = type and c.type_id = id) as comments, create_time from poll where user_id = %s union all
        select id, 'post' as type, (select count(*) from likes l where l.type = type and l.type_id = id and l.is_like = 'true') as likes, (select count(*) from likes l where l.type = type and l.type_id = id and l.is_like = 'false') as dislikes, (select count(*) from comment c where c.type = type and c.type_id = id) as comments, create_time from post where user_id = %s
        order by create_time desc
    """
    cur.execute(sql, (uid, uid, uid))
    resp = cur.fetchall()
    print(resp)
    return jsonify(resp)

@app.route('/my_profile', methods=['GET'])
def my_profile():
    conn = mysql.connect()
    cur = conn.cursor(cursor)
    uid = request.environ.get('user_id')
    sql = """
                SELECT u.id as user_id, u.name, u.create_time, u.profile_picture,(select count(*) from view where user_id = u.id) as views,(select count(*) from likes where user_id = u.id and is_like = 'true')as likes, (select count(*) from likes where user_id = u.id and is_like = 'false')as dislikes
                FROM video v inner join user u on v.user_id = u.id
                WHERE u.id = %s
                ORDER BY v.create_time desc
            """
    cur.execute(sql, uid)
    return jsonify(cur.fetchone())

@app.route('/my_likes', methods=['GET'])
def my_likes():
    conn = mysql.connect()
    cur = conn.cursor(cursor)
    uid = request.environ.get('user_id')
    sql = """
                SELECT l.type, l.type_id as id
                FROM likes l
                WHERE l.user_id = %s AND l.is_like = 'true'
                ORDER BY l.create_time desc
            """
    cur.execute(sql, uid)
    return jsonify(cur.fetchall())


#SELECT l.type, l.is_like, v.id as video_id, v.title, v.create_time as video_create_time, v.duration, v.thumbnail_resource, c.id as comment_id, c.content, c.create_time as comment_create_time, c.video_id,u.id as user_id, u.name, u.profile_picture,
 #               (select count(*) from view where video_id = v.id) as video_views,
  #              (select count(*) from likes where video_id = v.id and is_like = 'true')as video_likes,
   #             (select count(*) from likes where video_id = v.id and is_like = 'false')as video_dislikes,
    #            (select count(*) from likes where comment_id = c.id and is_like = 'true')as comment_likes,
     #           (select count(*) from likes where comment_id = v.id and is_like = 'false')as comment_dislikes
      #          (select count(*) from likes where type = l.type and type_id = l.type.id and is_like = 'true')as likes
       #         (select count(*) from likes where type = l.type and type_id = l.type.id and is_like = 'false')as dislikes
        #        FROM likes l left join video v on v.id = l.video_id left join comment c on c.id = l.comment_id inner join user u on u.id = l.user_id
         #       WHERE l.user_id = %s AND l.is_like = 'true' and not v.visibility = 'private'
          #      ORDER BY l.create_time desc



#SELECT l.type, l.is_like, v.id as video_id, v.title, v.create_time as video_create_time, v.duration,
#                v.thumbnail_resource, c.id as comment_id, c.content, c.create_time as comment_create_time, c.video_id,
#                u.id as user_id, u.name, u.profile_picture, p.content, p.create_time as post_create_time,
#                poll.create_time as poll_create_time,
#               (select count(*) from view where video_id = v.id) as video_views,
#                (select count(*) from poll_vote pv inner join poll_option po on po.id = pv.poll_option_id where po.poll_id = poll.id) as votes
#                (select GROUP_CONCAT(resource order by index asc) from post_image pi inner join post on post.id = pi.post_id where post.id == p.id) as images
#                (select count(*) from likes where type = l.type and type_id = l.type.id and is_like = 'true')as likes,
#                (select count(*) from likes where type = l.type and type_id = l.type.id and is_like = 'false')as dislikes
#                FROM likes l left join video v on v.id = l.type_id and l.type = 'video' left join comment c on c.id = l.type_id and l.type = 'comment' left join post p on p.id = l.type_id and l.type = 'post' left join poll on poll.id = l.type_id and l.type = 'poll' inner join user u on u.id = l.user_id
#               WHERE l.user_id = %s AND l.is_like = 'true' and not v.visibility = 'private'
#                ORDER BY l.create_time desc


@app.route('/my_dislikes', methods=['GET'])
def my_dislikes():
    conn = mysql.connect()
    cur = conn.cursor(cursor)
    uid = request.environ.get('user_id')
    sql = """
            SELECT l.type, l.type_id as id 
                FROM likes l
                WHERE l.user_id = %s AND l.is_like = 'false'
                ORDER BY l.create_time desc
            """
    cur.execute(sql, uid)
    resp = cur.fetchall()
    print(resp)
    return jsonify(resp)


@app.route('/my_views', methods=['GET'])
def my_views():
    conn = mysql.connect()
    cur = conn.cursor(cursor)
    uid = request.environ.get('user_id')
    sql = """
            SELECT v.id, "video" as type 
                FROM video v inner join view vi on v.id = vi.video_id
                WHERE vi.user_id = %s AND not v.visibility = 'private'
                ORDER BY vi.create_time desc
            """
    cur.execute(sql, uid)
    resp = cur.fetchall()
    print(f"myviews: {resp}")
    return jsonify(resp)



#"""
 #                   SELECT v.*, u.name, u.profile_picture,
  #                  (select count(*) from view where video_id = v.id) as video_views,
   #                 (select count(*) from likes where type = 'video' and type_id = v.id and is_like = 'true')as video_likes,
    #                (select count(*) from likes where type = 'video' and type_id = v.id and is_like = 'false')as video_dislikes,
     #               FROM video v inner join user u on v.user_id = u.id inner join view vi on vi.video_id = v.id
      #              WHERE vi.user_id = %s
       #             ORDER BY vi.create_time desc
        #        """

@app.route('/post', methods=['GET', 'POST', 'DELETE'])
def post():
    conn = mysql.connect()
    cur = conn.cursor(cursor)
    if request.method == 'GET':
        pid = request.args.get('post_id')
        uid = request.environ.get('user_id')
        sql1 = """
                SELECT p.content, p.id, p.create_time, u.id as user_id, u.name, u.profile_picture,
                (select case when count(*) >0 then 'true' else 'false' end from likes l where l.user_id = %s and l.type_id = p.id and l.type = 'post' and is_like = 'true') as is_liked,
                (select case when count(*) >0 then 'true' else 'false' end from likes l where l.user_id = %s and l.type_id = p.id and l.type = 'post' and is_like = 'false') as is_disliked,
                (select count(*) from likes where type = 'post' and type_id = p.id and is_like = 'true')as likes,
                (select count(*) from likes where type = 'post' and type_id = p.id and is_like = 'false')as dislikes,
                (select count(*) from comment where type = 'post' and type_id = p.id)as comments
                FROM post p inner join user u on p.user_id = u.id 
                WHERE p.id = %s
                """
        cur.execute(sql1, (uid, uid, pid))
        resp = cur.fetchone()
        sql2 = "select resource from post_image pi where pi.post_id = %s order by ordinal asc"
        cur.execute(sql2, pid)
        resp['images'] = cur.fetchall()
        return jsonify(resp)

    elif request.method == 'POST':
        content = request.values['content']#request.args.get('content')
        uid = request.environ.get('user_id')
        sql = "insert into post (user_id, content) values (%s, %s)"
        cur.execute(sql, (uid, content))
        pid = cur.lastrowid
        for key, file in request.files.items():
            resource = str(uuid.uuid4())
            resource_path = os.path.join(images_dir, f"{resource}.jpg")
            file.save(resource_path)
            print(key)
            cur.execute("insert into post_image (ordinal, resource, post_id) values (%s, %s, %s)", (int(key), resource, pid))

        conn.commit()
        return jsonify({'post_id': pid})

    elif request.method == 'DELETE':
        pid = request.args.get('post_id')
        sql = "delete from post p where p.id = %s"
        cur.execute(sql, pid)
        conn.commit()
        return jsonify({'status': 'ok'})


@app.route('/poll', methods=['GET', 'POST', 'DELETE'])
def poll():
    conn = mysql.connect()
    cur = conn.cursor(cursor)
    if request.method == 'GET':
        pid = request.args.get('poll_id')
        uid = request.environ.get('user_id')
        sql1 = """
                        select p.content, p.id, p.create_time, u.id as user_id, u.name, u.profile_picture,
                        (select ordinal from poll_vote pv inner join poll_option po on pv.poll_option_id = po.id where po.poll_id = p.id and pv.user_id = %s) as voted_ordinal,
                        (select case when count(*) >0 then 'true' else 'false' end from likes l where l.user_id = %s and l.type_id = p.id and l.type = 'poll' and is_like = 'true') as is_liked,
                        (select case when count(*) >0 then 'true' else 'false' end from likes l where l.user_id = %s and l.type_id = p.id and l.type = 'poll' and is_like = 'false') as is_disliked,
                        (select count(*) from poll_vote pv inner join poll_option po on pv.poll_option_id = po.id where po.poll_id = p.id) as votes,
                        (select count(*) from likes where type = 'poll' and type_id = p.id and is_like = 'true') as likes,
                        (select count(*) from likes where type = 'poll' and type_id = p.id and is_like = 'false') as dislikes,
                        (select count(*) from comment where type = 'poll' and type_id = p.id) as comments
                        FROM poll p inner join user u on p.user_id = u.id 
                        WHERE p.id = %s
                        """
        cur.execute(sql1, (uid, uid, uid, pid))
        resp = cur.fetchone()
        sql2 = """select id, name, ordinal, resource,
                (select count(*) from poll_vote where poll_option_id = po.id) as votes,
                FLOOR((select votes) / (select count(*) from poll_vote pv inner join poll_option po2 on pv.poll_option_id = po2.id where po2.poll_id = po.poll_id) * 100) as percentage
                from poll_option po where po.poll_id = %s"""
        cur.execute(sql2, pid)
        resp['poll_options'] = cur.fetchall()
        return jsonify(resp)
    elif request.method == 'POST':
        content = request.values['content']
        poll_options = json.loads(request.values['poll_options'])
        uid = request.environ.get('user_id')
        cur.execute("insert into poll (user_id, content) values (%s, %s)", (uid, content))
        pid = cur.lastrowid
        print(poll_options)
        for option in poll_options:
            resource = None
            ordinal = option['ordinal']
            name = option['name']
            file = request.files[ordinal]
            if file:
                resource = str(uuid.uuid4())
                resource_path = os.path.join(images_dir, f"{resource}.jpg")
                file.save(resource_path)
            cur.execute("insert into poll_option (poll_id, ordinal, name, resource) values (%s, %s, %s, %s)", (pid, ordinal, name, resource))

        conn.commit()
        return jsonify({'poll_id': pid})

    elif request.method == 'DELETE':
        pid = request.args.get('poll_id')
        cur.execute("delete from poll_vote pv inner join poll_option po on po.id = pv.poll_option_id where po.poll = %s", pid)
        cur.execute("delete from poll_option where poll_id = %s", pid)
        cur.execute("delete from poll where id = %s", pid)
        conn.commit()
        return jsonify({'status': 'ok'})

@app.route('/poll/vote/<poll_id>', methods=['POST', 'DELETE'])
def poll_vote(poll_id):
    conn = mysql.connect()
    cur = conn.cursor(cursor)
    uid = request.environ.get('user_id')
    cur.execute(
        """delete pv from poll_vote pv 
        inner join poll_option po on po.id = pv.poll_option_id 
        where po.poll_id = %s and pv.user_id = %s""",
        (poll_id, uid))
    if request.method == 'POST':
        poi = request.args.get('poll_option_id')
        cur.execute("insert into poll_vote (poll_option_id, user_id) values (%s, %s)", (poi, uid))

    conn.commit()
    return jsonify({'status': 'ok'})



@app.route('/video_preview', methods=['GET'])
def video_preview():
    conn = mysql.connect()
    cur = conn.cursor(cursor)
    vid = request.args.get('video_id')
    uid = request.environ.get('user_id')
    sql1 = """
                SELECT v.*, v.create_time, u.id, u.name, u.profile_picture,
                (select case when count(*) >0 then 'true' else 'false' end from likes l where l.user_id = %s and l.type_id = v.id and l.type = 'video' and is_like = 'true') as is_liked,
                (select case when count(*) >0 then 'true' else 'false' end from likes l where l.user_id = %s and l.type_id = v.id and l.type = 'video' and is_like = 'false') as is_disliked,
                (select count(*) from likes where type = 'video' and type_id = v.id and is_like = 'true') as likes,
                (select count(*) from likes where type = 'video' and type_id = v.id and is_like = 'false') as dislikes,
                (select count(*) from view where video_id = v.id) as views
                FROM video v inner join user u on v.user_id = u.id 
                WHERE v.id = %s
            """
    cur.execute(sql1, (uid, uid, vid))
    resp = cur.fetchone()
    print(resp)
    return jsonify(resp)


@app.route('/check_progress/<video_resource>', methods=['GET'])
def check_progress(vr):
    path = os.path.join(videos_dir, f"{vr}")
    path = os.path.join(path, "original.mp4")
    cap = cv2.VideoCapture(path)
    frame_count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))

    path = os.path.join(logs_dir, f'{vr}.txt')
    with open(path) as file:
        lines = file.readlines()
        lines.reverse()
        for line in lines:
            values = line.split()
            if values[0] == '[aac':
                return jsonify({'progress': -1, 'status': 'done'})
            elif values[0].startswith('frame='):
                if len(values[0]) == 6:
                    return jsonify({'progress': int(values[1]) / frame_count, 'status': 'progressing'})
                else:
                    frame = values[0].split('=')[1]
                    return jsonify({'progress': int(frame) / frame_count, 'status': 'progressing'})


# Just for testing
@app.route('/demo')
def demo():
    resp = jsonify('demo')
    resp.status = '200'
    return resp

    #path = os.path.join(root_dir, apt_id)
    #Path(path).mkdir(parents=True, exist_ok=True)
    #for k in ['image', 'depth', 'info']:
     #   f = request.files[k]
     #  f.save(os.path.join(path, f.filename))

def main():
    init()

    if mysql.get_db() is not None:
        print(mysql.get_db())
        SqlInit(mysql, app)
    app.secret_key= 'super secret key'
    app.run(host='0.0.0.0', port=3000)

main()

