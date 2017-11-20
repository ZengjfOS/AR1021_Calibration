/*****************************************************************************
* CODE OWNERSHIP AND DISCLAIMER OF LIABILITY
* 
* Microchip Technology Incorporated ("Microchip") retains all ownership and 
* intellectual property rights in the code accompanying this message and in 
* all derivatives hereto.  You may use this code, and any derivatives created 
* by any person or entity by or on your behalf, exclusively with Microchip's 
* proprietary products.  Your acceptance and/or use of this code constitutes 
* agreement to the terms and conditions of this notice.
* 
* CODE ACCOMPANYING THIS MESSAGE IS SUPPLIED BY MICROCHIP "AS IS".  NO 
* WARRANTIES, WHETHER EXPRESS, IMPLIED OR STATUTORY, INCLUDING, BUT NOT 
* LIMITED TO, IMPLIED WARRANTIES OF NON-INFRINGEMENT, MERCHANTABILITY AND 
* FITNESS FOR A PARTICULAR PURPOSE APPLY TO THIS CODE, ITS INTERACTION WITH 
* MICROCHIP'S PRODUCTS, COMBINATION WITH ANY OTHER PRODUCTS, OR USE IN ANY 
* APPLICATION. 
* 
* YOU ACKNOWLEDGE AND AGREE THAT, IN NO EVENT, SHALL MICROCHIP BE LIABLE, 
* WHETHER IN CONTRACT, WARRANTY, TORT (INCLUDING NEGLIGENCE OR BREACH OF 
* STATUTORY DUTY), STRICT LIABILITY, INDEMNITY, CONTRIBUTION, OR OTHERWISE, 
* FOR ANY INDIRECT, SPECIAL, PUNITIVE, EXEMPLARY, INCIDENTAL OR CONSEQUENTIAL 
* LOSS, DAMAGE, FOR COST OR EXPENSE OF ANY KIND WHATSOEVER RELATED TO THE 
* CODE, HOWSOEVER CAUSED, EVEN IF MICROCHIP HAS BEEN ADVISED OF THE 
* POSSIBILITY OR THE DAMAGES ARE FORESEEABLE.  TO THE FULLEST EXTENT ALLOWABLE 
* BY LAW, MICROCHIP'S TOTAL LIABILITY ON ALL CLAIMS IN ANY WAY RELATED TO THIS 
* CODE, SHALL NOT EXCEED THE PRICE YOU PAID DIRECTLY TO MICROCHIP SPECIFICALLY 
* TO HAVE THIS CODE DEVELOPED.
* 
* You agree that you are solely responsible for testing the code and 
* determining its suitability.  Microchip has no obligation to modify, test, 
* certify, or support the code.
*
* Author                Date        Comment
*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
* Steve Grahovac        05/27/11    Initial Microchip example Android touch 
*                                     calibration code.
*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
* 
* Description:
*    This source file displays three calibration points followed by saving
*    and applying a calibration after these points have been touched.
* 
******************************************************************************/

package com.microchip.calibration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;


/******************************************************************************
Class:
    calData()

Description:
    Utility class for processing and calculating calibration data from raw 
    touch points
******************************************************************************/
class calData {
    private int minX;
    private int minY;
    private int maxX;
    private int maxY;
    private int invertX;
    private int invertY;
    private int swapAxes;

    private int[] rawXCoords=new int[3];
    private int[] rawYCoords=new int[3];
    private int maxControllerValue=4095;

    /**************************************************************************
    Function:
        getMinX()

    Description:
        Return the smallest possible controller X coordinate value over the 
        left edge of the display.        
    **************************************************************************/
    int getMinX() {
        return minX;        
    }

    /**************************************************************************
    Function:
        getMinY()

    Description:
        Return the smallest possible controller Y coordinate value over the top
         edge of the display.        
    **************************************************************************/
    int getMinY() {
        return minY;        
    }

    /**************************************************************************
    Function:
        getMaxX()

    Description:
        Return the largest possible controller X coordinate value over the 
        right edge of the display.        
    **************************************************************************/
    int getMaxX() {
        return maxX;        
    }

