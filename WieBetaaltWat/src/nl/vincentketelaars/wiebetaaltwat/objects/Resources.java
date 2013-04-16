package nl.vincentketelaars.wiebetaaltwat.objects;

import android.content.Context;
import android.widget.Toast;


public class Resources {
	// Private file
	public final static String privateFile = "my_private_file";
	// SharedPreferences
	public final static String inlogFile = "Inlog";
	// WBW Url
	public final static String WBWUrl = "http://www.wiebetaaltwat.nl";
	
	public final static String bksFile = "res/raw/mynewkey3.bks";
	public final static String bksPassword = "pass12";
	
	public static void showToast(Context context, String text, int position, int duration) {
		Toast toast = Toast.makeText(context, text, duration);
		toast.setGravity(position, 0, 0);
		toast.show();
	}

}
