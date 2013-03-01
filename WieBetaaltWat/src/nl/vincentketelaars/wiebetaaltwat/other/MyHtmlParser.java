package nl.vincentketelaars.wiebetaaltwat.other;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.vincentketelaars.wiebetaaltwat.objects.Expense;
import nl.vincentketelaars.wiebetaaltwat.objects.Member;
import nl.vincentketelaars.wiebetaaltwat.objects.MemberGroup;
import nl.vincentketelaars.wiebetaaltwat.objects.WBWList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.util.Log;

public class MyHtmlParser {

	private String input;
	private Document doc;
	private ArrayList<String> errors;

	public MyHtmlParser(String in) {
		setInput(in);
		setErrors(new ArrayList<String>());
	}

	/**
	 * This is the html parser method. It returns a list of WBWLists. It uses the Jsoup html parser. It also calls a method to retrieve the headers.
	 * @param input
	 * @return
	 */
	public ArrayList<WBWList> parseTheWBWLists ()  {		
		if (!correctInputWBWLists())
			return null;
		ArrayList<WBWList> result = new ArrayList<WBWList>();		
		Elements view_lists = doc.getElementsByClass("view-lists");
		Element profile = doc.getElementById("login-panel-body");
		Elements h3 = profile.getElementsByTag("h3");
		String name = h3.get(0).text();
		Elements table_view_lists_tr = view_lists.get(1).getElementsByTag("tr");
		for (int i = 0; i < table_view_lists_tr.size(); i++) {
			if (i >= 2) {
				Elements td = table_view_lists_tr.get(i).getElementsByTag("td");
				String list = td.get(0).text();
				String html = td.get(0).child(0).attr("href");
				double myBalance = Double.parseDouble(td.get(1).child(0).text().substring(2).replace(",","."));
				double highBalance = Double.parseDouble(td.get(2).child(0).text().substring(2).replace(",", "."));
				double lowBalance = Double.parseDouble(td.get(3).child(0).text().substring(2).replace(",", "."));
				String highName = td.get(2).ownText();
				String lowName = td.get(3).ownText();
				Pattern p = Pattern.compile("lid=(\\d+)");
				Matcher m = p.matcher(html);
				String lid = "";
				if (m.find())
					lid =m.group().substring(4);
				WBWList wbwList = new WBWList(html, list, new Member(name,myBalance), new Member(highName, highBalance), new Member(lowName, lowBalance), lid);
				result.add(wbwList);
			}
		}
		return result;
	}

	/**
	 * Check if the input is null, or if the page has reported errors.
	 * @return true if input is ok, otherwise false
	 */
	private boolean correctInput() {
		if (input==null)
			return false;
		doc = Jsoup.parse(getInput());
		if (doc==null)
			return false;
		return true;
	}

	/**
	 * Check if the title of the html page corresponds to the WBWList.
	 * @return
	 */
	public boolean correctInputWBWLists() {
		if (!correctInput())
			return false;
		if (hasErrors())
			return false;
		String title = retrieveTitle();
		if (title != null && title.equals("Wiebetaaltwat.nl : Mijn lijsten"))
			return true;
		Log.i("WBWList", "Retrieved the wrong page");
		return false;
	}