    /**************************************************************************
    Function:
        getMaxY()

    Description:
        Return the largest possible controller Y coordinate value over the 
        bottom edge of the display.        
    **************************************************************************/
    int getMaxY() {
        return maxY;        
    }
    
    /**************************************************************************
    Function:
        getInvertX()

    Description:
        Returns whether or not the raw X coordinate values needed to be 
        recalculated at the opposite side of the screen.    
    **************************************************************************/
    int getInvertX() {
        return invertX;        
    }

    /**************************************************************************
    Function:
        getInvertY()

    Description:
        Returns whether or not the raw Y coordinate values needed to be 
        recalculated at the opposite side of the screen.    
    **************************************************************************/
    int getInvertY() {
        return invertY;        
    }

    /**************************************************************************
    Function:
        getSwapAxes()

    Description:
        Returns whether or not the raw X and Y coordinate values are swapped 
        such that when touching along the X axis of the screen, the Y axis 
        value is affected and vice versa.  
    **************************************************************************/
    int getSwapAxes() {
        return swapAxes;        
    }
    
    /**************************************************************************
    Function:
        InsetScaleFloatValue()

    Description:
        Calculate new X or Y value based on the center.
    **************************************************************************/
    float InsetScaleFloatValue(float value, float center, int inset, int inout)    {
        float returnValue=0;

        // We are scaling from the center of the screen, so to make it 100 
        // instead of 50 percent of range.
        // To accomplish this, we need to double inset.
        inset*=2.0;

        if (0==inout)
        {
            returnValue=((value-center)*(1f/(1f-inset/100.0f)))+center;
        }
        else
        {
            returnValue=((value-center)*(1f-inset/100.0f))+center;
        }

        return returnValue;
    }    

    /**************************************************************************
    Function:
        setCalPoint()

    Description:
        Store the current raw touch point for future processing.
    **************************************************************************/
    void setCalPoint(int x, int y, int calPointNum, int inset) {
        rawXCoords[calPointNum]=x;
        rawYCoords[calPointNum]=y;    
    }
    
    /**************************************************************************
    Function:
        calculateData()

    Description:
        Calculate calibration data based on raw touch points.
    **************************************************************************/
    void calculateData(int inset) {
        int i;
        int temp;
        int xcenter;
        int ycenter;
        
        // The X delta is the distance between the upper-left and upper-right
        // raw calibration coordinates.
        int deltaX=Math.abs(rawXCoords[1]-rawXCoords[0]);
        // The Y delta is the distance between the upper-right and lower-right
        // raw calibration coordinates.
        int deltaY=Math.abs(rawYCoords[1]-rawYCoords[0]);

        
        // In a non-swapped scenario, the X delta will be be greater for the 
        // first two points since it will appear like a horizontal line.
        // In a swapped scenario, the Y delta will be be greater for the 
        // first two points since it will appear like a vertical line.        
        if (deltaX < deltaY) {
            swapAxes=1;

            // if axes are swapped, unswap them
            for (i=0;i<3;i++)
            {
                temp=rawXCoords[i];
                rawXCoords[i]=rawYCoords[i];
                rawYCoords[i]=temp;    
            }
        }
        else {
            swapAxes=0;
        }

        // We need the controller coordinate origin so we may outset raw 
        // calibration points to the edge of the screen.
        xcenter=(rawXCoords[1]+rawXCoords[0])/2;
        ycenter=(rawYCoords[1]+rawYCoords[2])/2;
        
        // scale our raw coordinates out to the edges of the screen
        for (i=0;i<3;i++) {
            rawXCoords[i]=(int)InsetScaleFloatValue(rawXCoords[i], (float)xcenter, inset, 0);
            rawYCoords[i]=(int)InsetScaleFloatValue(rawYCoords[i], (float)ycenter, inset, 0);
        }
        
        // Correct inverted calibration points in X or Y if necessary. 
        
        if (rawXCoords[1] < rawXCoords[0]) {    
            invertX=1;
                
            // correct x-axis inversion
            for (i=0;i<3;i++) {
                rawXCoords[i]=maxControllerValue-rawXCoords[i];
            }                    
        }
        else {
            invertX=0;            
        }
        
        if (rawYCoords[1] > rawYCoords[2]) {
            invertY=1;            
        
            // correct y-axis inversion
            for (i=0;i<3;i++) {
                rawYCoords[i]=maxControllerValue-rawYCoords[i];    
            }                    
        }
        else {
            invertY=0;            
        }
            
        // set calibration values        
        
        minX=rawXCoords[0];
        
        // Choose lesser of URx/LRx to better ensure the edge of 
        // the display is touchable.
        if (rawXCoords[1] < rawXCoords[2]) {
            maxX=rawXCoords[1];
        }
        else {
            maxX=rawXCoords[2];
        }
        
        // Choose the larger of ULy/URy to better ensure the edge of 
        // the display is touchable.
        if (rawYCoords[0] > rawYCoords[1]) {
            minY=rawYCoords[0];
        }
        else {
            minY=rawYCoords[1];
        }

        maxY=rawYCoords[2];
        
        
    }
};

