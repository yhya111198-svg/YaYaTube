package com.jaw.tube;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.Intent;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Build;
import android.view.View;
import android.view.Gravity;
import android.widget.*;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.YoutubeDLResponse;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private final ExecutorService pool=Executors.newSingleThreadExecutor();
    private EditText urlInput;
    private TextView status, title;
    private Spinner quality;
    private ProgressBar bar;
    private Button inspect, download;
    private final ArrayList<String> options = new ArrayList<>();
    private volatile boolean ready=false, busy=false;
    private String videoUrl="";
    private final int navy=Color.rgb(12,19,37), cyan=Color.rgb(0,190,205);

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(22),dp(30),dp(22),dp(22)); root.setBackgroundColor(navy);
        ScrollView scroll=new ScrollView(this); scroll.addView(root); setContentView(scroll);
        TextView logo=text("▶ YaYaTube",32,Color.WHITE,true);root.addView(logo);
        TextView subtitle=text("تحميل فيديوهاتك المسموح بتنزيلها • بدون إعلانات",14,0xffa9b8cd,false); root.addView(subtitle);
        space(root,24);
        urlInput=new EditText(this); urlInput.setSingleLine(true); urlInput.setTextColor(Color.WHITE);urlInput.setHintTextColor(0xff8796ac);urlInput.setHint("الصق رابط الفيديو هنا");urlInput.setInputType(17);urlInput.setPadding(dp(12),dp(12),dp(12),dp(12));urlInput.setBackground(bg(0xff25334c,14));root.addView(urlInput,new LinearLayout.LayoutParams(-1,dp(56)));
        space(root,15);
        inspect=button("عرض الجودات المتاحة");root.addView(inspect,new LinearLayout.LayoutParams(-1,dp(52)));
        space(root,22);
        title=text("عنوان الفيديو يظهر هنا",17,Color.WHITE,true);root.addView(title);
        space(root,15);
        root.addView(text("اختار الجودة",15,0xffc6d3e6,false));
        quality=new Spinner(this);quality.setBackground(bg(0xffe9eef6,12)); root.addView(quality,new LinearLayout.LayoutParams(-1,dp(52)));
        options.add("أولًا: اعرض الجودات");updateSpinner();
        space(root,18);
        download=button("تنزيل الفيديو MP4");download.setEnabled(false);root.addView(download,new LinearLayout.LayoutParams(-1,dp(54)));
        space(root,20);
        bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);root.addView(bar,new LinearLayout.LayoutParams(-1,dp(10)));
        space(root,12);
        status=text("جاري تهيئة أداة التحميل...",14,0xffb8c4d7,false);root.addView(status);
        space(root,25);
        root.addView(text("الملفات بتتحفظ في Downloads / YaYaTube. الجودات الأعلى ممكن تحتاج دمج الصوت مع الفيديو.",13,0xff8194af,false));
        inspect.setOnClickListener(v->inspectVideo());download.setOnClickListener(v->downloadVideo());
        if (Intent.ACTION_SEND.equals(getIntent().getAction())) {String shared=getIntent().getStringExtra(Intent.EXTRA_TEXT);if(shared!=null)urlInput.setText(shared);}
        pool.execute(()->{try{YoutubeDL.getInstance().init(getApplicationContext());ready=true;message("جاهز. الصق الرابط واضغط عرض الجودات.");}catch(Exception e){message("تعذر تهيئة التحميل: "+e.getMessage());}});
    }
    private void inspectVideo(){
        if(!ready||busy)return;
        String url=urlInput.getText().toString().trim();
        if(!valid(url)){message("اكتب رابط http أو https صحيح");return;}
        busy=true;inspect.setEnabled(false);download.setEnabled(false);message("بفحص الفيديو والجودات...");title.setText("جاري قراءة البيانات...");
        pool.execute(()->{try{
            YoutubeDLRequest req=new YoutubeDLRequest(url);req.addOption("--no-playlist");req.addOption("--list-formats");
            YoutubeDLResponse result=YoutubeDL.getInstance().execute(req);
            String output=result.getOut();
            // yt-dlp lists video-only and combined formats; select distinct available heights.
            TreeSet<Integer> heights=new TreeSet<>(Collections.reverseOrder());
            Matcher m=Pattern.compile("(?<!\\d)(144|240|360|480|720|1080|1440|2160|4320)p?(?!\\d)").matcher(output);
            while(m.find()) heights.add(Integer.parseInt(m.group(1)));
            // Fallback: other resolutions in the format table (e.g. 854x480).
            Matcher dimensions=Pattern.compile("(?:\\d{2,5})x(\\d{3,4})(?!\\d)").matcher(output);
            while(dimensions.find()){int h=Integer.parseInt(dimensions.group(1));if(h>=144&&h<=4320)heights.add(h);}
            ArrayList<String> found=new ArrayList<>();for(int h:heights)found.add(h+"p");
            if(found.isEmpty())found.add("أفضل جودة متاحة");
            videoUrl=url;
            runOnUiThread(()->{options.clear();options.addAll(found);updateSpinner();title.setText("تم اكتشاف "+found.size()+" اختيارات جودة");download.setEnabled(true);message("اختار الجودة واضغط تنزيل. الجودات حسب المصدر.");});
        }catch(Exception ex){message("تعذر قراءة الجودات: "+ex.getMessage());runOnUiThread(()->title.setText("حاول برابط آخر"));}
        finally{busy=false;runOnUiThread(()->inspect.setEnabled(true));}});
    }
    private void downloadVideo(){
        if(!ready||busy||videoUrl.isEmpty())return;
        String selected=(String)quality.getSelectedItem();if(selected==null)return;
        String url=videoUrl;busy=true;inspect.setEnabled(false);download.setEnabled(false);bar.setProgress(0);message("بدأ التحميل...");
        pool.execute(()->{File tempDir=new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"work_"+System.currentTimeMillis());tempDir.mkdirs();
            try{
                YoutubeDLRequest req=new YoutubeDLRequest(url);
                req.addOption("--no-playlist");req.addOption("--restrict-filenames");
                req.addOption("-o",new File(tempDir,"%(title).120s.%(ext)s").getAbsolutePath());
                if(selected.matches("\\d+p")){
                    String h=selected.substring(0,selected.length()-1);
                    req.addOption("-f","best[height<="+h+"]/best");
                }else req.addOption("-f","best");
                YoutubeDL.getInstance().execute(req,(progress,eta)->runOnUiThread(()->{bar.setProgress(Math.max(0,Math.min(100,Math.round(progress))));status.setText("جاري التحميل: "+Math.round(progress)+"% • باقي حوالي "+eta+" ثانية");}));
                File[] files=tempDir.listFiles();int count=0;
                if(files!=null)for(File f:files){if(!f.isFile()||f.getName().endsWith(".part")||f.getName().endsWith(".ytdl"))continue;copyToDownloads(f);count++;}
                if(count==0)throw new IOException("التحميل انتهى بدون ملف صالح");
                runOnUiThread(()->bar.setProgress(100));message("تم الحفظ في Downloads / YaYaTube ✅");
            }catch(Exception ex){message("مشكلة في التحميل: "+ex.getMessage());}
            finally{deleteTree(tempDir);busy=false;runOnUiThread(()->{inspect.setEnabled(true);download.setEnabled(true);});}
        });
    }
    private void copyToDownloads(File file)throws IOException{
        if(Build.VERSION.SDK_INT>=29){
            ContentValues values=new ContentValues();values.put(MediaStore.Downloads.DISPLAY_NAME,file.getName());
            values.put(MediaStore.Downloads.MIME_TYPE,file.getName().endsWith(".webm")?"video/webm":"video/mp4");
            values.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/JawTube");
            values.put(MediaStore.Downloads.IS_PENDING,1);
            ContentResolver resolver=getContentResolver();Uri uri=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);if(uri==null)throw new IOException("لا يمكن إنشاء ملف التنزيل");
            boolean done=false;try(InputStream in=new FileInputStream(file);OutputStream out=resolver.openOutputStream(uri)){if(out==null)throw new IOException("لا يمكن فتح الملف");byte[] buf=new byte[65536];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);done=true;}
            finally{if(done){ContentValues finish=new ContentValues();finish.put(MediaStore.Downloads.IS_PENDING,0);resolver.update(uri,finish,null,null);}else resolver.delete(uri,null,null);}
        }else{
            File dir=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"JawTube");if(!dir.exists()&&!dir.mkdirs())throw new IOException("تعذر إنشاء مجلد التنزيل");
            try(InputStream in=new FileInputStream(file);OutputStream out=new FileOutputStream(new File(dir,file.getName()))){byte[] buf=new byte[65536];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}
        }
    }
    private boolean valid(String url){try{Uri u=Uri.parse(url);return ("https".equals(u.getScheme())||"http".equals(u.getScheme()))&&u.getHost()!=null;}catch(Exception ex){return false;}}
    private void message(String msg){runOnUiThread(()->status.setText(msg));}
    private void updateSpinner(){ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,options);quality.setAdapter(adapter);}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);t.setGravity(Gravity.RIGHT);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(navy);b.setTextSize(16);b.setAllCaps(false);b.setBackground(bg(cyan,14));return b;}
    private GradientDrawable bg(int color,int r){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(r));return d;}
    private int dp(int d){return (int)(d*getResources().getDisplayMetrics().density);}
    private void space(LinearLayout l,int px){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(px)));}
    private void deleteTree(File dir){File[] files=dir.listFiles();if(files!=null)for(File f:files){if(f.isDirectory())deleteTree(f);else f.delete();}dir.delete();}
    @Override protected void onDestroy(){super.onDestroy();pool.shutdown();}
}