	/**
	 * This method parses a html page. It expects a list of expenses, and returns exactly that.
	 * @param input
	 * @return
	 */
	public ArrayList<Expense> parseTheListOfExpenses ()  {	
		if (!correctInputExpenses())
			return null;
		ArrayList<Expense> result = new ArrayList<Expense>();
		Element table_list = doc.getElementById("list");
		Elements tbody = table_list.getElementsByTag("tbody");
		Elements tr = tbody.get(0).getElementsByTag("tr");
		for (int i = 0; i < tr.size(); i++) {
			// If the expense in question has been deleted, do not add it to the list.
			if (tr.get(i).attr("class").equals("deleted"))
				continue;
			Elements td = tr.get(i).getElementsByTag("td");
			if (td.size() >= 5) {
				String spender = td.get(0).text();
				String description = td.get(1).text();
				double amount = Double.parseDouble(td.get(2).text().substring(2).replace(",", "."));
				String date = td.get(3).text();
				String[] members = td.get(4).text().split(", ");
				ArrayList<Member> participants = new ArrayList<Member>();
				Pattern px = Pattern.compile("\\s\\d+x");
				for (int j = 0; j < members.length; j++) {
					Member participant = new Member(members[j],1);
					Matcher mx = px.matcher(members[j]);
					if (mx.find()) {
						String find = mx.group();
						int count = Integer.parseInt(find.substring(1,find.length()-1));
						participant.setName(members[j].substring(0,members[j].length()-find.length()));
						participant.setCount(count);						
					}
					participants.add(participant);
				}
				Elements a = td.get(5).getElementsByTag("a");
				String delete = a.get(1).attr("href");
				Pattern p = Pattern.compile("tid=(\\d)+");
				Matcher m = p.matcher(delete);
				String tid = null;
				if (m.find())
					tid = m.group().substring(4);
				Expense expense = new Expense(spender, description, amount, date, new MemberGroup(participants), tid, delete);
				result.add(expense);
			}
		}
		return result;
	}

	/**
	 * This method parses a html page for the ID's of members.
	 * @param memberGroup
	 * @return
	 */
	public MemberGroup parseAddExpense(MemberGroup memberGroup) {			
		if (memberGroup == null)
			return null;
		if (!correctInputAddExpense())
			return null;
		Element paymentBy = doc.getElementById("payment_by");
		Elements options = paymentBy.getElementsByTag("option");
		for (int i = 0; i < options.size(); i++) {
			int id = Integer.parseInt(options.get(i).attr("value"));
			String name = options.get(i).text();
			for (Member m : memberGroup.getGroupMembers()) {
				if (m.getName().equals(name)) {
					m.setUid(id);				}
			}
		}
		return memberGroup;
	}
	
	/**
	 * This method parses a html page for the emails of the members.
	 * @param memberGroup
	 * @return
	 */
	public MemberGroup parseEditGroup(MemberGroup memberGroup) {			
		if (memberGroup == null)
			return null;
		if (!correctInputEditGroup())
			return null;
		Elements membersAdmin = doc.getElementsByClass("members-admin");
		Elements tr = membersAdmin.get(0).getElementsByTag("tr");
		for (int i = 1; i < tr.size(); i++) {
			Elements input = tr.get(i).getElementsByTag("input");
			boolean notActive = tr.get(i).getElementsByTag("td").get(0).attr("class").contains("not-activated");			
			int id = Integer.parseInt(input.get(0).attr("value"));
			String name = input.get(3).attr("value");
			String email = input.get(4).attr("value");
			for (Member m : memberGroup.getGroupMembers()) {
				if (m.getName().equals(name)) {
					m.setEmail(email);
					m.setUid(id);
					if (notActive)
						m.setActivated(0);
					else 
						m.setActivated(1);
				}
			}
		}
		return memberGroup;
	}
	
	public ArrayList<MemberGroup> parseGroupLists() {
		if (!correctInputAddExpense())
			return null;
		ArrayList<MemberGroup> groupLists = new ArrayList<MemberGroup>();
		Elements listGroupsClass = doc.getElementsByClass("list-groups");
		Element listGroupsList = listGroupsClass.get(listGroupsClass.size()-1);
		Elements groups = listGroupsList.getElementsByTag("a");
		for (int x = 2; x < groups.size(); x++) {
			String onclick = groups.get(x).attr("onclick");
			if (onclick != null) {
				ArrayList<Member> members = new ArrayList<Member>();
				Pattern p = Pattern.compile("\\d+");
				Matcher m = p.matcher(onclick);
				while (m.find()) {
					int id =  Integer.parseInt(m.group());
					m.find();
					int count = Integer.parseInt(m.group());
					members.add(new Member(null, Double.POSITIVE_INFINITY, count, id));
				}
				MemberGroup mg = new MemberGroup(members);
				mg.setGroupName(groups.get(x).text());
				groupLists.add(mg);
			}
		}
		return groupLists;
	}

