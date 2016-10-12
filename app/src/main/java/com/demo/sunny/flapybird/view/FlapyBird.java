package com.demo.sunny.flapybird.view;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import com.demo.sunny.flapybird.R;
import com.demo.sunny.flapybird.util.Util;

import java.util.ArrayList;
import java.util.List;

public class FlapyBird extends SurfaceView implements Callback, Runnable
{
    private Bird mBird;
    private Bitmap mBirdBitmap;
    /**
     * 地板
     */
    private Floor mFloor;
    private Bitmap mFloorBg;
    private int mSpeed;
    private Paint mPaint;
    /**
     * 当前View的尺寸
     */
    private int mWidth;
    private int mHeight;
    private RectF mGamePanelRect = new RectF();

    /**
     * 背景
     */
    private Bitmap mBg;

    private SurfaceHolder mHolder;
    /**
     * 与SurfaceHolder绑定的Canvas
     */
    private Canvas mCanvas;
    /**
     * 用于绘制的线程
     */
    private Thread t;

    /**
     * 线程的控制开关
     */
    private boolean isRunning;

    /**
     * *********管道相关**********************
     */
    /**
     * 管道
     */
    private Bitmap mPipeTop;
    private Bitmap mPipeBottom;
    private RectF mPipeRect;
    private int mPipeWidth;
    /**
     * 管道的宽度 60dp
     */
    private static final int PIPE_WIDTH = 60;

    private List<Pipe> mPipes = new ArrayList<Pipe>();

    /**
     * 分数
     */
    private final int[] mNums = new int[] { R.drawable.n0, R.drawable.n1,
            R.drawable.n2, R.drawable.n3, R.drawable.n4, R.drawable.n5,
            R.drawable.n6, R.drawable.n7, R.drawable.n8, R.drawable.n9 };
    private Bitmap[] mNumBitmap;

    private int mGrade = 0;
    /**
     * 单个数字的高度的1/15
     */
    private static final float RADIO_SINGLE_NUM_HEIGHT = 1 / 15f;
    /**
     * 单个数字的宽度
     */
    private int mSingleGradeWidth;
    /**
     * 单个数字的高度
     */
    private int mSingleGradeHeight;
    /**
     * 单个数字的范围
     */
    private RectF mSingleNumRectF;
    //运动
    //省略了一些代码

    private enum GameStatus
    {
        WAITTING, RUNNING, STOP;
    }

    /**
     * 记录游戏的状态
     */
    private GameStatus mStatus = GameStatus.WAITTING;

    /**
     * 触摸上升的距离，因为是上升，所以为负值
     */
    private static final int TOUCH_UP_SIZE = -16;
    /**
     * 将上升的距离转化为px；这里多存储一个变量，变量在run中计算
     *
     */
    private final int mBirdUpDis = Util.dp2px(getContext(), TOUCH_UP_SIZE);

    private int mTmpBirdDis;
    /**
     * 鸟自动下落的距离
     */
    private final int mAutoDownSpeed = Util.dp2px(getContext(), 3);
    /**
     * 两个管道间距离
     */
    private final int PIPE_DIS_BETWEEN_TWO = Util.dp2px(getContext(), 300);
    /**
     * 记录移动的距离，达到 PIPE_DIS_BETWEEN_TWO 则生成一个管道
     */
    private int mTmpMoveDistance;
    /**
     * 记录需要移除的管道
     */
    private List<Pipe> mNeedRemovePipe = new ArrayList<Pipe>();
    private int mRemovedPipe = 0;

    /**
     * 子弹
     */
    private Bitmap bulletBg;

    private List<Bullet> bullets = new ArrayList<Bullet>();
    private int bulSpeed;
    private int mBulletWidth;
    /**
     * 子弹的宽度 20dp
     */
    private static final int BULLET_SIZE = 30;

    public FlapyBird(Context context)
    {
        this(context, null);
    }

    public FlapyBird(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);

        mHolder = getHolder();
        mHolder.addCallback(this);
        setZOrderOnTop(true);// 设置画布 背景透明
        mHolder.setFormat(PixelFormat.TRANSLUCENT);

        // 设置可获得焦点
        setFocusable(true);
        setFocusableInTouchMode(true);
        // 设置常亮
        this.setKeepScreenOn(true);
        //初始化地图
        initBitmaps();
        // 初始化速度
        mSpeed = Util.dp2px(getContext(), 2);
        bulSpeed=Util.dp2px(getContext(), 10);
        //管道
        mPipeWidth = Util.dp2px(getContext(), PIPE_WIDTH);

