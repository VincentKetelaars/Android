package nl.vincentketelaars.wiebetaaltwat.objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MathNode {
	
	private MathNode parent;
	private MathNode left;
	private MathNode right;
	private boolean leafNode = false;
	private int value;
	
	private String exp;
	
	public MathNode(MathNode p, String e) {
		setParent(p);
		setExp(e);
		if (isDouble()) {
			setLeafNode(true);
			setValue(getIntFromDouble());
		}
		setLeft(null);
		setRight(null);
	}
	
	public int getIntFromDouble() {
		double d = toDouble();
		return setValue((int) Math.round(d * 100));
	}

	public boolean hasChildren() {
		if (left == null && right == null)
			return false;
		return true;
	}
	
	public boolean isDouble() {
		if (exp == null)
			return false;
		Pattern p = Pattern.compile("-?((\\d+(\\.\\d+)?)|\\.\\d+)");	
		Matcher m = p.matcher(exp);	
		if (m.find()) {
			if (exp.equals(m.group()))
				return true;
		}
		return false;
	}
	
	public double toDouble() {
		try {
			return Double.parseDouble(exp);
		} catch (Exception e) {
			System.out.println(e);
		}
		return Double.MAX_VALUE;
	}

	public String getExp() {
		return exp;
	}

	public void setExp(String exp) {
		this.exp = exp;
	}

	public MathNode getRight() {
		return right;
	}

	public void setRight(MathNode right) {
		this.right = right;
	}

	public MathNode getLeft() {
		return left;
	}

	public void setLeft(MathNode left) {
		this.left = left;
	}
	
	public void removeRight() {
		right = null;
	}
	
	public void removeLeft() {
		left = null;
	}
	
	public boolean hasLeft() {
		return left != null;
	}
	
	public boolean hasRight() {
		return right != null;
	}
	
	public boolean hasParent() {
		return parent != null;
	}

	public MathNode getParent() {
		return parent;
	}

	public void setParent(MathNode parent) {
		this.parent = parent;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(" , exp = ");
		sb.append(exp);
		sb.append(" , leafNode = ");
		sb.append(leafNode);
		return sb.toString();
	}

	public boolean isLeafNode() {
		return leafNode;
	}

	public void setLeafNode(boolean leafNode) {
		this.leafNode = leafNode;
	}
	
	public boolean childrenAreLeafs() {
		if (hasChildren() && !isLeafNode()) {
			if (left.isLeafNode() && right.isLeafNode())
				return true;
		}
		return false;
	}
	
	public boolean isOperator() {
		if (exp.equals("+") || exp.equals("_") || exp.equals("*") || exp.equals("/"))
			return true;
		return false;
	}
	
	public void performOperation() {
		if (childrenAreLeafs()) {
			if (exp.equals("+")) {
				value = getLeft().getValue() + getRight().getValue();
			} else if (exp.equals("_")) {
				value = getLeft().getValue() - getRight().getValue();
			} else if (exp.equals("*")) {
				value = getLeft().getValue() * getRight().getValue() / 100;
			} else if (exp.equals("/")) {
				value = getLeft().getValue() * 100 / getRight().getValue();
			}
			setLeafNode(true);
			setRight(null);
			setLeft(null);
			setExp(intToDoubleString(value));
		}
	}
	
	private String intToDoubleString(int value) {
		String result = "";
		// Set the result back in the inputField
		int cents = value % 100;
		if (cents > 0) {
			result = value / 100 +"."+ cents;
		} else if (cents < 0) { // the modulus of a negative number is either zero or negative
			result = value / 100+"."+ -cents;
			if (value / 100 == 0)
				result = "-0."+ -cents;
		} else {
			result = Integer.toString(value / 100);
		}
		return result;
	}
	
	public void createChildren() {
		int max = 0;
		if (exp.contains("+") || exp.contains("_")) {
			int lPlus = exp.lastIndexOf("+");
			int lMin = exp.lastIndexOf("_");
			max = Math.max(lPlus, lMin);
		} else if(exp.contains("*") || exp.contains("/")) {
			int lTimes = exp.lastIndexOf("*");
			int lDivide = exp.lastIndexOf("/");
			max = Math.max(lTimes, lDivide);
		} 
		MathNode newLeft = new MathNode(this, exp.substring(0, max));
		MathNode newRight = new MathNode(this, exp.substring(max + 1));
		this.setLeft(newLeft);
		this.setRight(newRight);
		exp = exp.substring(max, max+1);
	}

	public int getValue() {
		return value;
	}

	public int setValue(int value) {
		this.value = value;
		return value;
	}	
}
