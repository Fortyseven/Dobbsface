/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytestemplar.subgeniuswatchface;


import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.format.Time;

import org.joda.time.DateTime;
import org.joda.time.Duration;

public class BT_SubgeniusWatchface extends WatchFaceBase
{
    private Typeface mTypeface;

    private Resources mResources;

    private Bitmap mBackgroundBitmap;
    private Bitmap mBackgroundSecondaryBitmap;
    private Paint  mBackgroundPaint;
    private Paint  mTextPaint;

    private float mXOffset, mYOffset;
    private float mYOffsetCountdown;
    private float mTextSize;

    private boolean mIsRound = false;

//    private boolean mIsBlinkerOn = true;
//    private long mLastBlinkTime;

    private enum WATCH_MODE
    {
        NORMAL,
        COUNTDOWN
    }

    private WATCH_MODE mWatchMode = WATCH_MODE.NORMAL;

    protected void OnCreate()
    {
        mResources = getResources();

        mBackgroundBitmap = Bitmap.createScaledBitmap( BitmapFactory.decodeResource( mResources, R.drawable.dobbsface1 ), getRealWidth(), getRealHeight(), true );
        mBackgroundSecondaryBitmap = Bitmap.createScaledBitmap( BitmapFactory.decodeResource( mResources, R.drawable.dobbsface_bg ), getRealWidth(), getRealHeight(), true );

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor( mResources.getColor( R.color.background ) );

        mTextPaint = new Paint();
        mTextPaint = createTextPaint( Color.BLACK );
    }

    protected void OnDraw( Time time, Canvas canvas, Rect bounds )
    {
        String text;

        canvas.drawColor( Color.BLACK );


        switch ( mWatchMode ) {
            case NORMAL:

//                if ( ( System.currentTimeMillis() - mLastBlinkTime ) > 1000 ) {
//                    mLastBlinkTime = System.currentTimeMillis();
//                    mIsBlinkerOn = !mIsBlinkerOn;
//                }

                mTextPaint.setColor( Color.BLACK );
                mTextPaint.setTextSize( mTextSize );

                canvas.drawBitmap( mBackgroundBitmap, 0, 0, mBackgroundPaint );

                //text = String.format( "%d%s%02d", convert24to12( time.hour ), mIsBlinkerOn ? ":" : " ", time.minute );
                text = String.format( "%d:%02d", convert24to12( time.hour ), time.minute );
                canvas.drawText( text, bounds.centerX() - ( mTextPaint.measureText( text ) / 2 ), mYOffset, mTextPaint );
                break;

            case COUNTDOWN:
                canvas.drawBitmap( mBackgroundSecondaryBitmap, 0, 0, mBackgroundPaint );
                mTextPaint.setTextSize( mResources.getDimension( R.dimen.digital_text_size_countdown ) );

                // We're doing a drop-shadow here.
                float y = mYOffsetCountdown + 1;
                for ( String line : getXDayCountdown().split( "\n" ) ) {
                    mTextPaint.setColor( Color.BLACK );
                    canvas.drawText( line, bounds.centerX() - ( mTextPaint.measureText( line ) / 2 ), y, mTextPaint );

                    mTextPaint.setColor( Color.RED );
                    canvas.drawText( line, bounds.centerX() - ( mTextPaint.measureText( line ) / 2 ), y - 1, mTextPaint );
                    y -= mTextPaint.ascent();
                }

                break;
        }
    }

    protected void OnDrawAmbient( Time time, Canvas canvas, Rect bounds )
    {
        canvas.drawColor( Color.BLACK );

        mTextPaint.setColor( Color.WHITE );
        String text = String.format( "%d:%02d", convert24to12( time.hour ), time.minute );
        canvas.drawText( text, bounds.centerX() - ( mTextPaint.measureText( text ) / 2 ), mYOffset, mTextPaint );

    }

    @Override
    protected void OnShape( boolean isRound )
    {
        mIsRound = isRound;
        mXOffset = mResources.getDimension( isRound ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset );

        mYOffset = mResources.getDimension( isRound ? R.dimen.digital_y_offset_round : R.dimen.digital_y_offset );
        mYOffsetCountdown = mResources.getDimension( isRound ? R.dimen.digital_y_offset_countdown_round : R.dimen.digital_y_offset_countdown );
        mTextSize = mResources.getDimension( isRound ? R.dimen.digital_text_size_round : R.dimen.digital_text_size );
        mTextPaint.setTextSize( mTextSize );
    }

    @Override
    protected void OnAmbientChange( boolean in_ambient_mode, boolean is_low_bit_ambient )
    {
        if ( is_low_bit_ambient ) {
            mTextPaint.setAntiAlias( !in_ambient_mode );
        }
    }

    @Override
    protected void OnFaceTapped( int x, int y, long eventTime )
    {
        if ( mWatchMode == WATCH_MODE.NORMAL ) {
            mWatchMode = WATCH_MODE.COUNTDOWN;
        }
        else {
            mWatchMode = WATCH_MODE.NORMAL;
        }
    }

    private Paint createTextPaint( int textColor )
    {
        Typeface typeface = Typeface.createFromAsset( getAssets(), "diploma.ttf" );

        Paint paint = new Paint();
        paint.setColor( textColor );
        paint.setTypeface( typeface );
        paint.setAntiAlias( true );
        return paint;
    }

    private int convert24to12( int hour )
    {
        hour = hour % 12;
        if ( hour == 0 ) {
            hour = 12;
        }

        return hour;
    }

    private String getXDayCountdown()
    {
        DateTime today = DateTime.now();
        //DateTime today = new DateTime( 1998, 7, 6, 4, 20, 32, 0 );
        DateTime xday = new DateTime( today.getYear(), 7, 5, 7, 0, 0, 0 );

        if ( today.isAfter( xday ) ) {
            xday = new DateTime( today.getYear() + 1, 7, 5, 7, 0, 0, 0 );
        }

        Duration dur = new Duration( today, xday );

        StringBuilder sb = new StringBuilder();

        if ( dur.getStandardDays() > 0 ) {
            sb.append( dur.getStandardDays() );
            sb.append( " Day" );
            if ( dur.getStandardDays() > 1 ) {
                sb.append( "s" );
            }
            sb.append( "\n" );
        }

        long hours = dur.getStandardHours() % 24;
        if ( hours > 0 ) {
            sb.append( hours );
            sb.append( " Hour" );
            if ( hours > 1 ) {
                sb.append( "s" );
            }
            sb.append( "\n" );
        }

        long mins = dur.getStandardMinutes() % 60;
        if ( mins > 0 ) {
            sb.append( mins );
            sb.append( " Minute" );
            if ( mins > 1 ) {
                sb.append( "s" );
            }
            sb.append( "\n" );
        }

        long secs = dur.getStandardSeconds() % 60;
        if ( secs > 0 ) {
            sb.append( secs );
            sb.append( " Second" );
            if ( secs > 1 ) {
                sb.append( "s" );
            }
            sb.append( "\n" );
        }

        sb.append( "Until X-Day!" );

        return sb.toString();
    }
}
