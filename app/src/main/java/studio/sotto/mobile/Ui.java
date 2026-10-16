package studio.sotto.mobile;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

final class Ui {
    static int dp(Context c,float value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
    static int color(Context c,int resource){return c.getColor(resource);}
    static LinearLayout column(Context c){LinearLayout view=new LinearLayout(c);view.setOrientation(LinearLayout.VERTICAL);return view;}
    static final class ContentColumn extends LinearLayout {
        ContentColumn(Context c){super(c);setOrientation(VERTICAL);}
        @Override protected void onMeasure(int width,int height){super.onMeasure(MeasureSpec.makeMeasureSpec(Math.min(MeasureSpec.getSize(width),dp(getContext(),760)),MeasureSpec.getMode(width)),height);}
    }
    static LinearLayout row(Context c){LinearLayout view=new LinearLayout(c);view.setGravity(Gravity.CENTER_VERTICAL);view.setOrientation(LinearLayout.HORIZONTAL);return view;}
    static TextView text(Context c,String value,String role){TextView view=new TextView(c);int style=role.equals("headline")?R.style.TextAppearance_Sotto_Headline:role.equals("title")?com.google.android.material.R.style.TextAppearance_Material3_TitleLarge:role.equals("label")?com.google.android.material.R.style.TextAppearance_Material3_LabelLarge:role.equals("caption")?com.google.android.material.R.style.TextAppearance_Material3_BodySmall:R.style.TextAppearance_Sotto_Body;view.setTextAppearance(style);view.setTypeface(ResourcesCompat.getFont(c,R.font.manrope),role.equals("headline")||role.equals("label")?Typeface.BOLD:Typeface.NORMAL);view.setFontVariationSettings("'wght' "+(role.equals("headline")?650:role.equals("title")||role.equals("label")?600:400));view.setText(value);view.setTextColor(color(c,role.equals("caption")?R.color.on_surface_variant:R.color.on_surface));if(android.os.Build.VERSION.SDK_INT>=29)view.setBreakStrategy(android.graphics.text.LineBreaker.BREAK_STRATEGY_HIGH_QUALITY);return view;}
    static void space(LinearLayout parent,int height){View view=new View(parent.getContext());parent.addView(view,new LinearLayout.LayoutParams(1,dp(parent.getContext(),height)));}
    static void add(LinearLayout parent,View child,int marginBottom){LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.bottomMargin=dp(parent.getContext(),marginBottom);parent.addView(child,params);}
    static MaterialButton button(Context c,String label,String icon,boolean outlined){MaterialButton button=new MaterialButton(c,null,outlined?com.google.android.material.R.attr.materialButtonOutlinedStyle:com.google.android.material.R.attr.materialButtonStyle);button.setText(label);button.setAllCaps(false);button.setMinHeight(dp(c,48));button.setMinimumHeight(dp(c,48));button.setCornerRadius(dp(c,28));button.setInsetTop(0);button.setInsetBottom(0);button.setTextSize(14);button.setTypeface(ResourcesCompat.getFont(c,R.font.manrope));button.setFontVariationSettings("'wght' 600");if(icon!=null){button.setIcon(new Icon(icon,color(c,outlined?R.color.on_surface:R.color.on_primary)));button.setIconSize(dp(c,20));}return button;}
    static MaterialButton icon(Context c,String label,String icon){MaterialButton button=button(c,"",icon,true);button.setContentDescription(label);button.setTooltipText(label);button.setMinWidth(dp(c,48));button.setMinimumWidth(dp(c,48));button.setPadding(dp(c,12),0,dp(c,12),0);button.setIconPadding(0);return button;}
    static MaterialCardView card(Context c,LinearLayout content,boolean recorder){MaterialCardView card=new MaterialCardView(c);card.setRadius(dp(c,20));card.setCardElevation(0);card.setStrokeWidth(recorder?0:dp(c,1));card.setStrokeColor(color(c,R.color.outline_variant));card.setCardBackgroundColor(color(c,recorder?R.color.recorder_surface:R.color.surface));content.setPadding(dp(c,20),dp(c,20),dp(c,20),dp(c,20));card.addView(content);return card;}
    static class Icon extends Drawable {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final String name;private ColorStateList tint;private int size=24;
        Icon withSize(int value){size=value;return this;}
        @Override public void setTint(int color){setTintList(ColorStateList.valueOf(color));}
        @Override public void setTintList(ColorStateList colors){tint=colors;onStateChange(getState());}
        @Override public boolean isStateful(){return tint!=null&&tint.isStateful();}
        @Override protected boolean onStateChange(int[] state){if(tint==null)return false;int next=tint.getColorForState(state,tint.getDefaultColor());if(paint.getColor()==next)return false;paint.setColor(next);invalidateSelf();return true;}
        Icon(String name,int color){this.name=name;paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.8f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);}
        public void draw(@NonNull Canvas canvas){canvas.save();canvas.translate(getBounds().left,getBounds().top);canvas.scale(getBounds().width()/24f,getBounds().height()/24f);Path p=new Path();
            switch(name){
                case "mic":canvas.drawRoundRect(9,2,15,15,3,3,paint);p.moveTo(5,10);p.lineTo(5,12);p.cubicTo(5,21,19,21,19,12);p.lineTo(19,10);p.moveTo(12,19);p.lineTo(12,23);break;
                case "history":canvas.drawCircle(12,12,9,paint);p.moveTo(12,7);p.lineTo(12,12);p.lineTo(16,14);break;
                case "plug":p.moveTo(8,2);p.lineTo(8,8);p.moveTo(16,2);p.lineTo(16,8);p.moveTo(5,8);p.lineTo(19,8);p.lineTo(19,10);p.cubicTo(19,19,5,19,5,10);p.close();p.moveTo(12,18);p.lineTo(12,23);break;
                case "settings":canvas.drawCircle(12,12,8,paint);canvas.drawCircle(12,12,3,paint);for(int i=0;i<8;i++){double a=i*Math.PI/4;canvas.drawLine(12+(float)Math.cos(a)*8,12+(float)Math.sin(a)*8,12+(float)Math.cos(a)*11,12+(float)Math.sin(a)*11,paint);}break;
                case "copy":canvas.drawRoundRect(8,8,21,22,2,2,paint);p.moveTo(15,8);p.lineTo(15,2);p.lineTo(2,2);p.lineTo(2,15);p.lineTo(8,15);break;
                case "arrow":p.moveTo(4,12);p.lineTo(20,12);p.moveTo(14,6);p.lineTo(20,12);p.lineTo(14,18);break;
                case "stop":canvas.drawRoundRect(6,6,18,18,2,2,paint);break;
                case "close":p.moveTo(6,6);p.lineTo(18,18);p.moveTo(18,6);p.lineTo(6,18);break;
                case "plus":p.moveTo(12,5);p.lineTo(12,19);p.moveTo(5,12);p.lineTo(19,12);break;
                case "check":p.moveTo(4,12);p.lineTo(10,18);p.lineTo(21,5);break;
                case "back":p.moveTo(19,5);p.lineTo(8,5);p.lineTo(2,12);p.lineTo(8,19);p.lineTo(22,19);p.lineTo(22,5);p.close();p.moveTo(12,9);p.lineTo(18,15);p.moveTo(18,9);p.lineTo(12,15);break;
                case "keyboard":canvas.drawRoundRect(1,5,23,20,2,2,paint);for(int i=0;i<3;i++)for(int j=0;j<7;j++)canvas.drawPoint(4+j*2.6f,8+i*3,paint);p.moveTo(7,18);p.lineTo(17,18);break;
                case "more":canvas.drawCircle(12,5,1,paint);canvas.drawCircle(12,12,1,paint);canvas.drawCircle(12,19,1,paint);break;
                default:canvas.drawCircle(12,12,8,paint);
            }canvas.drawPath(p,paint);canvas.restore();}
        public void setAlpha(int alpha){paint.setAlpha(alpha);}public void setColorFilter(android.graphics.ColorFilter filter){paint.setColorFilter(filter);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}public int getIntrinsicWidth(){return size;}public int getIntrinsicHeight(){return size;}
    }
    static final class Wave extends View {
        private float level;private boolean active;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final int tint;
        Wave(Context c,boolean inverse){super(c);tint=color(c,inverse?R.color.recorder_muted:R.color.on_surface_variant);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        void level(float level){this.level=level;invalidate();}void active(boolean value){active=value;invalidate();}
        protected void onDraw(Canvas canvas){super.onDraw(canvas);paint.setColor(tint);int bars=Math.min(39,Math.max(9,getWidth()/dp(getContext(),8)));float step=getWidth()/(float)bars;float phase=active?SystemClock.uptimeMillis()/150f:0;for(int i=0;i<bars;i++){float envelope=1-Math.abs(i-(bars-1)/2f)/(bars/2f);float height=dp(getContext(),3)+(getHeight()*.7f)*level*envelope*(.4f+.6f*(float)Math.abs(Math.sin(i*.91+phase)));float x=(i+.5f)*step;canvas.drawRoundRect(x-dp(getContext(),1.2f),(getHeight()-height)/2,x+dp(getContext(),1.2f),(getHeight()+height)/2,dp(getContext(),2),dp(getContext(),2),paint);}if(active&&level>.01f&&android.provider.Settings.Global.getFloat(getContext().getContentResolver(),android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1)>0)postInvalidateDelayed(32);}
    }
}
