package com.sabawoon.riyadalisan;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfRenderer;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
  private ZoomImageView pageView; private TextView pageLabel; private ProgressBar progress;
  private PdfRenderer renderer; private ParcelFileDescriptor descriptor; private Bitmap bitmap;
  private int page=0; private final ExecutorService worker=Executors.newSingleThreadExecutor();
  private SharedPreferences prefs;

  private final String[] toc={"پوښ","فهرست","سریزه","قاعدې د لیکلو سبب او اهداف","د قاعدې اهمیت",
    "حروف هجاء مفرده اهمیت","هغه ځایونه چې حروف ترې اداء کیږي","د حروف هجاء د عملي اداء تمرین",
    "تمرین په عملي کولو د بعضې حروفو په صفاتو","صفت همس","صفت رخوت","صفت بینیة",
    "تمرین په ډکو حروفو باندې","حروف مفخمه په منځ د حروف مرققه کې","پرله پسې ډک حروف",
    "حرف راء د تکریر تمرین","تجوید بېلابېلو احکامو عملي کول","اخفاء د اداء کولو تمرین",
    "تمرین په مقدار د مدونو کې","د تسهیل په اداء کولو تمرین","د امالې په اداء کولو تمرین",
    "اشمام او روم","د کلمې په اخر کې د همزې پر وقف کولو تمرین","په مشدد حرف د وقف تمرین"};
  private final int[] tocPages={0,1,2,3,4,5,6,8,24,25,26,27,28,29,31,34,35,37,39,43,44,45,46,47};

  public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(7,91,138));
    prefs=getSharedPreferences("reader",MODE_PRIVATE);buildUi();openBook();}
  private int dp(float v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
  private Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(14);b.setMinHeight(0);return b;}
  private TextView txt(String s,float z,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setGravity(Gravity.CENTER);return t;}
  private void buildUi(){
    getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(244,248,251));
    LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(4),dp(4),dp(4),dp(4));top.setBackgroundColor(Color.rgb(12,130,198));
    Button tocBtn=btn("فهرست"),markBtn=btn("نښه"),jumpBtn=btn("پاڼه");TextView title=txt("رياضة اللسان",18,Color.WHITE);
    top.addView(tocBtn,new LinearLayout.LayoutParams(0,dp(46),1));top.addView(markBtn,new LinearLayout.LayoutParams(0,dp(46),1));
    top.addView(jumpBtn,new LinearLayout.LayoutParams(0,dp(46),1));top.addView(title,new LinearLayout.LayoutParams(0,dp(46),2));
    FrameLayout viewer=new FrameLayout(this);pageView=new ZoomImageView(this);pageView.setBackgroundColor(Color.rgb(232,238,242));
    viewer.addView(pageView,new FrameLayout.LayoutParams(-1,-1));progress=new ProgressBar(this);viewer.addView(progress,new FrameLayout.LayoutParams(dp(52),dp(52),Gravity.CENTER));
    LinearLayout bottom=new LinearLayout(this);bottom.setGravity(Gravity.CENTER);bottom.setBackgroundColor(Color.WHITE);
    Button prev=btn("◀ مخکینی"),next=btn("راتلونکی ▶");pageLabel=txt("…",14,Color.rgb(21,35,43));
    bottom.addView(prev,new LinearLayout.LayoutParams(0,dp(48),1));bottom.addView(pageLabel,new LinearLayout.LayoutParams(dp(105),dp(48)));
    bottom.addView(next,new LinearLayout.LayoutParams(0,dp(48),1));root.addView(top,new LinearLayout.LayoutParams(-1,-2));
    root.addView(viewer,new LinearLayout.LayoutParams(-1,0,1));root.addView(bottom,new LinearLayout.LayoutParams(-1,-2));setContentView(root);
    prev.setOnClickListener(v->showPage(page-1));next.setOnClickListener(v->showPage(page+1));jumpBtn.setOnClickListener(v->jump());
    tocBtn.setOnClickListener(v->showToc());markBtn.setOnClickListener(v->bookmark());title.setOnClickListener(v->about());
  }
  private void openBook(){progress.setVisibility(View.VISIBLE);worker.execute(()->{try{
      File pdf=new File(getFilesDir(),"book.pdf");if(!pdf.exists()||pdf.length()==0)try(InputStream in=getAssets().open("book.pdf");FileOutputStream out=new FileOutputStream(pdf)){
        byte[] buf=new byte[32768];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}
      descriptor=ParcelFileDescriptor.open(pdf,ParcelFileDescriptor.MODE_READ_ONLY);renderer=new PdfRenderer(descriptor);
      int saved=Math.max(0,Math.min(renderer.getPageCount()-1,prefs.getInt("last",0)));runOnUiThread(()->showPage(saved));
    }catch(Exception e){runOnUiThread(()->new AlertDialog.Builder(this).setTitle("تېروتنه").setMessage("کتاب خلاص نه شو: "+e.getMessage()).setPositiveButton("سمه ده",null).show());}});}
  private void showPage(int wanted){if(renderer==null||wanted<0||wanted>=renderer.getPageCount())return;page=wanted;
    prefs.edit().putInt("last",page).apply();pageLabel.setText((page+1)+" / "+renderer.getPageCount());progress.setVisibility(View.VISIBLE);
    worker.execute(()->{Bitmap result=null;PdfRenderer.Page p=null;try{p=renderer.openPage(wanted);int vw=Math.max(pageView.getWidth(),dp(720));
      float s=(float)vw/p.getWidth();int h=Math.max(1,(int)(p.getHeight()*s));result=Bitmap.createBitmap(vw,h,Bitmap.Config.ARGB_8888);
      result.eraseColor(Color.WHITE);p.render(result,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);}catch(Exception ignored){}finally{if(p!=null)p.close();}
      Bitmap ready=result;runOnUiThread(()->{if(wanted!=page){if(ready!=null)ready.recycle();return;}if(ready!=null){if(bitmap!=null&&!bitmap.isRecycled())bitmap.recycle();
        bitmap=ready;pageView.setImageBitmap(ready);pageView.resetZoom();}progress.setVisibility(View.GONE);});});}
  private void jump(){if(renderer==null)return;EditText input=new EditText(this);input.setInputType(InputType.TYPE_CLASS_NUMBER);input.setGravity(Gravity.CENTER);input.setHint("1 - "+renderer.getPageCount());
    new AlertDialog.Builder(this).setTitle("پاڼې ته لاړ شه").setView(input).setPositiveButton("لاړ شه",(d,w)->{try{int p=Integer.parseInt(input.getText().toString().trim());
      if(p>=1&&p<=renderer.getPageCount())showPage(p-1);else Toast.makeText(this,"د پاڼې شمېره سمه نه ده",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"شمېره ولیکه",Toast.LENGTH_SHORT).show();}})
      .setNegativeButton("بند",null).show();}
  private void showToc(){new AlertDialog.Builder(this).setTitle("فهرست").setItems(toc,(d,i)->{if(i<tocPages.length)showPage(tocPages[i]);}).setNegativeButton("بند",null).show();}
  private void bookmark(){int saved=prefs.getInt("bookmark",-1);String[] items=saved>=0?new String[]{"دا پاڼه نښه کړه","نښه شوې پاڼې ته لاړ شه ("+(saved+1)+")","نښه پاکه کړه"}:new String[]{"دا پاڼه نښه کړه"};
    new AlertDialog.Builder(this).setTitle("د کتاب نښه").setItems(items,(d,i)->{if(i==0){prefs.edit().putInt("bookmark",page).apply();Toast.makeText(this,"پاڼه نښه شوه",Toast.LENGTH_SHORT).show();}
      else if(i==1&&saved>=0)showPage(saved);else if(i==2)prefs.edit().remove("bookmark").apply();}).setNegativeButton("بند",null).show();}
  private void about(){TextView t=txt("رياضة اللسان على النطق بأحرف القرآن\n\nحاشیه: أبو دجانه رومل البدر\nترتیب کوونکی: سباوون ولي\n\nOffline Android Book App",17,Color.rgb(21,35,43));
    t.setPadding(dp(22),dp(22),dp(22),dp(22));ScrollView s=new ScrollView(this);s.addView(t);new AlertDialog.Builder(this).setTitle("د اپ په اړه").setView(s).setPositiveButton("سمه ده",null).show();}
  protected void onDestroy(){super.onDestroy();worker.shutdownNow();try{if(renderer!=null)renderer.close();}catch(Exception ignored){}try{if(descriptor!=null)descriptor.close();}catch(IOException ignored){}
    if(bitmap!=null&&!bitmap.isRecycled())bitmap.recycle();}
}