	/**
	 * This method checks whether there is a class called status-error and logs those errors.
	 * @return true if there are status errors, otherwise false
	 */
	public boolean hasErrors() {
		if (doc == null)
			return false;
		Elements es = doc.getElementsByClass("status-error");
		if (es != null && es.size() != 0) {
			getErrors().add(es.text());
			Log.i("Error-parsing", es.text());
			return true;
		}
		return false;
	}

	/**
	 * This method checks the input string for html code. It checks if it has the correct title of the ExpenseList.
	 * @param input
	 * @return true if code checks out, otherwise return false
	 */
	public boolean correctInputExpenses() {
		if (!correctInput())
			return false;
		if (hasErrors())
			return false;
		String title = retrieveTitle();
		if (title != null && title.equals("Wiebetaaltwat.nl : Overzicht"))
			return true;
		Log.i("ExpenseList", "Retrieved the wrong page");
		return false;
	}

	/**
	 * This method checks the input string for html code. It checks if it has the correct title off the AddExpense.
	 * @param input
	 * @return true if code checks out, otherwise return false
	 */
	public boolean correctInputAddExpense() {
		if (!correctInput())
			return false;
		if (hasErrors())
			return false;
		String title = retrieveTitle();
		if (title != null && title.equals("Wiebetaaltwat.nl : Invoer"))
			return true;
		Log.i("AddExpense", "Retrieved the wrong page");
		return false;
	}
	
	/**
	 * This method checks the input string for html code. It checks if it has the correct title off the EditGroup.
	 * @param input
	 * @return true if code checks out, otherwise return false
	 */
	public boolean correctInputEditGroup() {
		if (!correctInput())
			return false;
		if (hasErrors())
			return false;
		String title = retrieveTitle();
		if (title != null && title.equals("Wiebetaaltwat.nl : Deelnemers"))
			return true;
		Log.i("AddExpense", "Retrieved the wrong page");
		return false;
	}
	
	/**
	 * This method checks the input string for html code. It checks if it has the correct title of the ExpenseList.
	 * @param input
	 * @return true if code checks out, otherwise return false
	 */
	public boolean correctParticipantsPage() {
		if (!correctInput())
			return false;
		if (hasErrors())
			return false;
		String title = retrieveTitle();
		if (title != null && title.equals("Wiebetaaltwat.nl : Deelnemers"))
			return true;
		Log.i("Participants", "Retrieved the wrong page");
		return false;
	}

	/**
	 * This takes a html page. It returns the title of the page or null.
	 * @param in
	 * @return
	 */
	private String retrieveTitle() {
		if (!correctInput())
			return null;
		Elements titles = doc.getElementsByTag("title");
		if (titles == null || titles.size() == 0)
			return null;
		Element title = titles.get(0);
		if (title != null) {
			String titleString = title.html();
			return titleString;
		}
		return null;
	}

	/**
	 * This method parses the members of the group and returns
	 * @param html document
	 * @return
	 */
	public MemberGroup parseGroupMembers() {
		if (!correctInputExpenses())
			return null;
		Element userBalance = doc.getElementById("user-balance");
		Element memberBalances = doc.getElementById("list-members-balance");
		ArrayList<Member> members = new ArrayList<Member>();
		Elements h3 = userBalance.getElementsByTag("h3");
		String me = h3.get(0).text();
		double myBalance = Double.parseDouble(userBalance.getElementsByTag("strong").get(0).text().substring(2).replace(",", "."));
		members.add(new Member(me, myBalance));
		for (Element e : memberBalances.getElementsByTag("p")) {
			Elements span = e.getElementsByTag("span");
			String member = span.get(0).text();
			double balance = Double.parseDouble(e.child(1).text().substring(2).replace(",", "."));
			members.add(new Member(member, balance));
		}		
		return new MemberGroup(members);
	}

	/**
	 * This method checks whether the login was successful, by checking the title of the response. 
	 * @return boolean login failed
	 */
	public boolean loginFailed() {
		String title = retrieveTitle();
		if (title != null && title.equals("Wiebetaaltwat.nl : Houd gezamenlijke uitgaven overzichtelijk!")){
			Document doc = Jsoup.parse(input);
			Elements es = doc.getElementsByClass("status-error");
			getErrors().add(es.text());
			Log.i("Login", es.get(0).text());
			return true;
		}				
		return false;
	}

