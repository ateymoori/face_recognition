package org.tensorflow.lite.examples.detection.clean.data.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.hardware.usb.UsbManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import org.tensorflow.lite.examples.detection.MyApplication
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.round

fun String.log(tag: String? = null): String {
    if (tag != null)
        Log.d(tag, this)
    else
        Log.d("debug_", this)
    return this
}

infix fun Double.round(decimals: Int): Double {
    var multiplier = 1.00
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}

fun View.hide() {
    this.visibility = View.GONE
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.visible(visible: Boolean? = true) {
    this.visibility = if (visible != false) View.VISIBLE else View.GONE
}

fun String.toast(): String {
    this.log("toast_")
    Toast.makeText(MyApplication.instance, this, Toast.LENGTH_LONG).show()
    return this
}

fun <T : androidx.recyclerview.widget.RecyclerView.ViewHolder> T.listen(event: (position: Int, type: Int) -> Unit): T {
    itemView.setOnClickListener {
        event.invoke(adapterPosition, itemViewType)
    }
    return this
}


fun Long.length() = when (this) {
    0.toLong() -> 1
    else -> log10(abs(toDouble())).toInt() + 1
}

fun String.getAge(): Int {
    return try {
        this.toInt()
    } catch (e: Exception) {
        0
    }
}


fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }
    })
}

inline fun <T : Any> ifLet(vararg elements: T?, closure: (List<T>) -> Unit) {
    if (elements.all { it != null }) {
        closure(elements.filterNotNull())
    }
}

fun ImageView.setImageDrawable(context: Context?, drawable: Int) {
    if (context != null)
        this.setImageDrawable(ContextCompat.getDrawable(context, drawable))

}


fun ImageView.loadFile(file: File) {
    Glide.with(this.context)
        .load(file)
        //.placeholder(R.drawable.avatar_placeholder)
        .centerCrop()
        .into(this)
}

fun ImageView.loadFile(drawable: Drawable?) {
    Glide.with(this.context)
        .load(drawable)
        //.placeholder(R.drawable.avatar_placeholder)
        .centerCrop()
        .into(this)
}

fun ImageView.loadUrl(url: String?) {
    Glide.with(this.context)
        .load(url)
        .centerCrop()
        .into(this)
}


fun String.toByteArray(): ByteArray {
    val charset = Charsets.UTF_8
    return toByteArray(charset)
}

fun Char.parseByte() = this.toString().toByte()


fun ByteArray?.getString(): String? {
    if (this == null) return null
    return String(this, StandardCharsets.UTF_8)
}

fun ByteArray.toHexString(): String {
    return this.joinToString("") {
        java.lang.String.format("%02x", it)
    }
}


fun Context.getDeviceList() = (getSystemService(Context.USB_SERVICE) as UsbManager).deviceList

fun String.random(length: Int): String {
    val charset = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    return List(length) { charset.random() }
        .joinToString("")
}
