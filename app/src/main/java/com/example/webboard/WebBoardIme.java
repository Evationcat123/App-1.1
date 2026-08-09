package com.example.webboard;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.view.inputmethod.InputConnection;
import java.util.Locale;

public class WebBoardIme extends InputMethodService {
    private static final int BG = Color.rgb(16,17,20), PANEL = Color.rgb(25,27,31), KEY = Color.rgb(38,41,47), SPECIAL = Color.rgb(52,56,64), TEXT = Color.WHITE, MUTED = Color.rgb(175,180,190);
    private LinearLayout root, keys;
    private WebView web;
    private EditText url;
    private boolean shift = false, symbols = false, webInputFocused = false;

    @Override public void onCreate() {
        super.onCreate();
        Window w = getWindow().getWindow();
        if (w != null) w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    @Override public View onCreateInputView() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(7),dp(7),dp(7),dp(5));
        root.addView(buildBrowserBar(), new LinearLayout.LayoutParams(-1,dp(48)));

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true);
        s.setSupportZoom(true); s.setBuiltInZoomControls(false); s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(false); s.setUseWideViewPort(false);
        web.setFocusable(true); web.setFocusableInTouchMode(true); web.setBackgroundColor(Color.WHITE);
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new BrowserBridge(), "WebBoard");
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view, String pageUrl){ super.onPageFinished(view,pageUrl); injectFocusBridge(); }
        });
        web.loadUrl("https://www.google.com/");
        root.addView(web,new LinearLayout.LayoutParams(-1,0,1f));

        keys = new LinearLayout(this); keys.setOrientation(LinearLayout.VERTICAL);
        root.addView(keys,new LinearLayout.LayoutParams(-1,dp(246)));
        buildKeys();
        return root;
    }

    private View buildBrowserBar(){
        LinearLayout bar=new LinearLayout(this); bar.setOrientation(LinearLayout.HORIZONTAL); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(dp(2),dp(2),dp(2),dp(2));
        url=new EditText(this); url.setSingleLine(true); url.setText("https://www.google.com/"); url.setTextColor(TEXT); url.setHintTextColor(MUTED); url.setHint("Adresse oder Suche"); url.setTextSize(14);
        url.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI); url.setShowSoftInputOnFocus(false); url.setPadding(dp(14),0,dp(8),0); url.setBackground(rounded(PANEL,24));
        url.setOnFocusChangeListener((v,has)->{if(has){webInputFocused=false;url.setSelection(url.length());}}); url.setOnEditorActionListener((v,id,e)->{navigate();return true;});
        bar.addView(url,new LinearLayout.LayoutParams(0,-1,1f));
        Button links=toolbarButton("🔗"); links.setContentDescription("Vorgefertigte Links"); links.setOnClickListener(v->showLinksMenu(v)); bar.addView(links,new LinearLayout.LayoutParams(dp(45),-1));
        Button go=toolbarButton("↗"); go.setOnClickListener(v->navigate()); bar.addView(go,new LinearLayout.LayoutParams(dp(45),-1));
        Button back=toolbarButton("‹"); back.setOnClickListener(v->{if(web.canGoBack())web.goBack();}); bar.addView(back,new LinearLayout.LayoutParams(dp(40),-1));
        Button forward=toolbarButton("›"); forward.setOnClickListener(v->{if(web.canGoForward())web.goForward();}); bar.addView(forward,new LinearLayout.LayoutParams(dp(40),-1));
        return bar;
    }

    private Button toolbarButton(String text){Button b=new Button(this);b.setText(text);b.setTextColor(TEXT);b.setTextSize(15);b.setAllCaps(false);b.setPadding(0,0,0,0);b.setBackground(rounded(PANEL,22));return b;}

    private void showLinksMenu(View anchor){
        LinearLayout menu=new LinearLayout(this);menu.setOrientation(LinearLayout.VERTICAL);menu.setPadding(dp(10),dp(10),dp(10),dp(10));menu.setBackground(rounded(Color.rgb(31,33,38),18));
        TextView title=new TextView(this);title.setText("Schnelllinks");title.setTextColor(TEXT);title.setTextSize(16);title.setPadding(dp(10),dp(5),dp(10),dp(8));menu.addView(title);
        addLink(menu,"▶  YouTube","https://www.youtube.com"); addLink(menu,"G  Google","https://www.google.com"); addLink(menu,"A  Asura Scans","https://www.asurascans.com");
        PopupWindow popup=new PopupWindow(menu,dp(250),dp(205),true);popup.setBackgroundDrawable(rounded(Color.rgb(31,33,38),18));popup.setOutsideTouchable(true);popup.setElevation(dp(10));popup.showAsDropDown(anchor,-dp(195),-dp(215));
    }
    private void addLink(LinearLayout menu,String label,String target){
        Button b=new Button(this);b.setText(label);b.setTextColor(TEXT);b.setTextSize(14);b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setAllCaps(false);b.setPadding(dp(12),0,dp(8),0);b.setBackground(rounded(Color.rgb(43,46,53),12));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.setMargins(0,dp(3),0,dp(3));menu.addView(b,p);
        b.setOnClickListener(v->{url.setText(target);url.setSelection(url.length());url.clearFocus();webInputFocused=false;web.loadUrl(target);web.requestFocus();});
    }

    private void navigate(){
        if(url==null||web==null)return;String q=url.getText().toString().trim();if(q.isEmpty())return;
        String target=q.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")?q:"https://www.google.com/search?q="+android.net.Uri.encode(q);
        webInputFocused=false;web.loadUrl(target);url.clearFocus();web.requestFocus();
    }

    private void buildKeys(){
        keys.removeAllViews();
        if(symbols){
            addRow("1234567890"); addRow("@#$%&*+-=/");
            LinearLayout r=row(); addKey(r,"ABC",1.2f,v->{symbols=false;buildKeys();}); addKey(r,"()[]{}",2f,v->type(((Button)v).getText().toString())); addKey(r,"!?;:'\"",2f,v->type(((Button)v).getText().toString())); addKey(r,"⌫",1.25f,v->delete()); keys.addView(r,new LinearLayout.LayoutParams(-1,0,1f));
        }else{
            addRow("qwertzuiopü"); addRow("asdfghjklöä");
            LinearLayout r=row(); addKey(r,"⇧",1.25f,v->{shift=!shift;buildKeys();});
            for(String c:new String[]{"y","x","c","v","b","n","m",",","."})addKey(r,c,1f,v->type(((Button)v).getText().toString()));
            addKey(r,"⌫",1.3f,v->delete()); keys.addView(r,new LinearLayout.LayoutParams(-1,0,1f));
        }
        LinearLayout bottom=row(); addKey(bottom,symbols?"ABC":"?123",1.25f,v->{symbols=!symbols;shift=false;buildKeys();}); addKey(bottom,"🔗",1f,v->showLinksMenu(v)); addKey(bottom,"🌐",1f,v->focusBrowserInput()); addKey(bottom,"Leertaste",4f,v->type(" ")); addKey(bottom,".",1f,v->type(".")); addKey(bottom,"↵",1.25f,v->enter()); keys.addView(bottom,new LinearLayout.LayoutParams(-1,0,1f));
    }
    private void focusBrowserInput(){webInputFocused=false;url.requestFocus();url.setSelection(url.length());}
    private LinearLayout row(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);return r;}
    private void addRow(String chars){LinearLayout r=row();for(int i=0;i<chars.length();i++){String c=String.valueOf(chars.charAt(i));addKey(r,c,1f,v->type(((Button)v).getText().toString()));}keys.addView(r,new LinearLayout.LayoutParams(-1,0,1f));}
    private void addKey(LinearLayout row,String label,float weight,View.OnClickListener listener){Button b=new Button(this);String shown=label;if(shift&&label.length()==1&&Character.isLetter(label.charAt(0)))shown=label.toUpperCase(Locale.GERMANY);b.setText(shown);b.setTextColor(TEXT);b.setTextSize(label.equals("Leertaste")?12:16);b.setAllCaps(false);b.setPadding(0,0,0,0);b.setBackground(rounded(label.equals("⇧")||label.equals("⌫")||label.equals("?123")||label.equals("ABC")?SPECIAL:KEY,10));b.setOnClickListener(listener);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,weight);p.setMargins(dp(2),dp(2),dp(2),dp(2));row.addView(b,p);}
    private GradientDrawable rounded(int color,int radiusDp){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radiusDp));return d;}

    private void type(String text){
        if(shift){text=text.toUpperCase(Locale.GERMANY);shift=false;buildKeys();}
        if(url!=null&&url.hasFocus()){int start=Math.max(0,url.getSelectionStart()),end=Math.max(0,url.getSelectionEnd());url.getText().replace(Math.min(start,end),Math.max(start,end),text);url.setSelection(Math.min(start,end)+text.length());return;}
        if(webInputFocused&&web!=null){web.evaluateJavascript("window.WebBoardInsert("+quoteJs(text)+");",null);return;}
        InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.commitText(text,1);
    }
    private void delete(){
        if(url!=null&&url.hasFocus()){int start=url.getSelectionStart(),end=url.getSelectionEnd();if(start!=end)url.getText().delete(Math.min(start,end),Math.max(start,end));else if(start>0)url.getText().delete(start-1,start);return;}
        if(webInputFocused&&web!=null){web.evaluateJavascript("window.WebBoardDelete();",null);return;}
        InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.deleteSurroundingText(1,0);
    }
    private void enter(){
        if(url!=null&&url.hasFocus()){navigate();return;}
        if(webInputFocused&&web!=null){web.evaluateJavascript("window.WebBoardEnter();",null);return;}
        InputConnection ic=getCurrentInputConnection();if(ic!=null){ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER));ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_ENTER));}
    }

    private void injectFocusBridge(){
        if(web==null)return;
        String js="(function(){if(window.__webboardInstalled)return;window.__webboardInstalled=true;"+
        "window.WebBoardInsert=function(t){var e=document.activeElement;if(!e)return;if(e.isContentEditable){document.execCommand('insertText',false,t);}else if('value'in e){var s=e.selectionStart==null?e.value.length:e.selectionStart,z=e.selectionEnd==null?s:e.selectionEnd,proto=Object.getPrototypeOf(e),d=Object.getOwnPropertyDescriptor(proto,'value');if(d&&d.set)d.set.call(e,e.value.slice(0,s)+t+e.value.slice(z));else e.value=e.value.slice(0,s)+t+e.value.slice(z);var p=s+t.length;if(e.setSelectionRange)e.setSelectionRange(p,p);e.dispatchEvent(new Event('input',{bubbles:true}));}};"+
        "window.WebBoardDelete=function(){var e=document.activeElement;if(!e)return;if(e.isContentEditable){document.execCommand('delete',false,null);}else if('value'in e){var s=e.selectionStart||0,z=e.selectionEnd||0;if(s===z&&s>0)s--;var proto=Object.getPrototypeOf(e),d=Object.getOwnPropertyDescriptor(proto,'value');if(d&&d.set)d.set.call(e,e.value.slice(0,s)+e.value.slice(z));else e.value=e.value.slice(0,s)+e.value.slice(z);if(e.setSelectionRange)e.setSelectionRange(s,s);e.dispatchEvent(new Event('input',{bubbles:true}));}};"+
        "window.WebBoardEnter=function(){var e=document.activeElement;if(!e)return;var ev=new KeyboardEvent('keydown',{key:'Enter',code:'Enter',keyCode:13,which:13,bubbles:true});e.dispatchEvent(ev);if(e.tagName==='TEXTAREA'||e.isContentEditable)window.WebBoardInsert('\\n');else if(e.form&&e.type!=='button')e.form.requestSubmit?e.form.requestSubmit():e.form.submit();};"+
        "document.addEventListener('focusin',function(ev){var e=ev.target;if(e&&((e.matches&&e.matches('input,textarea,select,[contenteditable=true]'))||e.isContentEditable))WebBoard.setWebInputFocused(true);},true);"+
        "document.addEventListener('focusout',function(){setTimeout(function(){var e=document.activeElement;if(!(e&&((e.matches&&e.matches('input,textarea,select,[contenteditable=true]'))||e.isContentEditable)))WebBoard.setWebInputFocused(false);},100);},true);"+
        "})();";
        web.evaluateJavascript(js,null);
    }
    private String quoteJs(String value){return "\""+value.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r")+"\"";}
    public class BrowserBridge{@JavascriptInterface public void setWebInputFocused(boolean focused){webInputFocused=focused;}}
    private int dp(int x){return(int)(x*getResources().getDisplayMetrics().density+0.5f);}
}
