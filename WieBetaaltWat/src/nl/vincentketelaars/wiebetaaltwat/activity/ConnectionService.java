package nl.vincentketelaars.wiebetaaltwat.activity;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.StreamCorruptedException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.List;

import nl.vincentketelaars.wiebetaaltwat.objects.Expense;
import nl.vincentketelaars.wiebetaaltwat.objects.Member;
import nl.vincentketelaars.wiebetaaltwat.objects.MemberGroup;
import nl.vincentketelaars.wiebetaaltwat.objects.WBW;
import nl.vincentketelaars.wiebetaaltwat.objects.WBWList;
import nl.vincentketelaars.wiebetaaltwat.other.AdditionalKeyStoresSSLSocketFactory;
import nl.vincentketelaars.wiebetaaltwat.other.MyHtmlParser;
import nl.vincentketelaars.wiebetaaltwat.other.Resources;

import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;

/**
 * This Service is to remain active throughout the use of the application. Each activity should be able to bind to it. After binding, each activity can use any method needed,
 * in relation to connecting to the WBW website.
 * @author Vincent
 *
 */
public class ConnectionService extends Service {

	// Global instances
	private HttpClient client;	
	private IBinder mBinder;
	private WBW wbw;
	private WBW wbwInitialize;
	private int numberOfExpenses = 1000;
	private ResultReceiver mReceiver;
	private int[] requestCounter;


	/**
	 * This method is called upon creation of this Service.
	 * It merely instantiates the DefaultHttpClient and LocalBinder. Both are to remain throughout the application use.
	 */
	public void onCreate() {
		super.onCreate();
		HttpParams httpParameters = new BasicHttpParams();
		// Set the timeout in milliseconds until a connection is established.
		// The default value is zero, that means the timeout is not used. 
		int timeoutConnection = 3000;
		HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
		// Set the default socket timeout (SO_TIMEOUT) 
		// in milliseconds which is the timeout for waiting for data.
		int timeoutSocket = 5000;
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
		// Set maximum number of connections
		ConnManagerParams.setMaxTotalConnections(httpParameters, 100);
		
		 // Create and initialize scheme registry 
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", createAdditionalCertsSSLSocketFactory(), 443));
        // Create an HttpClient with the ThreadSafeClientConnManager.
        // This connection manager must be used if more than one thread will
        // be using the HttpClient.
        ClientConnectionManager cm = new ThreadSafeClientConnManager(httpParameters, schemeRegistry);
		client = new DefaultHttpClient(cm, httpParameters);
		mBinder = new LocalBinder();		
	}
	
	protected org.apache.http.conn.ssl.SSLSocketFactory createAdditionalCertsSSLSocketFactory() {
	    try {
	        final KeyStore ks = KeyStore.getInstance("BKS");

	        // the bks file we generated above
	        final InputStream in = this.getClass().getClassLoader().getResourceAsStream(Resources.bksFile);  
	        try {
	            // don't forget to put the password used above in strings.xml/mystore_password
	            ks.load(in, Resources.bksPassword.toCharArray());
	        } finally {
	            in.close();
	        }
	        return new AdditionalKeyStoresSSLSocketFactory(ks);

	    } catch( Exception e ) {
	        throw new RuntimeException(e);
	    }
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		mReceiver = intent.getParcelableExtra("receiver");
		System.out.println("Ik kom hier..");
		return START_STICKY;
	}

	/**
	 * This public Binder class.
	 * @author Vincent
	 *
	 */
	public class LocalBinder extends Binder {
		protected ConnectionService getService() {
			// Return this instance of LocalService so clients can call public methods
			System.out.println("Wouw, weer een Localbinder?");
			return ConnectionService.this;
		}
	}

