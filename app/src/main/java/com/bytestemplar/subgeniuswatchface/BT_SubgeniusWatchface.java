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

public class BT_SubgeniusWatchface extends WatchFaceBase
{
    private Typeface mTypeface;

    private Resources mResources;

    private Bitmap mBackgroundBitmap;
    private Paint  mBackgroundPaint;
    private Paint  mTextPaint;

    private float mXOffset, mYOffset;

    protected void OnCreate()
    {
        mResources = getResources();

        mBackgroundBitmap = Bitmap.createScaledBitmap( BitmapFactory.decodeResource( mResources, R.drawable.dobbsface1 ), getRealWidth(), getRealHeight(), true );

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor( mResources.getColor( R.color.background ) );

        mTextPaint = new Paint();
        mTextPaint = createTextPaint( Color.BLACK );
    }

    protected void OnDraw( Time time, Canvas canvas, Rect bounds )
    {
        canvas.drawColor( Color.BLACK );

        mTextPaint.setColor( Color.BLACK );
        canvas.drawBitmap( mBackgroundBitmap, 0, 0, mBackgroundPaint );
        //String text = String.format( "%d:%02d:%02d", convert24to12( time.hour ), time.minute, time.second );
        String text = String.format( "%d:%02d", convert24to12( time.hour ), time.minute );
        canvas.drawText( text, bounds.centerX() - ( mTextPaint.measureText( text ) / 2 ), mYOffset, mTextPaint );

    }

    protected void OnDrawAmbient( Time time, Canvas canvas, Rect bounds )
    {
        canvas.drawColor( Color.BLACK );

        mTextPaint.setColor( Color.WHITE );
        String text = String.format( "%d:%02d", convert24to12( time.hour ), time.minute );
        canvas.drawText( text, bounds.centerX() - ( mTextPaint.measureText( text ) / 2 ), mYOffset, mTextPaint );

    }

    private int convert24to12( int hour )
    {
        hour = hour % 12;
        if ( hour == 0 ) {
            hour = 12;
        }

        return hour;
    }

    @Override
    protected void OnShape( boolean isRound )
    {
        mXOffset = mResources.getDimension( isRound ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset );
        mYOffset = mResources.getDimension( isRound ? R.dimen.digital_y_offset_round : R.dimen.digital_y_offset );
        float textSize = mResources.getDimension( isRound ? R.dimen.digital_text_size_round : R.dimen.digital_text_size );
        mTextPaint.setTextSize( textSize );
    }

    @Override
    protected void OnAmbientChange( boolean in_ambient_mode, boolean is_low_bit_ambient )
    {
        if ( is_low_bit_ambient ) {
            mTextPaint.setAntiAlias( !in_ambient_mode );
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

}
