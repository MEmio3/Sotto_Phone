package studio.sotto.mobile;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public final class SottoApplication extends Application {
    final SignInForeground signInForeground=new SignInForeground();
    @Override public void onCreate(){super.onCreate();int theme=getSharedPreferences("sotto",MODE_PRIVATE).getInt("theme",AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);AppCompatDelegate.setDefaultNightMode(theme);}
}