        mBulletWidth= Util.dp2px(context, BULLET_SIZE);

    }
    /**
     * 处理一些逻辑上的计算
     */
    private void logic()
    {
        switch (mStatus)
        {
            case RUNNING:
                mGrade = 0;
                // 更新我们地板绘制的x坐标，地板移动
                mFloor.setX(mFloor.getX() - mSpeed);
                logicPipe();
                logicBullet();
                // 计算分数
                mGrade += mRemovedPipe;
                for (Pipe pipe : mPipes)
                {
                    if (pipe.getX() + mPipeWidth < mBird.getX())
                    {
                        mGrade++;
                    }
                }
                //默认下落，点击时瞬间上升
                mTmpBirdDis += mAutoDownSpeed;
                mBird.setY(mBird.getY() + mTmpBirdDis);
                checkGameOver();

                break;

            case STOP: // 鸟落下
                // 如果鸟还在空中，先让它掉下来
                if (mBird.getY() < mFloor.getY() - mBird.getWidth())
                {
                    mTmpBirdDis += mAutoDownSpeed;
                    mBird.setY(mBird.getY() + mTmpBirdDis);
                } else
                {
                    mStatus = GameStatus.WAITTING;
                    initPos();
                }
                break;
            default:
                break;
        }
    }

    private void logicPipe()
    {
        // 管道移动
        for (Pipe pipe : mPipes)
        {
            if (pipe.getX() < -mPipeWidth)
            {
                mNeedRemovePipe.add(pipe);
                mRemovedPipe++;
                continue;
            }
            pipe.setX(pipe.getX() - mSpeed);
        }
        // 移除管道
        mPipes.removeAll(mNeedRemovePipe);
        mNeedRemovePipe.clear();

        // Log.e("TAG", "现存管道数量：" + mPipes.size());

        // 管道
        mTmpMoveDistance += mSpeed;
        // 生成一个管道
        if (mTmpMoveDistance >= PIPE_DIS_BETWEEN_TWO)
        {
            Pipe pipe = new Pipe(getContext(), getWidth(), getHeight(),
                    mPipeTop, mPipeBottom);
            mPipes.add(pipe);
            mTmpMoveDistance = 0;
        }
    }

    private void logicBullet()
    {
        if(bullets.size()>0) {
        // 管道移动
        for (Bullet bullet : bullets) {
            if (bullet.getX() < -mBulletWidth) {
                // 移除管道
                bullets.remove(bullet);
                continue;
            }
            bullet.setX(bullet.getX() - bulSpeed);
        }
        }
        // 生成一个管道
        if (bullets.size() < 1)
        {
            Bullet bullet = new Bullet(getContext(), getWidth(), getHeight(),bulletBg);
            bullets.add(bullet);
        }
    }

    /**
     * 重置鸟的位置等数据
     */
    private void initPos()
    {
        mPipes.clear();
        //立即增加一个
        mPipes.add(new Pipe(getContext(), getWidth(), getHeight(), mPipeTop,
                mPipeBottom));
        //子弹清空
        bullets.clear();
        //立即增加一个
        bullets.add(new Bullet(getContext(), getWidth(), getHeight(),bulletBg));

        //重置鸟的位置
        mBird.resetHeigt();
        //重置下落速度
        // 重置下落速度
        mTmpBirdDis = 0;
        mTmpMoveDistance = 0 ;
        mRemovedPipe = 0;                                                                                                                                        mTmpMoveDistance = 0 ;
    }

    private void checkGameOver()
    {

        // 如果触碰地板，gg
        if (mBird.getY() > mFloor.getY() - mBird.getHeight())
        {
            mStatus = GameStatus.STOP;
        }
        // 如果撞到管道
        for (Pipe wall : mPipes)
        {
            //已经穿过的
            if (wall.getX() + mPipeWidth < mBird.getX())
            {
                continue;
            }
            if (wall.touchBird(mBird))
            {
                mStatus = GameStatus.STOP;
                break;
            }
        }
        if(bullets.size()>0) {
            // 如果撞到管道
            for (Bullet bullet : bullets) {
                //已经穿过的
                if (bullet.getX() + mBulletWidth < mBird.getX()) {
                    continue;
                }
                if (bullet.touchBird(mBird)) {
                    mStatus = GameStatus.STOP;
                    break;
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {

        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN)
        {
            switch (mStatus)
            {
                case WAITTING:
                    mStatus = GameStatus.RUNNING;
                    break;
                case RUNNING:
                    mTmpBirdDis = mBirdUpDis;
                    break;
            }

        }

        return true;

    }
    /**
     * 初始化图片
     */
    private void initBitmaps()
    {
        mBg = loadImageByResId(R.drawable.bg1);
        mBirdBitmap = loadImageByResId(R.drawable.b1);
        mFloorBg = loadImageByResId(R.drawable.floor_bg2);

        mPipeTop = loadImageByResId(R.drawable.g2);
        mPipeBottom = loadImageByResId(R.drawable.g1);

        mNumBitmap = new Bitmap[mNums.length];
        for (int i = 0; i < mNumBitmap.length; i++)
        {
            mNumBitmap[i] = loadImageByResId(mNums[i]);
        }
        bulletBg=loadImageByResId(R.drawable.bullet);
    }

    private void draw()
    {
        try
        {
            // 获得canvas
            mCanvas = mHolder.lockCanvas();
            if (mCanvas != null)
            {
                drawBg();
                drawBird();
                drawFloor();
                drawPipes();
                drawGrades();
                drawBullet();
            }
        } catch (Exception e)
        {
        } finally
        {
            if (mCanvas != null)
                mHolder.unlockCanvasAndPost(mCanvas);
        }
    }

    /**
     * 绘制背景
     */
    private void drawBg()
    {
        mCanvas.drawBitmap(mBg, null, mGamePanelRect, null);
    }


    private void drawBird()
    {
        mBird.draw(mCanvas);
    }

    private void drawFloor()
    {
        mFloor.draw(mCanvas, mPaint);
    }

    /**
     * 绘制管道
     */
    private void drawPipes()
    {
        for (Pipe pipe : mPipes)
        {
            pipe.draw(mCanvas, mPipeRect);
        }
    }
    //画子弹
    private void drawBullet()
    {
        for (Bullet bullet : bullets)
        {
            bullet.draw(mCanvas);
        }
    }
    /**
     * 绘制分数
     */
    private void drawGrades()
    {
        String grade = mGrade + "";
        mCanvas.save(Canvas.MATRIX_SAVE_FLAG);
        mCanvas.translate(mWidth / 2 - grade.length() * mSingleGradeWidth / 2,
                1f / 8 * mHeight);
        // draw single num one by one
        for (int i = 0; i < grade.length(); i++)
        {
            String numStr = grade.substring(i, i + 1);
            int num = Integer.valueOf(numStr);
            mCanvas.drawBitmap(mNumBitmap[num], null, mSingleNumRectF, null);
            mCanvas.translate(mSingleGradeWidth, 0);
        }
        mCanvas.restore();

    }
    /**
     * 初始化尺寸相关
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);

        mWidth = w;
        mHeight = h;
        mGamePanelRect.set(0, 0, w, h);
        // 初始化mBird
        mBird = new Bird(getContext(), mWidth, mHeight, mBirdBitmap);
        // 初始化地板
        mFloor = new Floor(mWidth, mHeight, mFloorBg);

        // 初始化管道范围
        mPipeRect = new RectF(0, 0, mPipeWidth, mHeight);
        Pipe pipe = new Pipe(getContext(), w, h, mPipeTop, mPipeBottom);
        mPipes.add(pipe);

        Bullet bullet = new Bullet(getContext(), w, h, bulletBg);
        bullets.add(bullet);

        // 初始化分数
        mSingleGradeHeight = (int) (h * RADIO_SINGLE_NUM_HEIGHT);
        mSingleGradeWidth = (int) (mSingleGradeHeight * 1.0f
                / mNumBitmap[0].getHeight() * mNumBitmap[0].getWidth());
        mSingleNumRectF = new RectF(0, 0, mSingleGradeWidth, mSingleGradeHeight);
    }

    /**
     * 根据resId加载图片
     *
     * @param resId
     * @return
     */
    private Bitmap loadImageByResId(int resId)
    {
        return BitmapFactory.decodeResource(getResources(), resId);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {

        // 开启线程
        isRunning = true;
        t = new Thread(this);
        t.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        // 通知关闭线程
        isRunning = false;
    }

    @Override
    public void run()
    {
        while (isRunning)
        {
            long start = System.currentTimeMillis();
            logic();
            draw();
            long end = System.currentTimeMillis();

            try
            {
                if (end - start < 50)
                {
                    Thread.sleep(50 - (end - start));
                }
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }

        }

    }
}

