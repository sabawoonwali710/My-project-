package com.sabawoon.riyadalisan;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

public class ZoomImageView extends ImageView {
  private final Matrix matrix=new Matrix(), saved=new Matrix();
  private final PointF start=new PointF();
  private final ScaleGestureDetector detector;
  private float scale=1f;
  public ZoomImageView(Context c){this(c,null);}
  public ZoomImageView(Context c, AttributeSet a){
    super(c,a); setScaleType(ScaleType.MATRIX);
    detector=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
      public boolean onScale(ScaleGestureDetector d){
        float old=scale; scale=Math.max(1f,Math.min(5f,scale*d.getScaleFactor()));
        matrix.postScale(scale/old,scale/old,d.getFocusX(),d.getFocusY()); setImageMatrix(matrix); return true;
      }
      public void onScaleEnd(ScaleGestureDetector d){ if(scale<=1.01f) resetZoom(); }
    });
  }
  public void resetZoom(){scale=1f;fit();}
  private void fit(){
    Drawable d=getDrawable(); if(d==null||getWidth()==0||getHeight()==0)return;
    float dw=d.getIntrinsicWidth(),dh=d.getIntrinsicHeight(); if(dw<=0||dh<=0)return;
    float s=Math.min((float)getWidth()/dw,(float)getHeight()/dh);
    matrix.reset(); matrix.postScale(s,s);
    matrix.postTranslate((getWidth()-dw*s)/2f,(getHeight()-dh*s)/2f); setImageMatrix(matrix);
  }
  protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);post(this::fit);}
  public void setImageDrawable(Drawable d){super.setImageDrawable(d);post(this::fit);}
  public boolean onTouchEvent(MotionEvent e){
    detector.onTouchEvent(e);
    if(e.getActionMasked()==MotionEvent.ACTION_DOWN){saved.set(matrix);start.set(e.getX(),e.getY());}
    else if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&!detector.isInProgress()&&scale>1f){
      matrix.set(saved);matrix.postTranslate(e.getX()-start.x,e.getY()-start.y);setImageMatrix(matrix);
    }
    return true;
  }
}
