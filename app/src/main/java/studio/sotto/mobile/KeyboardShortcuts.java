package studio.sotto.mobile;

import org.json.JSONObject;
import java.util.Locale;

/** Output-language commands are active only during an explicit voice recording. */
final class KeyboardShortcuts {
    static JSONObject defaults(){try{return new JSONObject().put("b","Bangla").put("e","English");}catch(Exception impossible){throw new IllegalStateException(impossible);}}
    static String key(String value){String key=value==null?"":value.trim().toLowerCase(Locale.ROOT);if(!key.matches("[a-z]"))throw new IllegalArgumentException("Choose one letter from A to Z.");return key;}
    static JSONObject assign(JSONObject current,String previous,String letter,String language)throws Exception{
        String key=key(letter),name=language==null?"":language.trim();if(name.isEmpty()||name.length()>50||name.contains("\n"))throw new IllegalArgumentException("Enter a language name, up to 50 characters.");
        JSONObject next=new JSONObject(current.toString());if(!key.equals(previous)&&next.has(key))throw new IllegalArgumentException("That letter already has a language. Edit its existing shortcut.");if(previous!=null&&!previous.isEmpty())next.remove(previous);next.put(key,name);return next;
    }
    static String resolve(JSONObject bindings,String value,boolean enabled,boolean recording,boolean sensitive){if(!enabled||!recording||sensitive)return "";try{return bindings.optString(key(value),"");}catch(IllegalArgumentException ignored){return "";}}
}