	/**
	 * This mandatory method returns the LocalBinder, so that activities can actively bind to this Service.
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		System.out.println("He ik word weer gebonden..!");
		return mBinder;
	}


	public void initialize(String email, String password) {
		setWbw(new WBW(new ArrayList<WBWList>()));
		WBW temp = null;
		try {
		temp = inputWBW(Resources.privateFile);
		} catch (ClassCastException e) {
			Log.i("wbw", "Log file contains wrong object!");
		}
		if (temp != null && email.equals(temp.getEmail()) && password.equals(temp.getPassword())) {
			setWbw(temp);		
		} else {
			getWbw().setEmail(email);
			getWbw().setPassword(password);
		}

		// Get the current WBW
		wbwInitialize = new WBW(new ArrayList<WBWList>());
		requestCounter = new int[3];
		requestCounter[0] += 1;
		new AsyncLogin().execute(new String[]{email, password});
	}


	public String changeExpenseList(String lid, String sort, String page, String numResults){
		String url = "/index.php?page=balance&lid="+lid+"&p=&sort_column="+sort+"&rows="+numResults+"&p="+page+"#list";
		return retrieveHtmlPage(url);
	}

	/**
	 * Send an modified expense to the server with an http post. 
	 * @param lid
	 * @param spender
	 * @param description
	 * @param amount
	 * @param date
	 * @param members
	 * @return server result
	 */
	public String sendModifiedExpense(String tid, String lid, String spenderId, String description, String amount, String date, MemberGroup members){
		List<NameValuePair> arguments = new ArrayList<NameValuePair>();
		arguments = getExpenseArguments(lid, spenderId, description, amount, date, members);
		arguments.add(new BasicNameValuePair("action", "mod_transaction"));
		arguments.add(new BasicNameValuePair("tid", tid));
		return sendHttpPost(arguments);
	}

	/**
	 * Send an expense to the server with an http post. 
	 * @param lid
	 * @param spender
	 * @param description
	 * @param amount
	 * @param date
	 * @param members
	 * @return server result
	 */
	public String sendExpense(String lid, String spenderId, String description, String amount, String date, MemberGroup members){
		List<NameValuePair> arguments = new ArrayList<NameValuePair>();
		arguments = getExpenseArguments(lid, spenderId, description, amount, date, members);
		arguments.add(new BasicNameValuePair("action", "add_transaction"));
		return sendHttpPost(arguments);
	}

	/**
	 * This method returns all the arguments needed for an expense post request.
	 * @param lid
	 * @param spenderId
	 * @param description
	 * @param amount
	 * @param date
	 * @param members
	 * @return arguments
	 */
	public List<NameValuePair> getExpenseArguments(String lid, String spenderId, String description, String amount, String date, MemberGroup members){
		List<NameValuePair> arguments = new ArrayList<NameValuePair>();
		arguments.add(new BasicNameValuePair("lid", lid));
		arguments.add(new BasicNameValuePair("payment_by", spenderId));
		arguments.add(new BasicNameValuePair("description", description));
		arguments.add(new BasicNameValuePair("amount", amount.replace(".",",")));
		arguments.add(new BasicNameValuePair("date", date));
		for (Member m : members.getGroupMembers()) {
			arguments.add(new BasicNameValuePair("factor["+ m.getUid() +"]", Integer.toString(m.getCount())));
		}
		arguments.add(new BasicNameValuePair("submit_add", "Verwerken"));
		return arguments;
	}

	/**
	 * This method takes two parameters.
	 * @param email
	 * @param password
	 * It sends these parameters, accompanied by three other variables in a httppost. The DefaultHttpClient executes this post, and returns a response. This response is then handled by
	 * the getResponseBody method, and results in a string.
	 * @return the string returned by getResponseBody
	 */
	public String logOnToSite(String email, String password){
		List<NameValuePair> arguments = new ArrayList<NameValuePair>();
		arguments.add(new BasicNameValuePair("action", "login"));
		arguments.add(new BasicNameValuePair("username", email));
		arguments.add(new BasicNameValuePair("password", password));
		arguments.add(new BasicNameValuePair("remember_me", "1"));
		arguments.add(new BasicNameValuePair("login_submit", "submit"));
		return sendHttpPost(arguments);
	}

