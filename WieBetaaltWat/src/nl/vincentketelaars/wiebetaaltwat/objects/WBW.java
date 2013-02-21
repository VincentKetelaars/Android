package nl.vincentketelaars.wiebetaaltwat.objects;

import java.io.Serializable;
import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class WBW implements Serializable, Parcelable {
		private ArrayList<WBWList> wbwLists;
		private String email;
		private String password;
		private long lastUpdate;
		
		public WBW (ArrayList<WBWList> l) {
			setWbwLists(l);
			setLastUpdate(System.currentTimeMillis());
		}
		
		/**
		 * This constructor reads the instances from the Parcel.
		 * @param in
		 */
		public WBW (Parcel in) {
			readFromParcel(in);
		}

		public ArrayList<WBWList> getWbwLists() {
			return wbwLists;
		}

		public void setWbwLists(ArrayList<WBWList> wbwLists) {
			this.wbwLists = wbwLists;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public long getLastUpdate() {
			return lastUpdate;
		}

		public void setLastUpdate(long lastUpdate) {
			this.lastUpdate = lastUpdate;
		}

		/**
		 * This is a mandatory method with the parcelable interface
		 */
		public int describeContents() {
			// TODO Auto-generated method stub
			return 0;
		}

		/**
		 * This method writes each instance to the Parcel. This method takes a Parcel, and some kind of flag integer.
		 */
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeList(getWbwLists());
			dest.writeString(email);
			dest.writeString(password);
			dest.writeLong(lastUpdate);
		}
		
		/**
		 * This method assigns the values from the Parcel to the instances.
		 * @param in (Parcel)
		 */
		public void readFromParcel(Parcel in) {	
			setWbwLists(in.readArrayList(WBWList.class.getClassLoader()));
			setEmail(in.readString());
			setPassword(in.readString());
			setLastUpdate(in.readLong());
		}

		/**
		 * This method.....
		 */
		public static final Parcelable.Creator<WBW> CREATOR =
				new Parcelable.Creator<WBW>() {
			public WBW createFromParcel(Parcel in) {
				return new WBW(in);
			}

			public WBW[] newArray(int size) {
				return new WBW[size];
			}
		};
		
		public boolean mergeWBWLists(ArrayList<WBWList> temp) {
			if (temp == null)
				return false;
			boolean success = true;
			for (WBWList w : temp) {
				for (WBWList x : wbwLists) {
					if (w.getHTML().equals(x.getHTML())) {
						success = x.mergeWBWList(w) && success;
						break;
					}
				}
				wbwLists.add(w); // New WBWList
				w.setLastUpdate(System.currentTimeMillis());
			}			
			lastUpdate = System.currentTimeMillis();
			for (WBWList x : wbwLists) {
				if (Math.abs(lastUpdate - x.getLastUpdate()) > 1000) // The updates should easily have been done in one second.
					wbwLists.remove(x); // Any wbwList not in temp has to be removed from the current WBW.
			}
			return success;
		}


}
