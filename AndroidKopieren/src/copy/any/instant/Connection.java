package copy.any.instant;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.content.res.Resources;


public class Connection {		
	// Instance fields
	private String url;
	private JSONObject json;
	private String ServerReceiptString = "JSON";
	private String authName;
	private String authPass;
	private Resources resources;


	/**
	 * @param s is a url string
	 * @param j is information to be send to via this url.
	 */
	public Connection (String s,JSONObject j, Resources res) {
		url = s;
		json = j;
		resources = res;
		getAuth();
	}


	/**
	 * This method creates a hashmap with info for the serverside using the json object. 
	 * Then it doPost creates a HttpResponse which in is transformed to string.
	 * @return the output string, received from the server.
	 */
	public String unprotectedConnection() {
		Map<String, String> kvPairs = new HashMap<String, String>();
		kvPairs.put(ServerReceiptString, json.toString());		
		String temp = "";

		try {
			HttpResponse re = doPost(kvPairs);
			temp = EntityUtils.toString(re.getEntity());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return temp;
	}


	/**
	 * Creates a HttpClient and HttpPost. 
	 * @param kvPairs
	 * @return Httpresponse
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private HttpResponse doPost(Map<String, String> kvPairs) throws ClientProtocolException, IOException{
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(url); 
		final String serverAuth = authName + ":" + authPass;
		final String serverAuthBase64 = MyBase64.encode(serverAuth.getBytes());
		httppost.setHeader("Authorization", "Basic " + serverAuthBase64);

		if (kvPairs != null && !kvPairs.isEmpty()) {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(kvPairs.size());
			String k, v;
			Iterator<String> itKeys = kvPairs.keySet().iterator(); 

			while (itKeys.hasNext()) {
				k = itKeys.next();
				v = kvPairs.get(k);
				nameValuePairs.add(new BasicNameValuePair(k, v));
			} 

			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		} 

		HttpResponse response;
		response = httpclient.execute(httppost);
		httpclient.getConnectionManager().shutdown();
		System.out.println(response);
		return response;
	}	
	
	private void getAuth() {		
        InputStream iS = resources.openRawResource(R.raw.server_authorization);  
        Scanner sc = new Scanner(iS);
        authName = sc.next();
        authPass = sc.next();
	}
}

