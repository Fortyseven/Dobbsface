package com.bytestemplar.subgeniuswatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public abstract class WatchFaceBase extends CanvasWatchFaceService
{
    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis( 1 );

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine()
    {
        return new Engine();
    }

    /***********************************************************************************/
    private static class EngineHandler extends Handler
    {
        private final WeakReference<Engine> mWeakReference;

        public EngineHandler( WatchFaceBase.Engine reference )
        {
            mWeakReference = new WeakReference<>( reference );
        }

        @Override
        public void handleMessage( Message msg )
        {
            WatchFaceBase.Engine engine = mWeakReference.get();
            if ( engine != null ) {
                switch ( msg.what ) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    protected abstract void OnCreate();

    protected abstract void OnDraw( Time time, Canvas canvas, Rect bounds );

    protected abstract void OnDrawAmbient( Time time, Canvas canvas, Rect bounds );

    protected abstract void OnShape( boolean is_round );

    protected abstract void OnAmbientChange( boolean is_ambient_mode, boolean is_low_bit_ambient );

    protected int getRealWidth() { return getResources().getDisplayMetrics().widthPixels; }

    protected int getRealHeight() { return getResources().getDisplayMetrics().heightPixels; }

    protected abstract void OnFaceTapped( int x, int y, long eventTime );


    /***********************************************************************************/
    private class Engine extends CanvasWatchFaceService.Engine
    {
        final Handler mUpdateTimeHandler = new EngineHandler( this );
        boolean mRegisteredTimeZoneReceiver = false;
        boolean mAmbient;
        Time    mTime;

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive( Context context, Intent intent )
            {
                mTime.clear( intent.getStringExtra( "time-zone" ) );
                mTime.setToNow();
            }
        };


        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate( SurfaceHolder holder )
        {
            super.onCreate( holder );

            setWatchFaceStyle( new WatchFaceStyle.Builder( WatchFaceBase.this ).setCardPeekMode( WatchFaceStyle.PEEK_MODE_VARIABLE ).setBackgroundVisibility( WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE ).setAcceptsTapEvents( true ).setShowSystemUiTime( false ).build() );

            Resources resources = WatchFaceBase.this.getResources();

            mTime = new Time();

            OnCreate();
        }

        @Override
        public void onDestroy()
        {
            mUpdateTimeHandler.removeMessages( MSG_UPDATE_TIME );
            super.onDestroy();
        }


        @Override
        public void onVisibilityChanged( boolean visible )
        {
            super.onVisibilityChanged( visible );

            if ( visible ) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear( TimeZone.getDefault().getID() );
                mTime.setToNow();
            }
            else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver()
        {
            if ( mRegisteredTimeZoneReceiver ) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter( Intent.ACTION_TIMEZONE_CHANGED );
            WatchFaceBase.this.registerReceiver( mTimeZoneReceiver, filter );
        }

        private void unregisterReceiver()
        {
            if ( !mRegisteredTimeZoneReceiver ) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            WatchFaceBase.this.unregisterReceiver( mTimeZoneReceiver );
        }

        @Override
        public void onApplyWindowInsets( WindowInsets insets )
        {
            super.onApplyWindowInsets( insets );

            // Load resources that have alternate values for round watches.
            Resources resources = WatchFaceBase.this.getResources();
            boolean   isRound   = insets.isRound();
            OnShape( isRound );
        }

        @Override
        public void onPropertiesChanged( Bundle properties )
        {
            super.onPropertiesChanged( properties );
            mLowBitAmbient = properties.getBoolean( PROPERTY_LOW_BIT_AMBIENT, false );
        }

        @Override
        public void onTimeTick()
        {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged( boolean inAmbientMode )
        {
            super.onAmbientModeChanged( inAmbientMode );
            if ( mAmbient != inAmbientMode ) {
                mAmbient = inAmbientMode;
                OnAmbientChange( inAmbientMode, mLowBitAmbient );
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw( Canvas canvas, Rect bounds )
        {
            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            mTime.setToNow();

            // Draw the background.
            if ( isInAmbientMode() ) {
                OnDrawAmbient( mTime, canvas, bounds );
            }
            else {
                OnDraw( mTime, canvas, bounds );
            }
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer()
        {
            mUpdateTimeHandler.removeMessages( MSG_UPDATE_TIME );
            if ( shouldTimerBeRunning() ) {
                mUpdateTimeHandler.sendEmptyMessage( MSG_UPDATE_TIME );
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning()
        {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage()
        {
            invalidate();
            if ( shouldTimerBeRunning() ) {
                long timeMs  = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS - ( timeMs % INTERACTIVE_UPDATE_RATE_MS );
                mUpdateTimeHandler.sendEmptyMessageDelayed( MSG_UPDATE_TIME, delayMs );
            }
        }

        @Override
        public void onTapCommand( @TapType int tapType, int x, int y, long eventTime )
        {
            super.onTapCommand( tapType, x, y, eventTime );

            if ( tapType == CanvasWatchFaceService.TAP_TYPE_TAP ) {
                OnFaceTapped( x, y, eventTime );
            }
        }
    }
}
