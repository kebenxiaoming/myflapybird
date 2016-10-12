package com.demo.sunny.flapybird.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.Log;

import com.demo.sunny.flapybird.util.Util;

import java.util.Random;

/**
 * lalala
 * Created by sunny on 2016/10/12.
 */

public class Bullet {
    /**
     * 子弹的宽度 30dp
     */
    private static final int BULLET_SIZE = 30;

    /**
     * 子弹的横坐标
     */
    private int x;
    /**
     * 子弹的纵坐标
     */
    private int y;
    /**
     * 子弹的宽度
     */
    private int mWidth;
    /**
     * 子弹的高度
     */
    private int mHeight;

    /**
     * 子弹的bitmap
     */
    private Bitmap bitmap;

    private int mGameHeight ;

    private static Random random = new Random();
    /**
     * 鸟绘制的范围
     */
    private RectF rect = new RectF();

    /**
     * 距离上边的距离
     */
    private int height;

    public Bullet(Context context, int gameWidth, int gameHeight, Bitmap bullet)
    {
        // 默认从最左边出现
        x = gameWidth+500;
        mGameHeight=gameHeight;
        bitmap=bullet;
        // 计算子弹的宽度和高度
        mWidth = Util.dp2px(context, BULLET_SIZE);
        mHeight = (int) (mWidth * 1.0f / bitmap.getWidth() * bitmap.getHeight());
        randomHeight(gameHeight);
    }

    /**
     * 随机生成一个高度
     */
    private void randomHeight(int gameHeight)
    {
        height = random
                .nextInt((int) (gameHeight));
        height=(int)(height+gameHeight*(2 / 5));
        if(height>gameHeight*4/5){
            height=gameHeight*3/5;
        }
    }

    public void draw(Canvas mCanvas)
    {
        rect.set(x, height, x + mWidth, height + mHeight);
        mCanvas.drawBitmap(bitmap, null, rect, null);
    }
    public int getX()
    {
        return x;
    }

    public void setX(int x)
    {
        this.x = x;
    }

    public int getY()
    {
        return height;
    }

    public void setY(int y)
    {
        this.height = y;
    }
    /**
     * 判断和鸟是否触碰
     * @param mBird
     * @return
     */
    public boolean touchBird(Bird mBird)
    {
        /**
         * 如果bird已经触碰子弹
         */
        if (((this.x>mBird.getX())&&(this.x< mBird.getX()+mBird.getWidth())
                && (height>mBird.getY())&&(height< mBird.getY()+mBird.getHeight()))||((this.x+mWidth>mBird.getX())&&(this.x+mWidth< mBird.getX()+mBird.getWidth())
                && (height>mBird.getY())&&(height< mBird.getY()+mBird.getHeight()))||((this.x>mBird.getX())&&(this.x< mBird.getX()+mBird.getWidth())
                && (height+mHeight>mBird.getY())&&(height+mHeight< mBird.getY()+mBird.getHeight()))||((this.x+mWidth>mBird.getX())&&(this.x+mWidth< mBird.getX()+mBird.getWidth())
                && (height+mHeight>mBird.getY())&&(height+mHeight< mBird.getY()+mBird.getHeight())))
        {
            return true;
        }
        return false;

    }
}
