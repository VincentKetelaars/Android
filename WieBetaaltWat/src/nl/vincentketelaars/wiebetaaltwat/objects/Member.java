package nl.vincentketelaars.wiebetaaltwat.objects;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

public class Member implements Parcelable, Serializable {
	private String name;
	private double balance;
	private int count;
	private int uid;
	private String email;
	private int activated; // -1 is not initialized, 0 is not activated, 1 is activated
	private long lastUpdate;

	public Member(String name) {
		setName(name);
		setBalance(Double.MAX_VALUE); // This value should never be used!
		setCount(0);
		setUid(-1);
		setEmail(null);
		setActivated(-1);
		setLastUpdate(System.currentTimeMillis());
	}
	
	public Member(String name, int count) {
		setName(name);
		setBalance(Double.MAX_VALUE); // This value should never be used!
		setCount(count);
		setUid(-1);
		setEmail(null);
		setActivated(-1);
		setLastUpdate(System.currentTimeMillis());
	}

	public Member(String name, double amount) {
		setName(name);
		setBalance(amount);
		setCount(0);
		setUid(-1);
		setEmail(null);
		setActivated(-1);
		setLastUpdate(System.currentTimeMillis());
	}
	
	public Member(String name, double amount, String email, int id, int activated) {
		setName(name);
		setBalance(amount);
		setCount(0);
		setUid(id);
		setEmail(email);
		setActivated(activated);
		setLastUpdate(System.currentTimeMillis());
	}
	
	public Member(String name, double amount, int count, int id) {
		setName(name);
		setBalance(amount);
		setCount(count);
		setUid(id);
		setEmail(null);
		setActivated(-1);
		setLastUpdate(System.currentTimeMillis());
	}

	/**
	 * This constructor reads the instances from the Parcel.
	 * @param in
	 */
	public Member (Parcel in) {
		readFromParcel(in);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getBalance() {
		return balance;
	}

	public void setBalance(double balance) {
		this.balance = balance;
	}
	
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getUid() {
		return uid;
	}

	public void setUid(int id) {
		this.uid = id;
	}

	public String toString() {
		return "<("+getName()+", "+getBalance()+", "+getCount()+", "+getUid()+", "+getEmail()+", "+getActivated()+")>";
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public int getActivated() {
		return activated;
	}

	public void setActivated(int activated) {
		this.activated = activated;
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
		return 0;
	}

	/**
	 * This method writes each instance to the Parcel. This method takes a Parcel, and some kind of flag integer.
	 */
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeDouble(balance);
		dest.writeInt(count);
		dest.writeInt(uid);
		dest.writeString(email);
		dest.writeInt(activated);
		dest.writeLong(lastUpdate);
	}

	/**
	 * This method assigns the values from the Parcel to the instances.
	 * @param in (Parcel)
	 */
	public void readFromParcel(Parcel in) {		
		name = in.readString();
		balance = in.readDouble();
		count = in.readInt();
		uid = in.readInt();
		email = in.readString();
		activated = in.readInt();
		lastUpdate = in.readLong();
	}

	/**
	 * This method.....
	 */
	public static final Parcelable.Creator<Member> CREATOR =
			new Parcelable.Creator<Member>() {
		public Member createFromParcel(Parcel in) {
			return new Member(in);
		}

		public Member[] newArray(int size) {
			return new Member[size];
		}
	};

	public boolean mergeMember(Member m) {
		setBalance(m.getBalance());
		if (m.getCount() != 0)
			setCount(m.getCount());
		if (m.getUid() != -1)
			setUid(m.getUid());
		if (m.getEmail() != null)
			setEmail(m.getEmail());
		if (m.getActivated() != -1)
			setActivated(m.getActivated());
		setLastUpdate(System.currentTimeMillis());		
		return false;
	}

}