/******************************************************************************
Class:
    Calibration()

Description:
    Handles and initiates events within the applications Activity.
******************************************************************************/
public class Calibration extends Activity {

    // we save a reference to the current activity so we may exit after calibration
    static Activity thisCalibrationActivity;
    static calData calibrationData=new calData();
    static String sysfsPath=new String();

    /**************************************************************************
    Function:
        readKernelValue()

    Description:
        Read a kernel value from the sysfs tree.
    **************************************************************************/    
    public static int readKernelValue(String valueToRead){
        int returnValue=0;
        try {
            BufferedReader in = new BufferedReader(new FileReader(sysfsPath+"/"+valueToRead));
            String str;
            str=in.readLine();
            if (null!=str){
                returnValue=Integer.parseInt(str);                
            }
            else {
                Log.v("MCHP","Unable to read "+valueToRead);
            }
            
            in.close();
        } catch (IOException e) {
            Log.v("MCHP","Exception writing kernel data.");
            e.printStackTrace();
            Log.v("MCHP", e.getMessage());  
        }        

        return returnValue;
    }
    
    /**************************************************************************
    Function:
        writeKernelValue()

    Description:
        Read a kernel value to the sysfs tree.
    **************************************************************************/    
    public static void writeKernelValue(String valueToWrite, String value){
        try
        {
            File root = new File("", sysfsPath);            
            if (!root.exists()) {
                root.mkdirs();
            }
            
            File gpxfile = new File(root, valueToWrite);
            FileWriter writer = new FileWriter(gpxfile);
            
            writer.append(value);
            writer.flush();
            writer.close();          
        }
        catch(IOException e) {
            Log.v("MCHP","Exception reading kernel data.");
             e.printStackTrace();
             Log.v("MCHP", e.getMessage());  
        }
        
    }

    /**************************************************************************
    Function:
        setRawMode()

    Description:
        Put kernel into a raw mode so we may get valid calibration values.
    **************************************************************************/    
    public void setRawMode() {
        writeKernelValue("minX",Integer.toString(0));        
        writeKernelValue("minY",Integer.toString(0));    
        writeKernelValue("maxX",Integer.toString(4095));    
        writeKernelValue("maxY",Integer.toString(4095));    
        writeKernelValue("swapAxes",Integer.toString(0));    
        writeKernelValue("invertX",Integer.toString(0));    
        writeKernelValue("invertY",Integer.toString(0));    
    }
    
    /**************************************************************************
    Function:
        onCreate()

    Description:
        This is the first function that is called when the application is
        launched directly.
    **************************************************************************/        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Bundle bundle = this.getIntent().getExtras();
        String param1="";
        
        sysfsPath="/sys/kernel/ar1020";        
        File file =new File(sysfsPath);      
        // if path is not exist that may use ar1021 chip
        if (!file .exists()  && !file .isDirectory())        
        {         
            sysfsPath="/sys/kernel/ar1021";        
        } 
        
        if (null != bundle)
        {
            param1 = bundle.getString("param1");
            if (param1 == null)
            	param1 = "";

            Log.v("MCHP","param1 set to: "+param1);
        }

        super.onCreate(savedInstanceState);
        DisplayMetrics metrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        thisCalibrationActivity=this;

