package nl.vincentketelaars.wiebetaaltwat.views;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.vincentketelaars.wiebetaaltwat.R;
import nl.vincentketelaars.wiebetaaltwat.objects.MathNode;
import nl.vincentketelaars.wiebetaaltwat.objects.MathTree;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * This is the Calculator Dialog. It is a dialog that can perform basic addition, substraction, multiplication and division. The result can be send to the activity that creates the Calculator.
 * This dialog is implemented by using the buttons in a tableview. 
 * @author Vincent
 *
 */
public class Calculator extends Dialog implements OnClickListener {

	// Global variables
	private Context context;
	private EditText inputField;
	private Button b0;
	private Button b1;
	private Button b2;
	private Button b3;
	private Button b4;
	private Button b5;
	private Button b6;
	private Button b7;
	private Button b8;
	private Button b9;
	private Button bDot;
	private Button bPlus;
	private Button bMinus;
	private Button bTimes;
	private Button bDivide;
	private Button bIs;
	private Button bUse;
	private Button bBack;
	private Button bClear;
	private Button bCancel;

	/**
	 * Constructor takes only the context of the activity
	 * @param context
	 */
	public Calculator(Context context) {
		super(context);		
		this.context = context;
	}

	/** 
	 * Called when the activity is first created. Set the contentview and calls init.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.calculator);
		init();        
	}

	/**
	 * Set the title to Calculator, and set the Layout of the dialog. Calls the setViews with the dialog width.
	 */
	private void init() {
		DisplayMetrics DM = context.getResources().getDisplayMetrics();
		this.setTitle(context.getResources().getString(R.string.calculator));
		this.getWindow().setLayout(DM.widthPixels, LayoutParams.WRAP_CONTENT);
		setViews(DM.widthPixels);
	}

	/**
	 * This method only initializes the buttons, and sets their widths.
	 * @param widthPixels is the width of the view in pixels
	 */
	private void setViews(int widthPixels) {
		inputField = (EditText) this.findViewById(R.id.calc_edit_view);
		inputField.setInputType(InputType.TYPE_NULL);
		int bWidth = (int) Math.floor(widthPixels / 4.5);
		b0 = (Button) this.findViewById(R.id.calc_num_0);	b0.setWidth(bWidth);
		b1 = (Button) this.findViewById(R.id.calc_num_1);	b1.setWidth(bWidth);
		b2 = (Button) this.findViewById(R.id.calc_num_2);	b2.setWidth(bWidth);
		b3 = (Button) this.findViewById(R.id.calc_num_3);	b3.setWidth(bWidth);
		b4 = (Button) this.findViewById(R.id.calc_num_4);	b4.setWidth(bWidth);
		b5 = (Button) this.findViewById(R.id.calc_num_5);	b5.setWidth(bWidth);
		b6 = (Button) this.findViewById(R.id.calc_num_6);	b6.setWidth(bWidth);
		b7 = (Button) this.findViewById(R.id.calc_num_7);	b7.setWidth(bWidth);
		b8 = (Button) this.findViewById(R.id.calc_num_8);	b8.setWidth(bWidth);
		b9 = (Button) this.findViewById(R.id.calc_num_9);	b9.setWidth(bWidth);
		bDot = (Button) this.findViewById(R.id.calc_num_dot);	bDot.setWidth(bWidth);
		bPlus = (Button) this.findViewById(R.id.calc_op_plus);	bPlus.setWidth(bWidth);
		bMinus = (Button) this.findViewById(R.id.calc_op_minus);	bMinus.setWidth(bWidth);
		bTimes = (Button) this.findViewById(R.id.calc_op_times);	bTimes.setWidth(bWidth);
		bDivide = (Button) this.findViewById(R.id.calc_op_divide);	bDivide.setWidth(bWidth);
		bIs = (Button) this.findViewById(R.id.calc_op_is);	bIs.setWidth(bWidth);
		bUse = (Button) this.findViewById(R.id.calc_use);	bUse.setWidth(bWidth);
		bBack = (Button) this.findViewById(R.id.calc_back);		bBack.setWidth(bWidth);
		bClear = (Button) this.findViewById(R.id.calc_clear);	bClear.setWidth(bWidth);
		bCancel = (Button) this.findViewById(R.id.calc_cancel);		bCancel.setWidth(bWidth);
		b0.setOnClickListener(this);
		b1.setOnClickListener(this);
		b2.setOnClickListener(this);
		b3.setOnClickListener(this);
		b4.setOnClickListener(this);
		b5.setOnClickListener(this);
		b6.setOnClickListener(this);
		b7.setOnClickListener(this);
		b8.setOnClickListener(this);
		b9.setOnClickListener(this);
		bDot.setOnClickListener(this);
		bPlus.setOnClickListener(this);
		bMinus.setOnClickListener(this);
		bTimes.setOnClickListener(this);
		bDivide.setOnClickListener(this);
		bIs.setOnClickListener(this);
		bBack.setOnClickListener(this);
		bClear.setOnClickListener(this);
		bCancel.setOnClickListener(this);		
	}

