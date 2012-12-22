package copy.any.instant;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LogInActivity extends Activity implements OnClickListener {

	// Instance Fields
	private Button OK;
	private Button register;
	private EditText usernameInput;
	private EditText passwordInput;
	private String username;
	private String password;
	private TextView changeAccountString;
	private TextView usernameString;
	private TextView passwordString;
	private Button shortcut;
	private Button changeAccountButton;

	// URL
	private String logInURL = "http://www.maxhovens.nl/copies/scripts/jsonLogin.php";
	private String retrieveString = "Response";
	private String inlogFile = "Inlog";

	/**
	 * Sets the contentview. In addition the view elements are initialized.
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		
		usernameString = (TextView) findViewById(R.id.usernameString);
		passwordString = (TextView) findViewById(R.id.passwordString);
		changeAccountString = (TextView) findViewById(R.id.changeAccountString);
		usernameInput = (EditText) findViewById(R.id.usernameInput);
		passwordInput = (EditText) findViewById(R.id.passwordInput);
		changeAccountButton = (Button) findViewById(R.id.changeAccountButton);
		changeAccountButton.setOnClickListener(this);
		shortcut= (Button) findViewById(R.id.shortcut);
		shortcut.setOnClickListener(this);
		OK = (Button) findViewById(R.id.login);
		register = (Button) findViewById(R.id.register);
		OK.setOnClickListener(this);
		register.setOnClickListener(this);
	}

	public void onPause() {
		super.onPause();
		passwordInput.setText("");
		usernameInput.requestFocus();
	}


	/**
	 * If OK is clicked, the password and username are sent to the server.
	 * If register is clicked, the register Activity will start.
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.login: 
			onOKClicked();
			break;
		case R.id.register:
			Intent intent = new Intent(this,RegisterActivity.class);
			startActivity(intent);
			break;
		case R.id.changeAccountButton:
			onChangeAccountClicked();
			break;
		case R.id.shortcut:
			onShortcutClicked();
			break;
		}
	}

	private void onShortcutClicked() {
		new CheckInlogData().execute(new String[] {"Niks"});
	}

	private void onChangeAccountClicked() {
		setViewToLogIn();
	}

	private void setViewToLogIn() {
		changeAccountString.setVisibility(View.GONE);
		changeAccountButton.setVisibility(View.GONE);
		shortcut.setVisibility(View.GONE);	
		usernameString.setVisibility(View.VISIBLE);
		usernameInput.setVisibility(View.VISIBLE);
		passwordString.setVisibility(View.VISIBLE);
		passwordInput.setVisibility(View.VISIBLE);
		register.setVisibility(View.VISIBLE);
		OK.setVisibility(View.VISIBLE);		
	}

	private void setViewToShortCut() {
		usernameString.setVisibility(View.GONE);
		usernameInput.setVisibility(View.GONE);
		passwordString.setVisibility(View.GONE);
		passwordInput.setVisibility(View.GONE);
		register.setVisibility(View.GONE);
		OK.setVisibility(View.GONE);
		changeAccountString.setVisibility(View.VISIBLE);
		changeAccountButton.setVisibility(View.VISIBLE);
		shortcut.setVisibility(View.VISIBLE);

		changeAccountString.append("Click OK to log in as "+username+".");
	}

	public void onResume() {
		super.onResume();
		if (checkInlogFile()) {
			retrieveInlogFile();
			setViewToShortCut();			
		}
	}

	/**
	 * This method is called when the OK button is clicked. It will check a file for the username/password combination. If that fails the server will be called upon to check the combi.
	 * The response will be read and appropriately handled.
	 */
	public void onOKClicked() {
		username = usernameInput.getText().toString();
		password = passwordInput.getText().toString();

		new CheckInlogData().execute(new String[] {"niks"});		
	}

	private String createConnection() throws JSONException {
		JSONObject j = new JSONObject();
		try {
			j.put("User", username);
			j.put("Password", password);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		Connection conn = new Connection(logInURL,j,getResources());
		String back = conn.unprotectedConnection();

		return back;
	}

	private void handleReturn(String back) throws JSONException {
		Toast toast = Toast.makeText(getApplicationContext(), "",Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);		
		JSONObject json = new JSONObject(back);
		json = json.getJSONObject(retrieveString);
		if (json.getBoolean("LogInSuccess")) {
			createInlogFile();
			Intent intent = new Intent(this,AndroidKopierenActivity.class);
			intent.putExtra("Username", username);
			startActivity(intent);
		} else if (json.getBoolean("IncorrectUse")) {
			passwordInput.setText("");
			toast.setText("Incorrect username/password combination. \nPlease try again.");	
			toast.show();		
		} else {
			toast.setText("Log in failed, please try again later.\nIf this happens regularly, please report the issue.");
			toast.show();
		}
	}

	/**
	 * Checks the file for the username and password.
	 * @return true if the username/password combination is correct.
	 */
	private boolean checkInlogFile() {
		SharedPreferences settings = getSharedPreferences(inlogFile, 0);
		return settings.getBoolean("useInlogFile", false);		
	}

	private void retrieveInlogFile() {
		SharedPreferences settings = getSharedPreferences(inlogFile, 0);
		username = settings.getString("Username", "asdf890u320f/");
		password = settings.getString("Password", "/");
	}

	/**
	 * This method will create a file, holding username(s) and password(s).
	 */
	private void createInlogFile() {
		SharedPreferences settings = getSharedPreferences(inlogFile, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("useInlogFile", true);
		editor.putString("Username", username);
		editor.putString("Password", password);
		editor.commit();
	}

	private class CheckInlogData extends AsyncTask<String, Void, String> {
		protected String doInBackground(String... s) {
			String x = null;
			try {
				x = createConnection();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return x;
		}

		protected void onPostExecute(String result) {
			try {
				handleReturn(result);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
	}
}
