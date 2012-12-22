package copy.any.instant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ServerContentRetriever {

	private String username;
	private String authName;
	private String authPass;
	private Resources resources;

	private String urlObject = "http://www.maxhovens.nl/copies/Users/";

	public ServerContentRetriever(String user, Resources res) {
		username = user;
		urlObject += username+"/";
		resources = res;
		getAuth();
	}

	public String getString(String f) {
		String response = "";
		try {
			URL url = new URL(urlObject+f);			
			HttpURLConnection conn= (HttpURLConnection) url.openConnection();
			conn.setDoInput(true);
			final String serverAuth = authName + ":" + authPass;
			System.out.println(serverAuth);
			final String serverAuthBase64 = MyBase64.encode(serverAuth.getBytes());
			conn.setRequestProperty("Authorization", "Basic " + serverAuthBase64);

			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = rd.readLine()) != null) {
				response+=line+"\n";
			}
			rd.close();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 	
		return response;
	}

	public Bitmap getImage(String f) {
		Bitmap bmImg = null;
		URL myFileUrl =null; 
		try {
			myFileUrl= new URL(urlObject+f);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			HttpURLConnection conn= (HttpURLConnection) myFileUrl.openConnection();
			conn.setDoInput(true);
			final String serverAuth = authName + ":" + authPass;
			final String serverAuthBase64 = MyBase64.encode(serverAuth.getBytes());
			conn.setRequestProperty("Authorization", "Basic " + serverAuthBase64);
			conn.connect();
			InputStream is = conn.getInputStream();

			bmImg = BitmapFactory.decodeStream(is);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bmImg;
	}	
	
	private void getAuth() {		
        InputStream iS = resources.openRawResource(R.raw.server_authorization);  
        Scanner sc = new Scanner(iS);
        authName = sc.next();
        authPass = sc.next();
	}
}
