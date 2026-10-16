package studio.sotto.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

/** A separate app UID provides a real external editor for IME verification. */
public final class EditorFixtureActivity extends Activity {
    @Override public void onCreate(Bundle state){super.onCreate(state);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(24,48,24,24);TextView title=new TextView(this);title.setText("Sotto integration editor");title.setTextSize(22);root.addView(title);
        TextView submits=new TextView(this);submits.setText("Submissions: 0");submits.setContentDescription("No messages submitted");root.addView(submits);
        EditText first=field(root,"Original text field",false),second=field(root,"Different text field",false);field(root,"Password field",true);setContentView(root);first.requestFocus();first.postDelayed(()->((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(first,InputMethodManager.SHOW_IMPLICIT),500);
    }
    private EditText field(LinearLayout root,String label,boolean password){EditText edit=new EditText(this);edit.setHint(label);edit.setContentDescription(label);edit.setInputType(password?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);edit.setMinLines(password?1:2);edit.setTextSize(18);edit.setOnEditorActionListener((v,action,event)->{TextView submits=(TextView)root.getChildAt(1);submits.setText("Unexpected submission");return true;});root.addView(edit,new LinearLayout.LayoutParams(-1,-2));return edit;}
}
