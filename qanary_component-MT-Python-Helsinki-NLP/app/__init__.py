from app.mt_helsinki_nlp import mt_helsinki_nlp
from flask import Flask

# default config file (use -c parameter on command line specify a custom config file)
configfile = "app.conf"

# endpoint for health information of the service required for Spring Boot Admin server callback
healthendpoint = "/health"

aboutendpoint = "/about"

# initialize Flask app and add the externalized service information
app = Flask(__name__)
app.register_blueprint(mt_helsinki_nlp)


@app.route(healthendpoint, methods=['GET'])
def health():
    """required health endpoint for callback of Spring Boot Admin server"""
    return "alive"

@app.route(aboutendpoint, methods=['GET'])
def about():
    """required about endpoint for callback of Spring Boot Admin server"""
    return "about"