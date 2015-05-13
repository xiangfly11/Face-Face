package com.example.jiaxiangli.face_face;

import android.graphics.Bitmap;
import android.util.Log;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * Created by Jiaxiang Li on 5/13/2015.
 */
public class FaceDetect {

    public interface Callback{
        void success(JSONObject result);

        void error(FaceppParseException exeption);
    }


    public static void detect(final Bitmap bm, final Callback callBack){
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    HttpRequests requests=new HttpRequests(Constant.key,Constant.secertKey,true,true);
                    Bitmap bmSmall=Bitmap.createBitmap(bm,0,0,bm.getWidth(),bm.getHeight());

                    ByteArrayOutputStream stream=new ByteArrayOutputStream();

                    bmSmall.compress(Bitmap.CompressFormat.JPEG,100,stream);

                    byte[] array=stream.toByteArray();

                    PostParameters params=new PostParameters();
                    params.setImg(array);
                    JSONObject jsonObject=requests.detectionDetect(params);


                    Log.e("TAG",jsonObject.toString());
                    if(callBack!=null){
                        callBack.success(jsonObject);
                    }
                } catch (FaceppParseException e) {
                    e.printStackTrace();

                    if(callBack!=null){
                        callBack.error(e);
                    }
                }

            }
        }).start();
    }
}
