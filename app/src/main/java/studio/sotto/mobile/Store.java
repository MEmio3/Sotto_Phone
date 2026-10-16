package studio.sotto.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class Store {
    private final Context context;
    public final SharedPreferences settings;
    private JSONArray connections, history;
    private JSONObject vault;
    public Store(Context context) throws Exception {
        this.context = context.getApplicationContext(); settings = this.context.getSharedPreferences("sotto", Context.MODE_PRIVATE);
        connections = new JSONArray(settings.getString("connections", "[]"));
        history = new JSONArray(read("history.json", "[]")); vault = new JSONObject(read("credentials.json", "{}"));
        boolean recovered=false;
        for (int i=0;i<history.length();i++) { JSONObject entry=history.getJSONObject(i); if ("processing".equals(entry.optString("status"))) { entry.put("status","failed"); entry.put("error","Sotto closed before processing finished. Retry this recording."); recovered=true; } }
        if (recovered) write("history.json", history.toString());
    }
    public synchronized JSONArray connections() throws Exception { return new JSONArray(connections.toString()); }
    public synchronized JSONObject connection(String id) { for(int i=0;i<connections.length();i++){JSONObject c=connections.optJSONObject(i);if(c!=null&&id.equals(c.optString("id")))return c;} return null; }
    public synchronized JSONObject active() { return connection(settings.getString("activeId", "")); }
    public String mode() { String value=settings.getString("writingMode","precise");return DictationRules.validMode(value)?value:"precise"; }
    public String language() { return settings.getString("languageOverride", ""); }
    public JSONObject keyboardShortcuts(){try{return new JSONObject(settings.getString("keyboardShortcuts",KeyboardShortcuts.defaults().toString()));}catch(Exception ignored){return KeyboardShortcuts.defaults();}}
    public void saveShortcut(String previous,String letter,String language)throws Exception{settings.edit().putString("keyboardShortcuts",KeyboardShortcuts.assign(keyboardShortcuts(),previous,letter,language).toString()).apply();}
    public void removeShortcut(String key){JSONObject values=keyboardShortcuts();values.remove(key);settings.edit().putString("keyboardShortcuts",values.toString()).apply();}
    public synchronized void saveConnection(JSONObject data, JSONObject secret) throws Exception {
        String id=data.optString("id", ""); if(id.isEmpty()){id=UUID.randomUUID().toString();data.put("id",id);}
        JSONArray next=new JSONArray();for(int i=0;i<connections.length();i++)if(!id.equals(connections.getJSONObject(i).optString("id")))next.put(connections.get(i)); next.put(data);
        if(secret!=null)saveSecret(id,secret);
        connections=next;settings.edit().putString("connections",next.toString()).apply();
        if(active()==null)select(id);
    }
    public void select(String id) { if(connection(id)==null)throw new IllegalArgumentException("Connection not found."); settings.edit().putString("activeId",id).apply(); }
    public synchronized void deleteConnection(String id) throws Exception {
        JSONArray next=new JSONArray();for(int i=0;i<connections.length();i++)if(!id.equals(connections.getJSONObject(i).optString("id")))next.put(connections.get(i));connections=next;
        vault.remove(id);write("credentials.json",vault.toString());settings.edit().putString("connections",next.toString()).apply();
        if(id.equals(settings.getString("activeId","")))settings.edit().putString("activeId",next.length()>0?next.getJSONObject(0).getString("id"):"").apply();
    }
    private SecretKey key() throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        String alias="sotto.credentials.v1"; if(store.containsAlias(alias))return (SecretKey)store.getKey(alias,null);
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
    public synchronized void saveSecret(String id, JSONObject value) throws Exception {
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        byte[] encrypted=cipher.doFinal(value.toString().getBytes(StandardCharsets.UTF_8));
        vault.put(id,new JSONObject().put("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)).put("data",Base64.encodeToString(encrypted,Base64.NO_WRAP)));write("credentials.json",vault.toString());
    }
    public synchronized JSONObject secret(String id) throws Exception {
        JSONObject entry=vault.optJSONObject(id);if(entry==null)return new JSONObject();
        try {Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(entry.getString("iv"),Base64.NO_WRAP)));
            return new JSONObject(new String(cipher.doFinal(Base64.decode(entry.getString("data"),Base64.NO_WRAP)),StandardCharsets.UTF_8));
        } catch(Exception e){throw new Exception("This device could not unlock the saved connection. Reconnect in Connections.");}
    }
    public synchronized JSONArray history() throws Exception { return new JSONArray(history.toString()); }
    public synchronized JSONObject entry(String id) {for(int i=0;i<history.length();i++){JSONObject e=history.optJSONObject(i);if(e!=null&&id.equals(e.optString("id")))return e;}return null;}
    public synchronized void putHistory(JSONObject entry) throws Exception {
        JSONArray next=new JSONArray();next.put(new JSONObject(entry.toString()));for(int i=0;i<history.length();i++)if(!entry.getString("id").equals(history.getJSONObject(i).optString("id")))next.put(history.get(i));history=next;write("history.json",next.toString());
    }
    public synchronized void deleteHistory(String id) throws Exception {
        JSONObject entry=entry(id);if(entry!=null&&!entry.optString("audio").isEmpty())audioFile(entry.optString("audio")).delete();
        JSONArray next=new JSONArray();for(int i=0;i<history.length();i++)if(!id.equals(history.getJSONObject(i).optString("id")))next.put(history.get(i));history=next;write("history.json",history.toString());
    }
    public File audioFile(String id) throws Exception { if(!id.matches("[A-Za-z0-9-]+\\.wav"))throw new Exception("Invalid recording name."); File directory=new File(context.getFilesDir(),"recordings"); if(!directory.exists()&&!directory.mkdirs())throw new IOException("Could not create recording storage.");return new File(directory,id); }
    public String saveAudio(String id,byte[] bytes) throws Exception {String name=id+".wav";try(FileOutputStream out=new FileOutputStream(audioFile(name))){out.write(bytes);}return name;}
    public byte[] readAudio(String name) throws Exception {return bytes(new FileInputStream(audioFile(name)),16000000);}
    private String read(String name,String fallback) throws Exception {AtomicFile file=new AtomicFile(new File(context.getFilesDir(),name));if(!file.getBaseFile().exists()&&!new File(file.getBaseFile()+".bak").exists())return fallback;try(InputStream in=file.openRead()){return new String(bytes(in,50000000),StandardCharsets.UTF_8);} }
    private void write(String name,String text) throws Exception {AtomicFile file=new AtomicFile(new File(context.getFilesDir(),name));FileOutputStream out=null;try{out=file.startWrite();out.write(text.getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}catch(Exception e){if(out!=null)file.failWrite(out);throw e;}}
    public static byte[] bytes(InputStream in,int limit) throws Exception {try(InputStream input=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))!=-1){if(out.size()+n>limit)throw new IOException("Response or recording exceeded the supported size.");out.write(buffer,0,n);}return out.toByteArray();}}
}