        setRawMode();
        if (0!=param1.compareTo("silent")) {
            setContentView(new CalibrationView(this,metrics));
        }
        else{   
            // Either the previous saved calibration values or the
            // default values are being applied.  To change the default
            // calibration values, change the second parameter of the
            // getInt() function below.
            int temp;
            SharedPreferences preferences = getPreferences(MODE_PRIVATE);
            temp = preferences.getInt("minX", 0);
            writeKernelValue("minX",Integer.toString(temp));
            Log.v("MCHP","minX: "+Integer.toString(temp));
            
            temp = preferences.getInt("minY", 0);
            writeKernelValue("minY",Integer.toString(temp));
            Log.v("MCHP","minY: "+Integer.toString(temp));
            
            temp = preferences.getInt("maxX", 4095);
            writeKernelValue("maxX",Integer.toString(temp));
            Log.v("MCHP","maxX: "+Integer.toString(temp));
            
            temp = preferences.getInt("maxY", 4095);
            writeKernelValue("maxY",Integer.toString(temp));
            Log.v("MCHP","maxY: "+Integer.toString(temp));
            
            temp = preferences.getInt("swapAxes", 0);
            writeKernelValue("swapAxes",Integer.toString(temp));
            Log.v("MCHP","swapAxes: "+Integer.toString(temp));
            
            temp = preferences.getInt("invertX", 0);
            writeKernelValue("invertX",Integer.toString(temp));
            Log.v("MCHP","invertX: "+Integer.toString(temp));
            
            temp = preferences.getInt("invertY", 0);
            writeKernelValue("invertY",Integer.toString(temp));
            Log.v("MCHP","invertY: "+Integer.toString(temp));            
            
            Log.v("MCHP","Touchscreen calibration applied.");
            this.finish();            
        }
    }

    /**************************************************************************
    Function:
        onStop()

    Description:
        Before exiting calibration, this function is used to save and apply our 
        calibration values
    **************************************************************************/        
    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        
        // save calibration data so we may reuse it on boot
        editor.putInt("minX",  calibrationData.getMinX());
        editor.putInt("minY",  calibrationData.getMinY()); 
        editor.putInt("maxX",  calibrationData.getMaxX());         
        editor.putInt("maxY",  calibrationData.getMaxY());
        editor.putInt("swapAxes",  calibrationData.getSwapAxes()); 
        editor.putInt("invertX",  calibrationData.getInvertX());         
        editor.putInt("invertY",  calibrationData.getInvertY());         
        editor.commit();

        Log.v("MCHP","minX: "+Integer.toString(calibrationData.getMinX()));
        Log.v("MCHP","minY: "+Integer.toString(calibrationData.getMinY()));
        Log.v("MCHP","maxX: "+Integer.toString(calibrationData.getMaxX()));
        Log.v("MCHP","maxY: "+Integer.toString(calibrationData.getMaxY()));
        Log.v("MCHP","swapAxes: "+Integer.toString(calibrationData.getSwapAxes()));
        Log.v("MCHP","invertX: "+Integer.toString(calibrationData.getInvertX()));
        Log.v("MCHP","invertY: "+Integer.toString(calibrationData.getInvertY()));
        
        // apply calibration data
        writeKernelValue("minX",Integer.toString(calibrationData.getMinX()));        
        writeKernelValue("minY",Integer.toString(calibrationData.getMinY()));    
        writeKernelValue("maxX",Integer.toString(calibrationData.getMaxX()));    
        writeKernelValue("maxY",Integer.toString(calibrationData.getMaxY()));    
        writeKernelValue("swapAxes",Integer.toString(calibrationData.getSwapAxes()));    
        writeKernelValue("invertX",Integer.toString(calibrationData.getInvertX()));    
        writeKernelValue("invertY",Integer.toString(calibrationData.getInvertY()));

    }
    
    /**************************************************************************
    Class:
        CalibrationView()

    Description:
        Handles and initiates events within the applications View.
    **************************************************************************/
    private static class CalibrationView extends View {
        private Paint   mPaint = new Paint();
        private float[] mPts;
        private final float targetSize=50;
        private final int displayWidth;
        private final int displayHeight;
        private int targetNum=0;
        final int inset=10;
        
        private void buildTarget() {
            mPts = new float[8];

            mPts[0] = 0.5f*targetSize;
            mPts[1] = 0;
            mPts[2] = 0.5f*targetSize;
            mPts[3] = targetSize;        

            mPts[4] = 0;
            mPts[5] = 0.5f*targetSize;
            mPts[6] = targetSize; 
            mPts[7] = 0.5f*targetSize;        
        }

        /**********************************************************************
        Function:
            moveTarget()

        Description:
            Moves the calibration cross-hair to the specified origin.
        **********************************************************************/                 
        private void moveTarget(float X, float Y, Canvas canvas) {   
            Log.v("MCHP","Displaying target at " + X + ", " + Y);
            canvas.translate(X-targetSize/2, Y-targetSize/2);
        }
        
        /**********************************************************************
        Function:
            CalibrationView()

        Description:
            This class constructor sets up interval values such as display
            attributes and how the calibration cross-hair will appear. 
        **********************************************************************/                 
        public CalibrationView(Context context, DisplayMetrics metrics) {
            super(context);
            displayWidth=metrics.widthPixels;
            displayHeight=metrics.heightPixels;
            
            
            Log.v("MCHP","displayWidth: " + metrics.widthPixels);            
            Log.v("MCHP","displayHeight: " + metrics.heightPixels);
            
            buildTarget();
        }

        /**********************************************************************
        Function:
            onTouchEvent()

        Description:
            This function processes touch events.  In particular, we are 
            waiting for a touch up event (ACTION_UP) to determine when it time 
            to process and display the next calibration coordinate.
        **********************************************************************/                        
        @Override 
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    Log.v("MCHP","ACTION_UP");
                    int rawX=readKernelValue("lastPUCoordX"); 
                    int rawY=readKernelValue("lastPUCoordY");
                    calibrationData.setCalPoint(rawX, rawY, targetNum, inset);
                    targetNum++;
                    invalidate();                   
                    break;
            }
            return true;
        }
    
        
        /**********************************************************************
        Function:
            onDraw()

        Description:
            After every time the calibration view is invalidated or needs 
            redrawing, this function is called to draw the graphical elements 
            such as the text and calibration cross-hair.  
        **********************************************************************/                                
        @Override 
        protected void onDraw(Canvas canvas) {
            Paint paint = mPaint;
            final float X;
            final float Y;


            // need to save the matrix so we may properly move calibration target
            canvas.save();
            switch (targetNum){
                case 0:
                // upper-left
                X=calibrationData.InsetScaleFloatValue(0, displayWidth/2, inset, 1);
                Y=calibrationData.InsetScaleFloatValue(0, displayHeight/2, inset, 1);            
                moveTarget(X,Y,canvas);
                break;
                
                case 1:
                // upper-right
                X=calibrationData.InsetScaleFloatValue(displayWidth, displayWidth/2, inset, 1);
                Y=calibrationData.InsetScaleFloatValue(0, displayHeight/2, inset, 1);            
                moveTarget(X,Y,canvas);
                break;
                
                case 2:
                // lower-right
                X=calibrationData.InsetScaleFloatValue(displayWidth, displayWidth/2, inset, 1);
                Y=calibrationData.InsetScaleFloatValue(displayHeight, displayHeight/2, inset, 1);            
                moveTarget(X,Y,canvas);
                break;
                default:
                    calibrationData.calculateData(inset);              
                    Toast.makeText(thisCalibrationActivity, "Saved", Toast.LENGTH_SHORT).show();
                    thisCalibrationActivity.finish();
                break;
            }
                                          
            canvas.drawColor(Color.WHITE);

            paint.setColor(Color.RED);
            paint.setStrokeWidth(0);
            canvas.drawLines(mPts, paint);
            
            
            paint.setColor(Color.BLUE);
            paint.setStrokeWidth(3);
            canvas.drawPoints(mPts, paint);
            canvas.restore();
            
            paint.setColor(Color.BLACK); 
            paint.setAntiAlias(true);
            paint.setTextSize(20);
            paint.setTypeface(Typeface.SERIF);
            
            paint.setTextAlign(Paint.Align.CENTER);
            
            canvas.drawText("Please touch/release the calibration targets", canvas.getWidth()/2, canvas.getHeight()/2, paint);            
        }
    }
}

