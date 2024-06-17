import pymysql.cursors


class SqlInit:

    def __init__(self, mysql, app):
        self.mysql = mysql
        self.app = app
        app.config['MYSQL_DATABASE_DB'] = None
        mysql.init_app(app)

        print("Initializing database")
        self.execute_sql("init.sql")
        self.execute_sql("fake.sql")
        app.config['MYSQL_DATABASE_DB'] = 'videoapp'
        mysql.init_app(app)

    def execute_sql(self, filename):
        f = open(filename)
        sql_file = f.read()
        f.close()

        conn = self.mysql.connect()
        cur = conn.cursor(pymysql.cursors.DictCursor)
        for command in sql_file.split(';'):
            if not command.isspace():
                cur.execute(command)
        conn.commit()
