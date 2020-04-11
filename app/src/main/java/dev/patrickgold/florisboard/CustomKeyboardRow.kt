package dev.patrickgold.florisboard

import android.content.Context
import android.util.AttributeSet
import com.google.android.flexbox.FlexboxLayout

class CustomKeyboardRow : FlexboxLayout {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.customKeyboardRowStyle)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttrs: Int) : super(context, attrs, defStyleAttrs)
}
