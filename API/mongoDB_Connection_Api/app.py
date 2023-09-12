# Dependencies
import warnings
warnings.filterwarnings("ignore", category=UserWarning)
from flask import Flask, request, jsonify
# from waitress import serve
from flask_cors import CORS, cross_origin
app = Flask(__name__)
CORS(app)
import pymongo
uri = "mongodb+srv://ajitgupta:ajitgupta@cluster0.fmasiqv.mongodb.net/?retryWrites=true&w=majority"
# Create a new client and connect to the server
client = pymongo.MongoClient(uri)
mydb = client["PRISM"]
mycol = mydb["AP"]
@app.route('/upload', methods=['POST'])
def upload():
    if request.method == 'POST':
        try:
            data = request.get_json()
            print(data)
            print("Pinged your deployment. You successfully connected to MongoDB!")
            x = mycol.insert_one(data).inserted_id
            print(x)
            return {"status": "success"}
        except Exception as e:
            print(e)
            return {"status": "failed"}
@app.route('/')
def home():
    return "Hello"