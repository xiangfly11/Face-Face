package com.example.jiaxiangli.face_face;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.facepp.error.FaceppParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Handler;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private static final int PICK_CODE=0x110;
    private ImageView mPhoto;
    private Button mGetImage;
    private Button mDetect;
    private TextView mTip;
    private View mWaiting;

    private Bitmap mPhotoImg;

    private String mCurrentPhotoStr;

    private Paint mPaint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initEvents();
        mPaint=new Paint();

    }

    public void initViews(){
        mPhoto=(ImageView)findViewById(R.id.id_image);
        mGetImage=(Button)findViewById(R.id.id_getImage);
        mDetect=(Button)findViewById(R.id.id_detect);
        mTip=(TextView)findViewById(R.id.id_tip);
        mWaiting=findViewById(R.id.id_waiting);
    }

    protected void onActivityResult(int requestCode, int resultCode,Intent intent){
        if(requestCode==PICK_CODE){
            if(intent!=null){
                Uri uri=intent.getData();
                Cursor cursor=getContentResolver().query(uri, null, null, null, null);
                cursor.moveToFirst();

                int idx=cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                mCurrentPhotoStr=cursor.getString(idx);
                cursor.close();

                resizePhoto();

                mPhoto.setImageBitmap(mPhotoImg);
                mTip.setText("Click Detect ==>");
            }
        }

        super.onActivityResult(requestCode,resultCode,intent);
    }

    private void resizePhoto() {
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inJustDecodeBounds=true;

        BitmapFactory.decodeFile(mCurrentPhotoStr,options);

        double ratio=Math.max(options.outWidth*1.0d/1024f,options.outHeight*1.0d/1024f);

        options.inSampleSize=(int) Math.ceil(ratio);
        options.inJustDecodeBounds=false;
        mPhotoImg=BitmapFactory.decodeFile(mCurrentPhotoStr,options);
    }






    public void initEvents(){
        mGetImage.setOnClickListener(this);
        mDetect.setOnClickListener(this);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private static final int MSG_SUCCESS=0x111;
    private static final int MSG_ERROR=0x112;


    private android.os.Handler mHandler=new android.os.Handler(){
        public void handleMessage(Message msg){
            switch (msg.what){
                case MSG_SUCCESS:
                    mWaiting.setVisibility(View.GONE);
                    JSONObject rs=(JSONObject) msg.obj;

                    prePareRsBitmap(rs);

                    mPhoto.setImageBitmap(mPhotoImg);
                    break;
                case MSG_ERROR:
                    mWaiting.setVisibility(View.GONE);

                    String errorMsg=(String) msg.obj;
                    if(TextUtils.isEmpty(errorMsg)){
                        mTip.setText("Error.");
                    }else{
                        mTip.setText(errorMsg);
                    }
                    break;
            }

            super.handleMessage(msg);
        }
    };

    private void prePareRsBitmap(JSONObject rs) {
        Bitmap bitmap=Bitmap.createBitmap(mPhotoImg.getWidth(),mPhotoImg.getHeight(),mPhotoImg.getConfig());

        Canvas canvas=new Canvas(bitmap);

        canvas.drawBitmap(mPhotoImg,0,0,null);

        try {
            JSONArray faces=rs.getJSONArray("face");

            int faceCount=faces.length();

            mTip.setText("find"+faceCount);

            for(int i=0;i<faceCount;i++){
                JSONObject face= faces.getJSONObject(i);
                JSONObject posObj=face.getJSONObject("position");

                float x=(float )posObj.getJSONObject("center").getDouble("x");
                float  y=(float )posObj.getJSONObject("center").getDouble("y");

                float  w=(float )posObj.getDouble("width");
                float  h=(float )posObj.getDouble("height");

                x=x/100*bitmap.getWidth();
                y=y/100*bitmap.getHeight();

                w=w/100*bitmap.getWidth();
                h=h/100*bitmap.getHeight();

                mPaint.setColor(0xffffffff);
                mPaint.setStrokeWidth(3);

                canvas.drawLine(x-w/2,y-h/2,x-w/2,y+h/2,mPaint);
                canvas.drawLine(x-w/2,y-h/2,x+w/2,y-h/2,mPaint);
                canvas.drawLine(x+w/2,y-h/2,x+w/2,y+h/2,mPaint);
                canvas.drawLine(x-w/2,y+h/2,x+w/2,y+h/2,mPaint);


                //get age and gender

                int age=face.getJSONObject("attribute").getJSONObject("age").getInt("value");
                String gender=face.getJSONObject("attribute").getJSONObject("gender").getString("value");

                Bitmap ageBitmap=bulidAgeBitmap(age,"Male".equals(gender));

                int ageWidth=ageBitmap.getWidth();
                int ageHeight=ageBitmap.getHeight();

                if(bitmap.getWidth()<mPhoto.getWidth()&&bitmap.getHeight()<mPhoto.getHeight()){
                    float ratio=Math.max(bitmap.getWidth()*1.0f/mPhoto.getWidth(),bitmap.getHeight()*1.0f/mPhoto.getHeight());
                    ageBitmap=Bitmap.createScaledBitmap(ageBitmap,(int)(ageWidth*ratio),(int)(ageHeight*ratio),false);
                }

                canvas.drawBitmap(ageBitmap,x-ageBitmap.getWidth()/2,y-h/2-ageBitmap.getHeight(),null);

                mPhotoImg=bitmap;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Bitmap bulidAgeBitmap(int age, boolean isMale) {
        TextView tv=(TextView)mWaiting.findViewById( R.id.id_age_and_gender);
        tv.setText(age+"");
        if(isMale){
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.male),null,null,null);

        }else{
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.female),null,null,null);
        }


        tv.setDrawingCacheEnabled(true);
        Bitmap bitmap=Bitmap.createBitmap(tv.getDrawingCache());
        tv.destroyDrawingCache();

        return bitmap;
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
                case R.id.id_getImage:
                    Intent intent=new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    startActivityForResult(intent,PICK_CODE);
                    break;
                case R.id.id_detect:

                    mWaiting.setVisibility(View.VISIBLE);

                    if(mCurrentPhotoStr!=null&&!mCurrentPhotoStr.trim().equals("")){
                        resizePhoto();
                    }else{
                        mPhotoImg=BitmapFactory.decodeResource(getResources(),R.drawable.test);
                    }
                    FaceDetect.detect(mPhotoImg,new FaceDetect.Callback(){

                        @Override
                        public void success(JSONObject result) {
                            Message msg=Message.obtain();
                            msg.what=MSG_SUCCESS;
                            msg.obj=result;
                            mHandler.sendMessage(msg);
                        }

                        @Override
                        public void error(FaceppParseException exeption) {
                            Message msg=Message.obtain();
                            msg.what=MSG_ERROR;
                            msg.obj=exeption.getErrorMessage();
                            mHandler.sendMessage(msg);
                        }
                    });
                    break;
            }

    }
}
