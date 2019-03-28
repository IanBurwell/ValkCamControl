package com.valkcam.ian.valkcam;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

public class StaticView extends View {
    Paint p = new Paint();

    public StaticView(Context context) {
        super(context);
    }

    public StaticView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public StaticView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }


    final int size = 10;
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for(int x = 0; x < (canvas.getWidth()+1)/size; x++){
            for(int y = 0; y < (canvas.getHeight()+1)/size; y++){
                int col = (int)(Math.round(Math.random()*100)%50);
                if(Math.random() > 0.5){
                    col = 255 - col;
                }
                p.setColor(Color.rgb(col,col,col));
                canvas.drawRect(x*size,y*size,(x+1)*size,(y+1)*size,p);
            }
        }
        invalidate();
    }
}