	/**
	 * This method is called when a button is clicked. (Except the bUse button, which should be used in the parent activity)
	 * The bCancel button, cancels the dialog. The bBack button calls backInput. The bClear calls clearInput. The bIs button calls the checkInputForCalculation method. 
	 * All other buttons call the addText method with their respective values.
	 */
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.calc_cancel:
			this.cancel();
			break;
		case R.id.calc_back:
			backInput();
			break;
		case R.id.calc_clear:
			clearInput();
			break;
		case R.id.calc_op_plus:
			addText("+");
			break;
		case R.id.calc_op_minus:
			addText("-");
			break;
		case R.id.calc_op_times:
			addText("*");
			break;
		case R.id.calc_op_divide:
			addText("/");
			break;
		case R.id.calc_op_is:
			checkInputForCalculation(inputField.getText().toString());
			break;
		case R.id.calc_num_0:
			addText("0");
			break;
		case R.id.calc_num_1:
			addText("1");
			break;
		case R.id.calc_num_2:
			addText("2");
			break;
		case R.id.calc_num_3:
			addText("3");
			break;
		case R.id.calc_num_4:
			addText("4");
			break;
		case R.id.calc_num_5:
			addText("5");
			break;
		case R.id.calc_num_6:
			addText("6");
			break;
		case R.id.calc_num_7:
			addText("7");
			break;
		case R.id.calc_num_8:
			addText("8");
			break;
		case R.id.calc_num_9:
			addText("9");
			break;
		case R.id.calc_num_dot:
			addText(".");
			break;
		default:
			break;
		}		
	}

	/**
	 * This method removes the last character of the inputField string, if possible.
	 */
	private void backInput() {
		String in = inputField.getText().toString();
		if (in != null && in.length() != 0) {
			int pos = inputField.getSelectionStart();
			if (pos != 0) {
				inputField.setText(in.substring(0,pos-1)+in.substring(pos));
				inputField.setSelection(pos-1);
			}
		}
	}

	/**
	 * This method checks if the string in the inputField is a legitimate mathematical expression. If so it calls calculate.
	 */
	private void checkInputForCalculation(String in) {
		in = reduceText(in);
		// Check if the String is not null or empty.
		if (in != null && in.length() != 0) {
			Pattern p = Pattern.compile("-?((\\d+(\\.\\d+)?)|\\.\\d+)([\\+\\*\\/-]-?((\\d+(\\.\\d+)?)|\\.\\d+))*");		
			Matcher m = p.matcher(in);
			// Set found string, if a match occurs.
			if (m.find()) {
				String found = m.group();
				// If found is the same as the string in the inputField continue with calculating the expression. Otherwise show a toast.
				if (found.equals(in)) {
					inputField.setText(calculateWithTree(found));
					inputField.setSelection(inputField.getText().toString().length());
					return;
				}
			}			
			Toast.makeText(context, context.getResources().getString(R.string.calc_verify_input), Toast.LENGTH_LONG).show();
		} else {
			// Somewhere in the expression, there is a mistake. It could be useful to extend this for better support.
			Toast.makeText(context, context.getResources().getString(R.string.calc_input_empty), Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Reduce two minuses to one plus. Reduce plus minus to minus.
	 * @param input
	 * @return reduced string
	 */
	private String reduceText(String input) {
		while (input.contains("--")) {
			input = input.replace("--", "+");			
		}
		while (input.contains("+-")) {
			input = input.replace("+-", "-");			
		}
		return input;
	}
	
	/**
	 * Traverse the MathTree start at the left most node. If the children are numbers, perform an operation. If you have no children, but still an operator in the expression, 
	 * create a left and right child with the partial expressions, keep the operator.
	 * @param in
	 * @return
	 */
	private String calculateWithTree(String in) {
		// Find all minus operators
		Pattern p = Pattern.compile("\\d-");	
		Matcher m = p.matcher(in);
		while (m.find()) {
			String r = m.group();
			// Choose different character for minus operator
			in = in.replace(r, r.charAt(0)+"_");
		}
		MathTree tree = new MathTree();
		// Set rootNode
		MathNode node = new MathNode(null, in);
		tree.setRootNode(node);
		// While the rootNode is not a number, traverse the tree depth first from the left.
		while (!tree.getRootNode().isLeafNode()) {
			MathNode leftMost = tree.getLeftMostNode();
			if (leftMost.isOperator()) {
				// Remove children by performing operation.
				leftMost.performOperation();
			} else if (!leftMost.hasChildren())		{
				// Create children
				leftMost.createChildren();
			} else {
				// Something has failed.
				Toast.makeText(context, context.getResources().getString(R.string.calc_failed), Toast.LENGTH_LONG).show();
				break;
			}
		}
		// rootNode is now a value, return the expression
		return tree.getRootNode().getExp();
	}		

	/**
	 * This method retrieves the string from the inputField and adds the parameter add.
	 * @param add
	 */
	private void addText(String add) {
		String in = inputField.getText().toString();
		if (in != null) {
			int pos = inputField.getSelectionStart();
			String newText = in.substring(0,pos) + add + in.substring(pos);
			inputField.setText(newText);
			inputField.setSelection(pos + add.length());
		}
	}

	/**
	 * Makes the inputField empty.
	 */
	private void clearInput() {
		int posStart = inputField.getSelectionStart();
		int posEnd = inputField.getSelectionEnd();
		if (posStart == posEnd) {
			inputField.setText("");	
		} else {
			String in = inputField.getText().toString();
			inputField.setText(in.substring(0,posStart)+in.substring(posEnd));
			inputField.setSelection(posStart);
		}
	}

	/**
	 * This method checks if the inputField contains an actual double value. If so it returns true, otherwise fals 
	 * @return inputField has double value
	 */
	public boolean isDouble() {
		String in = inputField.getText().toString();
		if (in != null && in.length() != 0) {
			Pattern p = Pattern.compile("(-?\\d+(\\.\\d+)*)");		
			Matcher m = p.matcher(in);
			while (m.find()) {
				if (m.group().length() == in.length())
					return true;
			} 
		}
		return false;
	}

	/**
	 * Make sure to call isDouble() before you call this method. This method tries to parse the string in the inputField to a double.
	 * @return result of calculator.
	 */
	public double getResult() {
		String in = inputField.getText().toString();
		return Double.parseDouble(in);
	}

	/**
	 * Return the use Button, so that the parent activity can listen to it.
	 * @return Use Button
	 */
	public Button getUseButton() {
		return bUse;
	}
}
