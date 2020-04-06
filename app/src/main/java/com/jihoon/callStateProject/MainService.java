package com.jihoon.callStateProject;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

public class MainService extends Service
{
    WindowManager wm;
    View mView;
    private float mTouchX, mTouchY;
    private int mViewX, mViewY;
    WindowManager.LayoutParams params;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    public int onStartCommand(Intent intent, int flags, int startId) {

        LayoutInflater inflate = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        params = new WindowManager.LayoutParams(
                /*ViewGroup.LayoutParams.MATCH_PARENT*/300,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        |WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED//이거 잠금화면일때 띄워주는거 //개편이 필요허다!!
                        |WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        |WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.CENTER | Gravity.CENTER;

        mView = inflate.inflate(R.layout.test, null);
        mView.setOnTouchListener(mViewTouchListener);

        final Button bt = (Button)mView.findViewById(R.id.buttonExitJ);
        final TextView textView = (TextView) mView.findViewById(R.id.textViewShow);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDestroy();
            }
        });
        final Button bt1 = (Button)mView.findViewById(R.id.buttonHide);
        bt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(textView.getVisibility() == View.VISIBLE)
                    textView.setVisibility(View.INVISIBLE);
                else textView.setVisibility(View.VISIBLE);
            }
        });

        textView.setBackgroundColor(Color.argb(200,255,255,255));
        textView.setTextSize(15f);
        //                //name TEXT, phonenum TEXT, email TEXT, memo TEXT)
        String n = intent.getStringExtra("name");
        String ph = intent.getStringExtra("phonenum");
        String em = intent.getStringExtra("email");
        String me = intent.getStringExtra("memo");

        String [] arr = {n,ph,em,me};
        String [] show = {" 이름", " 번호", " 이메일", " 메모"};
        int len = -1;
        for(int i=0;i<4;i++)
        {
            if(arr[i] != null)
            {
                if(len < arr[i].length() + 5) len = arr[i].length() + 5;
                if (textView.getText().toString().equals(""))
                    textView.setText(show[i] + " : " + arr[i]);
                else
                    textView.setText(textView.getText().toString() + "\n" + show[i] + " : " + arr[i]);
            }
        }

        DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
        int width = (int) (dm.widthPixels * 0.6); // Display 사이즈의 90%
        params.width = width;
        textView.setBackground(ContextCompat.getDrawable(this, R.drawable.edge));
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        wm.addView(mView, params);

        return startId;
    }

    @Override
    public void onCreate() { super.onCreate();}

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if(wm != null) {
                if(mView != null) {
                    wm.removeView(mView);
                    mView = null;
                }
                wm = null;
            }
        }
        catch (Exception e)
        {
            Log.d("jihoonDebugging", e.getMessage());
        }
    }

    private View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mTouchX = event.getRawX();
                    mTouchY = event.getRawY();
                    mViewX = params.x;
                    mViewY = params.y;
                    break;
                case MotionEvent.ACTION_UP:
                    break;
                case MotionEvent.ACTION_MOVE:
                    int x = (int) (event.getRawX() - mTouchX);
                    int y = (int) (event.getRawY() - mTouchY);
                    params.x = mViewX + x;
                    params.y = mViewY + y;
                    wm.updateViewLayout(mView, params);
                    break;
            }
            return true;
        }
    };
}
