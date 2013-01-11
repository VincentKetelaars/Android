package nl.vincentketelaars.wiebetaaltwat.objects;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

public class Member implements Parcelable, Serializable {
	private String member;
	private double balance;
	private int count;
	private int id;

	public Member(String name) {
		setMember(name);
		setBalance(Double.MAX_VALUE); // This value should never be used!
		setCount(0);
		setId(-1);
	}
	
	public Member(String name, int count) {
		setMember(name);
		setBalance(Double.MAX_VALUE); // This value should never be used!
		setCount(count);
		setId(-1);
	}

	public Member(String name, double amount) {
		setMember(name);
		setBalance(amount);
		setCount(0);
		setId(-1);
	}
	
	public Member(String name, double amount, int count, int id) {
		setMember(name);
		setBalance(amount);
		setCount(count);
		setId(id);
	}

	/**
	 * This constructor reads the instances from the Parcel.
	 * @param in
	 */
	public Member (Parcel in) {
		readFromParcel(in);
	}

	public String getMember() {
		return member;
	}

	public void setMember(String member) {
		this.member = member;
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

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String toString() {
		return "<("+getMember()+", "+getBalance()+", "+getCount()+", "+getId()+")>";
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
		dest.writeString(member);
		dest.writeDouble(balance);
		dest.writeInt(count);
		dest.writeInt(id);
	}

	/**
	 * This method assigns the values from the Parcel to the instances.
	 * @param in (Parcel)
	 */
	public void readFromParcel(Parcel in) {		
		member = in.readString();
		balance = in.readDouble();
		count = in.readInt();
		id = in.readInt();
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

}
