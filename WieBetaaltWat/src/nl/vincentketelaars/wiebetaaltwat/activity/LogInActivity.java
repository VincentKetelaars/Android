package nl.vincentketelaars.wiebetaaltwat.activity;

import nl.vincentketelaars.wiebetaaltwat.R;
import nl.vincentketelaars.wiebetaaltwat.activity.ConnectionService.LocalBinder;
import nl.vincentketelaars.wiebetaaltwat.other.MyHtmlParser;
import nl.vincentketelaars.wiebetaaltwat.other.MyResultReceiver;
import nl.vincentketelaars.wiebetaaltwat.other.Resources;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This activity lets the user log in to http://www.wiebetaaltwat.nl 
 * In addition it offers the user the possibility to let the app remember the email and password. 
 * This activity therefore offers two views. The normal login view, and a shortcut view. From the shortcut view it the normal view is one button click away.
 * @author Vincent
 *
 */
public class LogInActivity extends Activity implements OnClickListener, MyResultReceiver.Receiver  {

	// View Fields
	private TextView loginTitle;
	private TextView fastLoginTitle;
	private Button OK;
	private EditText emailInput;
	private EditText passwordInput;
	private String email;
	private String password;
	private TextView logInAs;
	private TextView loginEmailAddress;
	private TextView emailString;
	private TextView passwordString;
	private TextView changeAccountString;
	private Button shortcut;
	private Button changeAccountButton;
	private CheckBox rememberMe;
	private ProgressDialog progressDialog;

	// Service
	private boolean mBound;
	private ConnectionService mService;
	private MyResultReceiver mReceiver;

	/**
	 * Sets the contentview. In addition the view elements are initialized.
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			setContentView(R.layout.login_landscape);
		} else {
			setContentView(R.layout.login);	
		}

		// Create MyResultReceiver
		mReceiver = new MyResultReceiver(new Handler());
		mReceiver.setReceiver(this);

		// Bind to the ConnectionService
		bindToService();
		
		// Link the views from the xml login file and set listeners
		loginTitle = (TextView) findViewById(R.id.log_in_title);
		fastLoginTitle = (TextView) findViewById(R.id.fast_log_in_title);
		emailString = (TextView) findViewById(R.id.emailString);
		passwordString = (TextView) findViewById(R.id.passwordString);
		logInAs = (TextView) findViewById(R.id.log_in_as);
		loginEmailAddress = (TextView) findViewById(R.id.log_in_emailaddress);
		emailInput = (EditText) findViewById(R.id.emailInput);
		passwordInput = (EditText) findViewById(R.id.passwordInput);
		changeAccountString = (TextView) findViewById(R.id.change_account_string);
		changeAccountButton = (Button) findViewById(R.id.changeAccountButton);
		changeAccountButton.setOnClickListener(this);
		shortcut= (Button) findViewById(R.id.shortcut);
		shortcut.setOnClickListener(this);
		OK = (Button) findViewById(R.id.login);
		OK.setOnClickListener(this);
		rememberMe = (CheckBox) findViewById(R.id.rememberMe);

		progressDialog = new ProgressDialog(this);
	}

	/**
	 * This method is called when the user leaves the activity.
	 */
	public void onPause() {
		super.onPause();
		passwordInput.setText("");
		emailInput.requestFocus();
	}


	/**
	 * If OK is clicked, the password and email address are sent to the server.
	 * The following two buttons are only available when a email address and password are succesfully remembered.
	 * If the changeAccountButton is clicked, the original login view will be viewed.
	 * If the shortcut button is clicked, the email and password are retrieved from SharedPreferences and sent to the server.
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.login: 
			onOKClicked();
			break;
		case R.id.changeAccountButton:
			onChangeAccountClicked();
			break;
		case R.id.shortcut:
			onShortcutClicked();
			break;
		}
	}

	/**
	 * This method is called when the shortcut button is clicked. This method will return the result of the server to the handler method.
	 */
	private void onShortcutClicked() {
		createConnection();		
		mService.initialize(email, password);
	}

