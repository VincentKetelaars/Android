package nl.vincentketelaars.wiebetaaltwat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import nl.vincentketelaars.wiebetaaltwat.objects.Member;
import nl.vincentketelaars.wiebetaaltwat.objects.MemberGroup;
import nl.vincentketelaars.wiebetaaltwat.objects.Resources;

import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;

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
		client = new DefaultHttpClient(httpParameters);
		mBinder = new LocalBinder();
	}
	
	/**
	 * This public Binder class.
	 * @author Vincent
	 *
	 */
    public class LocalBinder extends Binder {
        protected ConnectionService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ConnectionService.this;
        }
    }

    /**
     * This mandatory method returns the LocalBinder, so that activities can actively bind to this Service.
     */
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return mBinder;
	}
	
	
	public String changeExpenseList(String lid, String page, String numResults){
		String url = "/index.php?page=balance&lid="+lid+"&p=&sort_column=timestamp&rows="+numResults+"&p="+page+"#list";
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
			arguments.add(new BasicNameValuePair("factor["+ m.getId() +"]", Integer.toString(m.getCount())));
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
}
