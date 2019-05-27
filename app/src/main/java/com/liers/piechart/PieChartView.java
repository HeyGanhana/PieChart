package com.liers.piechart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import dalvik.annotation.TestTarget;

/**
 * @author zhangdi
 * @date 19-5-22 上午9:19
 * @description
 */
public class PieChartView extends View{

    //PieChart defautl height and width
    private static int DEFAULT_WIDTH = 200;
    private static int DEFAULT_HEIGHT = 200;
    private static float DEFAULT_RADIUS = 80;

    private int containerWidth = DEFAULT_WIDTH;
    private int containerHeight = DEFAULT_HEIGHT;
    private float containerRadius = DEFAULT_RADIUS;
    //控制饼状图半径
    private float radiusScale = 0.6f;
    //每个pieModel 之间的角度间隔
    private float intervalDegree = 2;
    //pieModel focused  距离中心的距离
    private float centerOffset = 10;
    //显示详细信息
    private boolean showPieText = false;

    //container 圆心
    private float containerCenterX;
    private float containerCenterY;
    private Paint piePaint;
    private RectF containerRectF;
    private RectF focusRectF;
    //piemodel 数据
    private List<PieModel> mPieModelList = new ArrayList<>();

    private float currentDegree = 0;

    private Region totalRegion;
    private Path path;

    public PieChartView(Context context){
        this(context, null);
    }

    public PieChartView(Context context, AttributeSet attrs){
        this(context, attrs, 0);
    }