	/**
	 * When the changeAccountButton is clicked, any shared preferences will be removed and the original Login view will be restored.
	 */
	private void onChangeAccountClicked() {
		removeSharedPreferences();
		setViewToLogIn();
	}

	/**
	 * Reset the Login view to the original normal View.
	 */
	private void setViewToLogIn() {
		fastLoginTitle.setVisibility(View.GONE);
		logInAs.setVisibility(View.GONE);
		loginEmailAddress.setVisibility(View.GONE);
		changeAccountString.setVisibility(View.GONE);
		changeAccountButton.setVisibility(View.GONE);
		shortcut.setVisibility(View.GONE);	
		loginTitle.setVisibility(View.VISIBLE);
		emailString.setVisibility(View.VISIBLE);
		emailInput.setVisibility(View.VISIBLE);
		passwordString.setVisibility(View.VISIBLE);
		passwordInput.setVisibility(View.VISIBLE);
		OK.setVisibility(View.VISIBLE);	
		rememberMe.setVisibility(View.VISIBLE);
	}

	/**
	 * Set the login view to the shortcut view, showing only the shortcut button and a change account button.
	 */
	private void setViewToShortCut() {
		loginTitle.setVisibility(View.GONE);
		emailString.setVisibility(View.GONE);
		emailInput.setVisibility(View.GONE);
		passwordString.setVisibility(View.GONE);
		passwordInput.setVisibility(View.GONE);
		OK.setVisibility(View.GONE);
		rememberMe.setVisibility(View.GONE);
		fastLoginTitle.setVisibility(View.VISIBLE);
		logInAs.setVisibility(View.VISIBLE);
		loginEmailAddress.setVisibility(View.VISIBLE);
		changeAccountString.setVisibility(View.VISIBLE);
		changeAccountButton.setVisibility(View.VISIBLE);
		shortcut.setVisibility(View.VISIBLE);

		loginEmailAddress.setText(email);
	}

	/**
	 * This method is called when the activity is resumed. (Also upon first start)
	 * The method checks if the sharedPreferences hold values for an email and password. If so, the shortcut view is shown.
	 */
	public void onResume() {
		super.onResume();
		if (checkInlogFile()) {
			retrieveInlogFile();
			setViewToShortCut();			
		}
	}

	/**
	 * This method is called when the OK button is clicked. The email and password will be sent to the server.
	 * The response will be read and appropriately handled.
	 */
	public void onOKClicked() {
		email = emailInput.getText().toString();
		password = passwordInput.getText().toString();
		createConnection();	
	}

	/**
	 * This method uses the ConnectionService to call a method, which sends the email and password to the server.
	 * @return server response
	 */
	private void createConnection() {
		if (!mService.isOnline()) {
			Resources.showToast(this, getResources().getString(R.string.not_connected), Gravity.CENTER, Toast.LENGTH_SHORT);
			continueWithoutConnection();
			return;
		}
		showProgressDialog();
		new AsyncLogin().execute(new String[]{"bla"});
	}

	/**
	 * This method handles the return string from the server. It does a simple check to establish that login has been successful. Otherwise it shows a Toast to notify that login has failed. 
	 * If the login was successful, the WBWListActivity will be started. If the checkbox was checked, the email and password will be remembered.
	 * @param back, which is the server return string.
	 */
	private void handleReturn(String back) {
		MyHtmlParser parser = new MyHtmlParser(back);
		if (parser.correctInputWBWLists()) {
			if (rememberMe.isChecked()) {
				createInlogFile();
			}
			Intent intent = new Intent(this,WBWListActivity.class);
			intent.putExtra("MyLists", back);
			startActivity(intent);
		} else {
			// If the user can continue, there is no need for further Toasts.
			if (continueWithoutConnection())
				return;
			if (back == null)
				Resources.showToast(this, getResources().getString(R.string.login_request_failed), Gravity.CENTER, Toast.LENGTH_SHORT);
			else if (parser.loginFailed())
				Resources.showToast(this, getResources().getString(R.string.login_data_incorrect), Gravity.CENTER, Toast.LENGTH_SHORT);
			else 
				Resources.showToast(this, getResources().getString(R.string.login_request_failed), Gravity.CENTER, Toast.LENGTH_SHORT);
		}			
	}

