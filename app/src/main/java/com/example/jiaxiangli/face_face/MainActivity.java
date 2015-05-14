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
import android.util.Log;
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

                //System.out.println("the uri is"+uri);

                Cursor cursor=getContentResolver().query(uri, null, null, null, null);
                //move the cursor to first row, and return false if the cursor is empty
                cursor.moveToFirst();

                //MediaStore.Images.ImageColumns.DATA is a data stream,and this data stream
                //is column name
                //Returns the zero-based index for the given column name,
                //or -1 if the column doesn't exist.
                int idx=cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);

                //Returns the value of the requested column as a String
                //mCurrentPhotoStr get the directory of the selected picture
                mCurrentPhotoStr=cursor.getString(idx);

                //String info=(String)mCurrentPhotoStr;
                //Log.e("TAG",mCurrentPhotoStr);

                //Closes the Cursor, releasing all of its resources and
                //making it completely invalid
                cursor.close();


                resizePhoto();

                //set a new image in ImageView
                mPhoto.setImageBitmap(mPhotoImg);
                mTip.setText("Click Detect ==>");
            }
        }

        super.onActivityResult(requestCode,resultCode,intent);
    }

    private void resizePhoto() {

        //get Options object,since bitmap has private constructor
        //so we cannot get bitmap directly
        BitmapFactory.Options options=new BitmapFactory.Options();

        //the bitmap will not completely decode by BitmapFactory.decodeFile later,
        //if inJustDecodeBounds set as true
        options.inJustDecodeBounds=true;

        //get bitmap by the directory which store in mCurrentPhotoStr
        BitmapFactory.decodeFile(mCurrentPhotoStr,options);

        //get the ratio
        double ratio=Math.max(options.outWidth*1.0d/1024f,options.outHeight*1.0d/1024f);

        options.inSampleSize=(int) Math.ceil(ratio);
        options.inJustDecodeBounds=false;

        //initialize mPhotoImage which is a Bitmap object and assign complete data
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
                //if id_getImage is clicked
                case R.id.id_getImage:
                    //create a intent and Pick an item from the data, returning what was selected
                    Intent intent=new Intent(Intent.ACTION_PICK);
                    //just select any data is image
                    intent.setType("image/*");
                    //start an activity and when it ends, it return selected data by intent
                    //PICK_CODE is any int number
                    startActivityForResult(intent,PICK_CODE);
                    break;

                case R.id.id_detect:

                    //set frame layout as visible
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