	/**
	 * This method checks whether you were logged out, by checking the title of the response. 
	 * @return boolean logged out
	 */
	public boolean loggedOut() {
		String title = retrieveTitle();
		if (title != null) {
			if (title.equals("Wiebetaaltwat.nl : Uitloggen") || title.equals("Wiebetaaltwat.nl")) {
				return true;
			} else if (hasErrors()) {
				return true; // Is dit ok?
			} else {
				Element panel = doc.getElementById("login-panel-body");
				Elements h2 = panel.getElementsByTag("h2");
				if (h2 != null && !h2.isEmpty()) {
					String text = h2.get(0).text();
					if (text.equals("Inloggen"))
						return true;
				}
			}
		}
		return false;
	}

	/**
	 * This method returns a statusNotice if it is there.
	 * @return statusNotice element
	 */
	private Element statusNotice() {
		if (!correctInput())
			return null;
		if (hasErrors())
			return null;
		Elements statusNotices  = doc.getElementsByClass("status-notice");
		if (statusNotices != null && statusNotices.size() > 0) {
			Elements p = statusNotices.get(0).getElementsByTag("p");
			if (p != null && p.size() > 0)
				return p.get(0);
		}
		return null;
	}

	/**
	 * This returns the statusNotice message.
	 * @return message string;
	 */
	public String statusNoticeMessage() {
		Element p = statusNotice();
		if (p != null) {
			return p.text();
		}
		return null;
	}

	/**
	 * Retrieve the number of pages which contain results. 
	 * @return number of pages
	 */
	public int getNumPages() {
		Element p = doc.getElementById("page_input");
		if (p != null) {
			String t = p.parent().text();
			Pattern pat = Pattern.compile("\\d+");
			Matcher m = pat.matcher(t);
			if (m.find())
				return Integer.parseInt(m.group());
		}
		return 1;
	}

	/**
	 * Parses the results per page, and returns an ArrayList with these numbers. If this fails it will return null. The first element is a duplicate and represents the selected value.
	 * @return ArrayList of results per page.
	 */
	public ArrayList<Integer> getResultsPerPage() {
		Element r = doc.getElementById("rows_select");
		if (r != null) {
			ArrayList<Integer> results = new ArrayList<Integer>();
			Elements option = r.getElementsByTag("option");
			for (int i = 0; i < option.size(); i++) {
				String value = option.get(i).attr("value");
				if (option.get(i).hasAttr("selected"))
					results.add(0, Integer.parseInt(value));
				results.add(Integer.parseInt(value));
			}
			return results;
		}
		return null;
	}
	
	/**
	 * This method parses the participant page for all participants (active or not)
	 * @return MemberGroup of participants
	 */
	public MemberGroup getListParticipants() {
		if (!correctParticipantsPage())
			return null;
		ArrayList<Member> result = new ArrayList<Member>();
		Elements membersAdmin = doc.getElementsByClass("members-admin");
		Elements tr = membersAdmin.get(0).getElementsByTag("tr");
		for (int i = 1; i < tr.size(); i++) {
			int active = 0;
			if (tr.get(i).hasClass("active"))
				active = 1;
			Elements input = tr.get(i).getElementsByTag("input");
			int id = Integer.parseInt(input.get(0).attr("value"));
			String name = input.get(3).attr("value");
			String email = input.get(4).attr("value");
			Member member = new Member(name, 0, email, id, active);
			result.add(member);
		}		
		return new MemberGroup(result);
	}

	/**
	 * If the list is not empty, this method will return the last appended String.
	 * @return string representation of error.
	 */
	public String getLastError() {
		if (errors == null || errors.isEmpty())
			return null;
		else 
			return errors.get(errors.size()-1);
	}

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	public ArrayList<String> getErrors() {
		return errors;
	}

	public void setErrors(ArrayList<String> errors) {
		this.errors = errors;
	}
}