	/**
	 * If the user has no connection to the Internet, he might still use this app if previous data is available.
	 */
	private boolean continueWithoutConnection(){
		boolean fileExists = false;
		for (String x : fileList()) {
			if (x.equals(Resources.privateFile)) {
				fileExists = true;
				break;
			}
		}
		// Check whether the file containing the WBWList exists. If so, check whether the email and password are equal to the ones in the Shared Preferences. 
		if (fileExists && inlogDataEqualPrevious()) {
			Intent intent = new Intent(this,WBWListActivity.class);
			startActivity(intent);
			return true;
		} 
		return false;
	}

	/**
	 * This method checks for the current email and password fields if they are equal to the field in the Shared Preferences. 
	 * @return true if both are equal, false otherwise.
	 */
	private boolean inlogDataEqualPrevious() {
		if (email == null || password == null)
			return false;
		SharedPreferences settings = getSharedPreferences(Resources.inlogFile, MODE_PRIVATE);
		String cmpEmail = settings.getString("Email", null);
		String cmpPassword = settings.getString("Password", null);
		if (cmpEmail == null || cmpPassword == null)
			return false;
		if (cmpEmail.equals(email) && cmpPassword.equals(password))
			return true;
		return false;
	}

	/**
	 * Checks the file for the useInlogFile boolean.
	 * @return useInlogFile if it exists.
	 */
	private boolean checkInlogFile() {
		SharedPreferences settings = getSharedPreferences(Resources.inlogFile, MODE_PRIVATE);
		return settings.getBoolean("useInlogFile", false);		
	}

	/**
	 * Retrieve the email and password from file.
	 */
	private void retrieveInlogFile() {
		SharedPreferences settings = getSharedPreferences(Resources.inlogFile, MODE_PRIVATE);
		email = settings.getString("Email", null);
		password = settings.getString("Password", null);
	}

	/**
	 * Remove all sharedPreferences.
	 */
	private void removeSharedPreferences() {
		SharedPreferences.Editor editor = getSharedPreferences(Resources.inlogFile, MODE_PRIVATE).edit();
		editor.clear().commit();
	}

	/**
	 * This method will create a file, holding an email and password.
	 */
	private void createInlogFile() {
		/*SharedPreferences settings = getSharedPreferences(Resources.inlogFile, MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("useInlogFile", true);
		editor.putString("Email", email);
		editor.putString("Password", password);
		editor.commit();*/
	}

	/**
	 * This is a private ServiceConnection class. Only the mandatory methods are implemented. 
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mBound = true;
		}

		public void onServiceDisconnected(ComponentName name) {
			mService = null;
			mBound = false;			
		}
	};

	/**
	 * This method binds this activity to the ConnectionService. It uses a new thread, to make sure the activity can run alongside.
	 */
	private void bindToService() {
		Thread t = new Thread(){
			public void run(){
				Intent intent = new Intent(Intent.ACTION_SYNC, null, getApplicationContext(), ConnectionService.class);
				intent.putExtra("receiver", mReceiver);
				getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
				getApplicationContext().startService(intent);
			}
		};
		t.start();
	}	

	/**
	 * This private class, makes sure that the network connections are made on a separate thread.
	 * @author Vincent
	 *
	 */
	private class AsyncLogin extends AsyncTask<String, Void, String> {				
		protected String doInBackground(String... s) {
			String back = null;
			if (mBound && mService != null) {
				back = mService.logOnToSite(email,password);				
			}
			return back;
		}

		protected void onPostExecute(String back) {
			if (progressDialog != null && progressDialog.isShowing())
				progressDialog.dismiss();
			handleReturn(back);
		}		
	}

	/**
	 * Show progress dialog for when a server request is made.
	 */
	private void showProgressDialog(){
		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage(getResources().getString(R.string.loading));
		progressDialog.setCancelable(false);
		progressDialog.show();
	}

	public void onReceiveResult(int resultCode, Bundle resultData) {
		System.out.println("Ik krijg lekker wel wat..");
	}
}