from werkzeug.wrappers import Request, Response
import jwt

class middleware():

    def __init__(self, app):
        self.app = app

    def __call__(self, environ, start_response):
        request = Request(environ)
        token = request.headers.get('Authorization')
        try:
            if not request.path == "/login" and not request.path == "/register" and not request.path.startswith("/files"):
                payload = jwt.decode(token, "secret", algorithms=["HS256"])
                environ['user_id'] = payload['id']
            return self.app(environ, start_response)
        except:
            res = Response('Authorization failed',status=401)
            return res(environ, start_response)