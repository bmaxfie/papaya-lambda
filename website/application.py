from flask import Flask, render_template, request



### Flask Application:

app = Flask(__name__, template_folder="static")

### Routes:
@app.route('/')
def index():
	return render_template('index.html')

@app.route('/login', methods=['GET', 'POST'])
def login():
	if request.method == 'GET':
		return render_template('login.html')
	if request.method == 'POST':
		access_key = request.args.get('access_key','')
		



if __name__ == "__main__":
	application.debug = True
	application.run(host="0.0.0.0")