package com.mateusz.niwa.pathcollector;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.View;

public class MainActivity extends Activity {
    private ITraceRepository mRepository;
    private TraceCollector mView;
    private RelativeLayout mLayout;

    private static final int REQUEST_WRITE_STORAGE = 112;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRepository = new FileTraceRepository();
        mView = new TraceCollector(this, mRepository);
        mLayout = (RelativeLayout) findViewById(R.id.Rel);
        mLayout.addView(mView);

        boolean hasPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);

        if (!hasPermission)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) { }

    @Override
    protected void onDestroy() {
        mRepository.push();
        super.onDestroy();
    }

    public class TraceCollector extends View {
        private static final float TOUCH_TOLERANCE = 4;

        private ITraceRepository mRepository;

        private float mX, mY;
        private Bitmap mBitmap;
        private Canvas mCanvas;
        private Path mPath;
        private Paint mBitmapPaint;
        private Paint mPaint;
        private int mNumber;
        private int mBitmapWidth;
        private int mBitmapHeight;
        private TracingSession mSession;

        public TraceCollector(Context c, ITraceRepository repository) {
            super(c);

            mRepository = repository;
            mNumber = 0;
            DisplayMetrics display = this.getResources().getDisplayMetrics();
            mBitmapWidth = display.widthPixels;
            mBitmapHeight =  display.heightPixels;
            mBitmap = Bitmap.createBitmap(mBitmapWidth, mBitmapHeight, Bitmap.Config.ARGB_4444);
            mCanvas = new Canvas(mBitmap);
            mPath = new Path();
            mBitmapPaint = new Paint(Paint.DITHER_FLAG);
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setDither(true);
            mPaint.setColor(ContextCompat.getColor(c, android.R.color.holo_green_dark));
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeJoin(Paint.Join.ROUND);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mPaint.setStrokeWidth(20);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            setDrawingCacheEnabled(true);
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            canvas.drawPath(mPath, mPaint);
            canvas.drawRect(mY, 0, mY, 0, mBitmapPaint);
        }

        private void reset() {
            mBitmap = Bitmap.createBitmap(mBitmapWidth, mBitmapHeight, Bitmap.Config.ARGB_4444);
            mPath.reset();
            mSession = new TracingSession();
        }

        private void touchStart(float x, float y, long t) {
            reset();
            mSession.begin();
            mPath.moveTo(x, y);
            mSession.moveTo(x, y, t);
            mX = x;
            mY = y;
        }

        private void touchMove(float x, float y, long t) {
            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);

            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                mSession.moveTo(x, y, t);
                mX = x;
                mY = y;
            }
        }

        private void touchUp(long t) {
            mPath.lineTo(mX, mY);
            mSession.moveTo(mX, mY, t);
            mCanvas.drawPath(mPath, mPaint);
            mSession.end();
            mRepository.addTrace(mSession.getTrace(), Integer.toString(mNumber));
            mNumber = (mNumber + 1) % 10;
            //TextView textbox = (TextView) findViewById(R.id.textview);
            //textbox.setText(Integer.toString(mNumber));
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            long t = System.currentTimeMillis();
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStart(x, y, t);
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    touchMove(x, y, t);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    touchUp(t);
                    invalidate();
                    break;
            }

            return true;
        }

    }
}