	/**
	 * Send Http post to index.php
	 * @param arguments
	 * @return result
	 */
	private String sendHttpPost(List<NameValuePair> arguments) {
		if (!isOnline())
			return null;
		HttpPost post = new HttpPost();
		String result = null;
		try{
			post.setURI(new URI(Resources.WBWUrl+"/index.php"));
			post.setEntity(new UrlEncodedFormEntity(arguments));
			HttpResponse response = client.execute(post);
			result = getResponseBody(response.getEntity());
			response.getEntity().consumeContent();
		}
		catch (URISyntaxException e){
			e.printStackTrace();
		} 
		catch (ClientProtocolException e) {
			e.printStackTrace();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * This method only expects one parameter, which is the url within the domain. Combined with the url of the domain itself, it represents the page that needs to be retrieved.
	 * @param html
	 * The response is transformed into a string by getResponseBody.
	 * @return the string returned by the server and transformed by getResponseBody
	 */
	public String retrieveHtmlPage(String html) {
		if (!isOnline())
			return null;
		HttpPost post = new HttpPost();

		String result = null;
		try{
			post.setURI(new URI(Resources.WBWUrl+html));
			HttpResponse response = client.execute(post);
			result = getResponseBody(response.getEntity());
			response.getEntity().consumeContent();
		}
		catch (URISyntaxException e){
			e.printStackTrace();
		} 
		catch (ClientProtocolException e) {
			e.printStackTrace();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * This method takes a HttpEntity and returns the string representation.
	 * @param entity
	 * @return string representation
	 * @throws IOException
	 * @throws ParseException
	 */
	private String getResponseBody(final HttpEntity entity) throws IOException, ParseException {
		if (entity == null) { throw new IllegalArgumentException("HTTP entity may not be null"); }
		InputStream instream = entity.getContent();
		if (instream == null) { return ""; }
		if (entity.getContentLength() > Integer.MAX_VALUE) { throw new IllegalArgumentException(
				"HTTP entity too large to be buffered in memory"); }
		String charset = getContentCharSet(entity);
		if (charset == null) {
			charset = HTTP.DEFAULT_CONTENT_CHARSET;
		}
		Reader reader = new InputStreamReader(instream, charset);
		StringBuilder buffer = new StringBuilder();
		try {
			char[] tmp = new char[1024];
			int l;
			while ((l = reader.read(tmp)) != -1) {
				buffer.append(tmp, 0, l);
			}
		} finally {
			reader.close();
		}
		return buffer.toString();
	}

	/**
	 * This method takes an HttpEntity and returns the charset of this entity.
	 * @param entity
	 * @return string 
	 * @throws ParseException
	 */
	private String getContentCharSet(final HttpEntity entity) throws ParseException {
		if (entity == null) { throw new IllegalArgumentException("HTTP entity may not be null"); }
		String charset = null;
		if (entity.getContentType() != null) {
			HeaderElement values[] = entity.getContentType().getElements();
			if (values.length > 0) {
				NameValuePair param = values[0].getParameterByName("charset");
				if (param != null) {
					charset = param.getValue();
				}
			}
		}
		return charset;
	}

	/**
	 * This method sends a request to the server to logout.
	 * @return logged out
	 */
	public String logOut() {
		String result = retrieveHtmlPage("/index.php?action=logout");
		return result;
	}

	/**
	 * This method checks whether the device has an internet connection. It returns false if there is no viable connection.
	 * @return
	 */
	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}

	/**
	 * This method sends an invitation.
	 * @param name
	 * @param email
	 * @param lid
	 * @return
	 */
	public String sendInvitation(String name, String email, String lid) {
		List<NameValuePair> arguments = new ArrayList<NameValuePair>();
		arguments.add(new BasicNameValuePair("page", "members"));
		arguments.add(new BasicNameValuePair("lid", lid));
		arguments.add(new BasicNameValuePair("action", "add_member"));
		arguments.add(new BasicNameValuePair("member_alias", name));
		arguments.add(new BasicNameValuePair("email", email));
		return sendHttpPost(arguments);
	}

	/**
	 * This method save an arraylist to a permanent file.
	 * @param file
	 * @param wbwlist
	 */
	private void outputWBW(String file, WBW w) {
		try {
			FileOutputStream fileOut = openFileOutput(file, Context.MODE_PRIVATE);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(w);
			out.close();
			fileOut.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method retrieves an arraylist from the permanent file.
	 * @param file
	 * @return
	 */
	private WBW inputWBW(String file) {
		WBW w = null;
		try {
			FileInputStream fileIn = openFileInput(file);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			w = (WBW) in.readObject();
			in.close();
			fileIn.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		return w;
	}

	public WBW getWbw() {
		return wbw;
	}

	public void setWbw(WBW wbw) {
		this.wbw = wbw;
	}

	/**
	 * This private class, makes sure that the network connections are made on a separate thread.
	 * @author Vincent
	 *
	 */
	private class AsyncLogin extends AsyncTask<String, Void, String> {				
		protected String doInBackground(String... s) {
			return logOnToSite(s[0], s[1]);
		}

		protected void onPostExecute(String back) {
			requestCounter[0] -= 1;
			MyHtmlParser parser = new MyHtmlParser(back);
			ArrayList<WBWList> temp = parser.parseTheWBWLists();
			wbwInitialize.setWbwLists(temp);
			if (wbwInitialize.getWbwLists() != null) {
				for (WBWList wL : wbwInitialize.getWbwLists()) {
					if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB) {
						new AsyncExpenses().executeOnExecutor(THREAD_POOL_EXECUTOR, new WBWList[]{wL});	
					} else {
						new AsyncExpenses().execute(new WBWList[]{wL});	
					}
					requestCounter[1] += 1; 
				}
				Bundle bundle = new Bundle();
				bundle.putParcelable("wbw", wbwInitialize);
				mReceiver.send(1, bundle);
				createInlogFile(wbwInitialize.getEmail(), wbwInitialize.getPassword());
			}			
		}		
	}

	/**
	 * This private class, makes sure that the network connections are made on a separate thread.
	 * @author Vincent
	 *
	 */
	private class AsyncExpenses extends AsyncTask<WBWList, Void, String> {
		WBWList wbw = null;
		protected String doInBackground(WBWList... w) {
			wbw = w[0];
			return retrieveHtmlPage(wbw.getHTML()+"&p=1&sort_column=timestamp&rows="+numberOfExpenses+"#list");
		}

		protected void onPostExecute(String back) {
			requestCounter[1] -= 1; 
			MyHtmlParser parser = new MyHtmlParser(back);
			boolean correctInput = parser.correctInputExpenses();	
			if (correctInput) {
				for (WBWList wL : wbwInitialize.getWbwLists()) {
					if (wL.getHTML().equals(wbw.getHTML())) {
						ArrayList<Expense> temp = parser.parseTheListOfExpenses();
						if (temp != null)
							wL.setExpenses(temp);
						MemberGroup members = parser.parseGroupMembers();
						if (members != null)
							wL.setGroupMembers(members);
						ArrayList<Integer> resultsPerPage = parser.getResultsPerPage();
						if (resultsPerPage != null) {
							wL.setNumResults(resultsPerPage.remove(0));	// The duplicate element is removed
							wL.setResultsPerPage(resultsPerPage);
						}
						wL.setPages(parser.getNumPages());
						if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB) {
							new AsyncRetrieveMemberStatus().executeOnExecutor(THREAD_POOL_EXECUTOR, new WBWList[]{wL});	
						} else {
							new AsyncRetrieveMemberStatus().execute(new WBWList[]{wL});	
						}
						requestCounter[2] += 1; 
					}
				}
				Bundle bundle = new Bundle();
				bundle.putParcelable("wbw", wbwInitialize);
				mReceiver.send(1, bundle);
			}
		}
	}

	/**
	 * This private class, makes sure that the network connections are made on a separate thread.
	 * @author Vincent
	 *
	 */
	private class AsyncRetrieveMemberStatus extends AsyncTask<WBWList, Void, String> {
		WBWList wbw = null;

		protected String doInBackground(WBWList... w) {
			wbw = w[0];
			return retrieveHtmlPage(wbw.getHTML().replace("balance","members"));
		}

		protected void onPostExecute(String back) {
			requestCounter[2] -= 1; 
			MyHtmlParser parser = new MyHtmlParser(back);
			for (WBWList wL : wbwInitialize.getWbwLists()) {
				if (wL.getHTML().equals(wbw.getHTML())) {
					wL.setGroupMembers(parser.parseEditGroup(wbw.getGroupMembers()));			
				}
			}
			if (requestCounter[2] == 0) outputWBW(Resources.privateFile, getWbw());
		}		
	}
	
	/**
	 * This method will create a file, holding an email and password.
	 */
	private void createInlogFile(String email, String password) {
		SharedPreferences settings = getSharedPreferences(Resources.inlogFile, MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("useInlogFile", true);
		editor.putString("Email", email);
		editor.putString("Password", password);
		editor.commit();
	}
}
