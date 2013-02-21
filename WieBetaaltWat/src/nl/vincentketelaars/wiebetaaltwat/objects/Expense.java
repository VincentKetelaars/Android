package nl.vincentketelaars.wiebetaaltwat.objects;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class represents the object Expense. An Expense is a combination of a person that spends money. A description that describes where this money went. The amount of money spend.
 * The date of the recording of this spending and the participants in this spending.
 * @author Vincent
 *
 */
public class Expense implements Parcelable, Serializable {
	private String spender;
	private String description;
	private double amount;
	private String date;
	private MemberGroup participants;	
	private String tid;
	private String delete;
	private long lastUpdate;

	public Expense (String sp, String de, double am, String d, MemberGroup p, String tid, String delete) {
		setSpender(sp);
		setDescription(de);
		setAmount(am);
		setDate(d);
		setParticipants(p);
		setTid(tid);
		setDelete(delete);
		setLastUpdate(System.currentTimeMillis());
	}

	/**
	 * This constructor reads the instances from the Parcel.
	 * @param in
	 */
	public Expense (Parcel in) {
		readFromParcel(in);
	}

	public String getSpender() {
		return spender;
	}

	public void setSpender(String spender) {
		this.spender = spender;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public MemberGroup getParticipants() {
		return participants;
	}

	public String getTid() {
		return tid;
	}

	public void setTid(String tid) {
		this.tid = tid;
	}

	public String getDelete() {
		return delete;
	}

	public void setDelete(String delete) {
		this.delete = delete;
	}

	public void setParticipants(MemberGroup participants) {
		this.participants = participants;
	}

	public long getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(long lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
	
	public boolean equals(Expense e) {
		return tid.equals(e.getTid()) && spender.equals(e.getSpender()) && description.equals(e.getDescription()) && date.equals(e.getDate()) &&
				Math.abs(e.getAmount()-amount) < 0.01 && getParticipants().participantsHaveEqualCount(e.getParticipants());
	}

	/**
	 * This is a String representation of this Expense object.
	 */
	public String toString() {
		return "<(Spender: "+getSpender()+", Description: "+getDescription()+", Amount: "+getAmount()+", Date: "+getDate()+", Participants: "+participants.toString()+")>";
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
		dest.writeString(spender);
		dest.writeString(description);
		dest.writeDouble(amount);
		dest.writeString(date);
		dest.writeParcelable(participants, 0);
		dest.writeString(tid);
		dest.writeString(delete);
		dest.writeLong(lastUpdate);
	}

	/**
	 * This method assigns the values from the Parcel to the instances.
	 * @param in (Parcel)
	 */
	public void readFromParcel(Parcel in) {		
		spender = in.readString();
		description = in.readString();
		amount = in.readDouble();
		date = in.readString();
		participants = in.readParcelable(MemberGroup.class.getClassLoader());
		tid = in.readString();
		delete = in.readString();
		lastUpdate = in.readLong();
	}

	/**
	 * This method.....
	 */
	public static final Parcelable.Creator<Expense> CREATOR =
			new Parcelable.Creator<Expense>() {
		public Expense createFromParcel(Parcel in) {
			return new Expense(in);
		}

		public Expense[] newArray(int size) {
			return new Expense[size];
		}
	};
}
