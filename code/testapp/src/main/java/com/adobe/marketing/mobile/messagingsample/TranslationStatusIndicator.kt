/*
 Copyright 2025 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.messagingsample

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat

/**
 * Custom view that displays the translation model status as a colored circle.
 * 
 * Status colors:
 * - Red: Model not cached
 * - Orange: Model downloading (with pulsing animation)
 * - Green: Model cached and ready
 * - Gray: Translation disabled/not applicable
 */
class TranslationStatusIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Status {
        NOT_CACHED,
        DOWNLOADING,
        READY,
        DISABLED
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var currentStatus: Status = Status.DISABLED
    private var pulseAnimator: ValueAnimator? = null
    private var pulseScale = 1f

    init {
        // Set initial color
        updateColor()
    }

    /**
     * Updates the status of the translation indicator.
     */
    fun setStatus(status: Status) {
        if (currentStatus == status) return
        
        currentStatus = status
        updateColor()
        
        // Start or stop pulse animation based on status
        when (status) {
            Status.DOWNLOADING -> startPulseAnimation()
            else -> stopPulseAnimation()
        }
        
        invalidate()
    }

    /**
     * Gets the current status.
     */
    fun getStatus(): Status = currentStatus

    private fun updateColor() {
        val colorRes = when (currentStatus) {
            Status.NOT_CACHED -> R.color.translationStatusNotCached
            Status.DOWNLOADING -> R.color.translationStatusDownloading
            Status.READY -> R.color.translationStatusReady
            Status.DISABLED -> R.color.translationStatusDisabled
        }
        paint.color = ContextCompat.getColor(context, colorRes)
    }

    private fun startPulseAnimation() {
        stopPulseAnimation()
        
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.3f, 1f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                pulseScale = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        pulseScale = 1f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val baseRadius = minOf(width, height) / 2f * 0.8f
        val radius = baseRadius * pulseScale

        canvas.drawCircle(centerX, centerY, radius, paint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Make it a square based on the smaller dimension
        val size = minOf(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        setMeasuredDimension(size, size)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopPulseAnimation()
    }
}