    public PieChartView(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);
        init();
    }

    private void initTestModel(){
        int[] colors = {Color.BLACK};
        float[] percents = {100F};
        String[] descs = {"java"};
        for (int i = 0; i < colors.length; i++){
            mPieModelList.add(new PieModel(descs[i], colors[i], percents[i]));
        }
    }

    public void setPieModelData(ArrayList<PieModel> pieModelList){
        mPieModelList.clear();
        mPieModelList = pieModelList;
        invalidate();
    }

    private void init(){
        mPieModelList.clear();
        //initTestModel();
        Log.d("zhangdi", "pieModeList size:" + mPieModelList.size());

        piePaint = new Paint();
        piePaint.setStyle(Paint.Style.FILL);
        piePaint.setAntiAlias(true);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        containerWidth = containerHeight = getMinContainerSize(getMeasuredWidth(), getMeasuredHeight());
        containerCenterX = (float) containerWidth / 2;
        containerCenterY = (float) containerHeight / 2;
        containerRadius = containerWidth * radiusScale / 2;
        if (containerRectF == null){
            containerRectF = new RectF(containerCenterX - containerRadius, containerCenterY - containerRadius, containerCenterX + containerRadius, containerCenterY + containerRadius);
        }

        if (focusRectF == null){
            focusRectF = new RectF();
        }
        if (totalRegion == null){
            totalRegion = new Region(0, 0, containerWidth, containerHeight);
        }

        setMeasuredDimension(containerWidth, containerHeight);
    }

    private int getMinContainerSize(int height, int width){
        int minSize;
        if (height >= width){
            minSize = width - getLeft() - getRight();
        }else{
            minSize = height - getTop() - getBottom();
        }
        return minSize >= DEFAULT_WIDTH ? minSize : DEFAULT_WIDTH;
    }


    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        for (int i = 0; i < mPieModelList.size(); i++){
            if (mPieModelList.size() == 1)
                intervalDegree = 0;
            PieModel pieModel = mPieModelList.get(i);
            float swapDegree = (360 - mPieModelList.size() * intervalDegree) * pieModel.getPercent();
            piePaint.setColor(pieModel.getColor());

            Region region = new Region();
            path = new Path();
            if (!pieModel.isFocus()){//判断piemodel当前是否被选中
                //开始绘画普通扇形
                path.moveTo(containerCenterX, containerCenterY);
                path.lineTo((float) (containerRadius * Math.cos((currentDegree * Math.PI) / 180)), (float) (containerCenterY * Math.sin((currentDegree * Math.PI) / 180)));
                path.addArc(containerRectF, currentDegree, swapDegree);
                //path.arcTo(containerRectF,currentDegree,swapDegree);
                path.lineTo(containerCenterX, containerCenterY);
                region.setPath(path, totalRegion);
                canvas.drawPath(path, piePaint);
                //canvas.drawArc(containerRectF,currentDegree,swapDegree,true,piePaint);
            }else{//画突出的扇形
                //确定突出扇形的rectF圆心
                float focusCenterX = (float) (containerCenterX + centerOffset * Math.cos((swapDegree / 2 + currentDegree) * Math.PI / 180));
                float focusCenterY = (float) (containerCenterY + centerOffset * Math.sin((swapDegree / 2 + currentDegree) * Math.PI / 180));
                focusRectF.set(focusCenterX - containerRadius, focusCenterY - containerRadius, focusCenterX + containerRadius, focusCenterY + containerRadius);

                path.moveTo(focusCenterX, focusCenterY);
                path.lineTo((float) (containerRadius * Math.cos((currentDegree * Math.PI) / 180)), (float) (containerCenterY * Math.sin((currentDegree * Math.PI) / 180)));
                path.addArc(focusRectF, currentDegree, swapDegree);
                //path.arcTo(focusRectF,currentDegree,swapDegree);
                path.lineTo(focusCenterX, focusCenterY);
                region.setPath(path, totalRegion);
                canvas.drawPath(path, piePaint);
                //canvas.drawArc(focusRectF,currentDegree,swapDegree,true,piePaint);
            }
            //为每个piemodel 设置region,方便判断哪个扇形被点击
            pieModel.setRegion(region);
            if (showPieText){//support show text
                float startPointX = (float) (containerRadius * Math.cos((swapDegree / 2 + currentDegree) * Math.PI / 180)) + containerCenterX;
                float startPointY = (float) (containerRadius * Math.sin((swapDegree / 2 + currentDegree) * Math.PI / 180)) + containerCenterY;

                float endPointX = (float) ((containerRadius + 10) * Math.cos((swapDegree / 2 + currentDegree) * Math.PI / 180)) + containerCenterX;
                float endPointY = (float) ((containerRadius + 10) * Math.sin((swapDegree / 2 + currentDegree) * Math.PI / 180)) + containerCenterY;
                Log.d("zhangdi", "startPoint:" + startPointX + "," + startPointY);
                Log.d("zhangdi", "endPoint:" + endPointX + "," + endPointY);
                canvas.drawLine(startPointX, startPointY, endPointX, endPointY, piePaint);
                //horizontal path location
                float newEndPointX;
                if (Math.cos((swapDegree / 2 + currentDegree) * Math.PI / 180) >= 0){//水平线方向向右
                    newEndPointX = endPointX + 20;
                    canvas.drawLine(endPointX, endPointY, newEndPointX, endPointY, piePaint);
                    piePaint.setTextAlign(Paint.Align.LEFT);
                    canvas.drawText(pieModel.getDesc(), newEndPointX, endPointY, piePaint);
                }else{//左边的pieModel
                    newEndPointX = endPointX - 20;
                    canvas.drawLine(endPointX, endPointY, newEndPointX, endPointY, piePaint);
                    piePaint.setTextAlign(Paint.Align.RIGHT);
                    canvas.drawText(pieModel.getDesc(), newEndPointX, endPointY, piePaint);
                }


            }

            path.close();


            currentDegree = currentDegree + swapDegree + intervalDegree;
        }


    }


    @Override
    public boolean onTouchEvent(MotionEvent event){
        int action = event.getAction();

        switch (action){

            case MotionEvent.ACTION_DOWN:
                int x = (int) event.getX();
                int y = (int) event.getY();
                if(mPieModelList.size() == 1)return false;
                for (PieModel model : mPieModelList){
                    if (model.inRegion(x, y)){
                        model.setFocus(true);
                        Log.d("zhangdi", "click model" + model.getDesc());
                    }else
                        model.setFocus(false);
                    //todo refresh
                    //Log.d("zhangdi","click model1111"+model.getDesc());

                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                //do nothing for now
                break;
        }
        return super.onTouchEvent(event);
    }

    public float getRadiusScale(){
        return radiusScale;
    }

    public void setRadiusScale(float radiusScale){
        this.radiusScale = radiusScale;
        invalidate();
    }

    public float getIntervalDegree(){
        return intervalDegree;
    }

    public void setIntervalDegree(float intervalDegree){
        this.intervalDegree = intervalDegree;
        invalidate();
    }

    public float getCenterOffset(){
        return centerOffset;
    }

    public void setCenterOffset(float centerOffset){
        this.centerOffset = centerOffset;
        invalidate();
    }

    public boolean isShowPieText(){
        return showPieText;
    }

    public void setShowPieText(boolean showPieText){
        this.showPieText = showPieText;
        invalidate();
    }

    class PieModel{

        private int color;
        private boolean isFocus = false;
        private float percent;
        private Region mRegion;
        private String desc;

        public PieModel(String desc, int color, float percent){
            this.desc = desc;
            this.color = color;
            this.percent = percent;
        }

        public float getPercent(){
            return this.percent / 100F;
        }

        public int getColor(){
            return this.color;
        }

        public boolean isFocus(){
            return this.isFocus;
        }

        public void setFocus(boolean isFocus){
            this.isFocus = isFocus;
        }

        public void setRegion(Region region){
            this.mRegion = region;
        }

        public String getDesc(){
            return this.desc;
        }

        public boolean inRegion(int x, int y){
            if (mRegion != null && mRegion.contains(x, y)){
                return true;
            }
            return false;
        }
    }
}
