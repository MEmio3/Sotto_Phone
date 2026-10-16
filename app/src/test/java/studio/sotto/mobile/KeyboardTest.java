package studio.sotto.mobile;

import org.json.JSONObject;
import org.junit.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class KeyboardTest {
    private OfflineSpelling spelling()throws Exception{return new OfflineSpelling(new InputStreamReader(new FileInputStream("src/main/assets/english-words.txt"),StandardCharsets.UTF_8));}
    @Test public void shortcutsAreVoiceCommandsNotNormalTyping()throws Exception{JSONObject bindings=KeyboardShortcuts.defaults();assertEquals("Bangla",KeyboardShortcuts.resolve(bindings,"B",true,true,false));assertEquals("",KeyboardShortcuts.resolve(bindings,"b",true,false,false));assertEquals("",KeyboardShortcuts.resolve(bindings,"b",false,true,false));assertEquals("",KeyboardShortcuts.resolve(bindings,"b",true,true,true));assertEquals("",KeyboardShortcuts.resolve(bindings,"!",true,true,false));}
    @Test public void customLettersPersistAndConflictsAreRejected()throws Exception{JSONObject original=KeyboardShortcuts.defaults(),edited=KeyboardShortcuts.assign(original,"b","x","Spanish");assertEquals("Bangla",original.getString("b"));assertFalse(edited.has("b"));assertEquals("Spanish",new JSONObject(edited.toString()).getString("x"));for(String value:new String[]{"e","ab","1",""})try{KeyboardShortcuts.assign(original,null,value,"French");fail("Invalid or duplicate key");}catch(IllegalArgumentException expected){assertFalse(expected.getMessage().isEmpty());}}
    @Test public void offlineCorrectionsPreserveRealWordsNumbersAndBangla()throws Exception{OfflineSpelling dictionary=spelling();assertEquals("the",dictionary.correction("teh"));assertEquals("receive",dictionary.correction("recieve"));assertEquals("Hello",dictionary.correction("Helllo"));for(String word:new String[]{"hello","professional","বাংলা","GPT","42","test@example.com"})assertEquals(word,dictionary.correction(word));}
    @Test public void completionsAndCurrentWordAreBounded()throws Exception{OfflineSpelling dictionary=spelling();assertTrue(dictionary.suggestions("hel").contains("hello"));assertTrue(dictionary.suggestions("teh").contains("the"));assertEquals("can't",OfflineSpelling.trailingWord("I can't"));assertEquals("বাংলা",OfflineSpelling.trailingWord("English বাংলা"));assertEquals("",OfflineSpelling.trailingWord("hello "));assertTrue(dictionary.suggestions("recieve").size()<=3);}
}
