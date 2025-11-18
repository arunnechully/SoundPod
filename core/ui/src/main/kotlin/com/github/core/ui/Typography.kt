package com.github.core.ui

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.isAvailableOnDevice
import androidx.compose.ui.unit.sp
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.parcelize.parcelableCreator

@Parcelize
@Immutable
data class Typography(
    internal val style: TextStyle,
    internal val fontFamily: BuiltInFontFamily
) : Parcelable {
    val xxs by lazy { style.copy(fontSize = 12.sp) }
    val xs by lazy { style.copy(fontSize = 14.sp) }
    val s by lazy { style.copy(fontSize = 16.sp) }
    val m by lazy { style.copy(fontSize = 18.sp) }
    val l by lazy { style.copy(fontSize = 20.sp) }
    val xxl by lazy { style.copy(fontSize = 32.sp) }

    fun copy(color: Color) = Typography(
        style = style.copy(color = color),
        fontFamily = fontFamily
    )

    companion object : Parceler<Typography> {
        override fun Typography.write(parcel: Parcel, flags: Int) = SavedTypography(
            color = style.color,
            fontFamily = fontFamily,
            includeFontPadding = style.platformStyle?.paragraphStyle?.includeFontPadding == true
        ).writeToParcel(parcel, flags)

        override fun create(parcel: Parcel) =
            parcelableCreator<SavedTypography>().createFromParcel(parcel).let {
                typographyOf(
                    color = it.color,
                    fontFamily = it.fontFamily,
                    applyFontPadding = it.includeFontPadding
                )
            }
    }
}

@Parcelize
@SuppressLint("ParcelCreator")
data class SavedTypography(
    val color: @RawValue ParcelableColor,
    val fontFamily: @RawValue BuiltInFontFamily,
    val includeFontPadding: Boolean
) : Parcelable


private val googleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

@Composable
fun googleFontsAvailable(): Boolean {
    val context = LocalContext.current

    return runCatching {
        googleFontsProvider.isAvailableOnDevice(context.applicationContext)
    }.getOrElse {
        it.printStackTrace()
        if (it is IllegalStateException) Log.e(
            "Typography",
            "Google Fonts certificates don't match. Is the user using a VPN?"
        )
        false
    }
}

private val poppinsFonts = listOf(
    Font(
        resId = R.font.poppins_w300,
        weight = FontWeight.Light
    ),
    Font(
        resId = R.font.poppins_w400,
        weight = FontWeight.Normal
    ),
    Font(
        resId = R.font.poppins_w500,
        weight = FontWeight.Medium
    ),
    Font(
        resId = R.font.poppins_w600,
        weight = FontWeight.SemiBold
    ),
    Font(
        resId = R.font.poppins_w700,
        weight = FontWeight.Bold
    )
)

private val poppinsFontFamily = FontFamily(poppinsFonts)

@Parcelize
enum class BuiltInFontFamily(internal val googleFont: @kotlinx.parcelize.RawValue GoogleFont?) : Parcelable {
    Poppins(null),
    Roboto(GoogleFont("Roboto")),
    Montserrat(GoogleFont("Montserrat")),
    Nunito(GoogleFont("Nunito")),
    Rubik(GoogleFont("Rubik")),
    System(null);

    companion object : Parceler<BuiltInFontFamily> {
        override fun BuiltInFontFamily.write(parcel: Parcel, flags: Int) = parcel.writeString(name)
        override fun create(parcel: Parcel) = BuiltInFontFamily.valueOf(parcel.readString()!!)
    }
}

private fun googleFontsFamilyFrom(font: BuiltInFontFamily) = font.googleFont?.let {
    FontFamily(
        listOf(
            Font(
                googleFont = it,
                fontProvider = googleFontsProvider,
                weight = FontWeight.Light
            ),
            Font(
                googleFont = it,
                fontProvider = googleFontsProvider,
                weight = FontWeight.Normal
            ),
            Font(
                googleFont = it,
                fontProvider = googleFontsProvider,
                weight = FontWeight.Medium
            ),
            Font(
                googleFont = it,
                fontProvider = googleFontsProvider,
                weight = FontWeight.SemiBold
            ),
            Font(
                googleFont = it,
                fontProvider = googleFontsProvider,
                weight = FontWeight.Bold
            )
        ) + poppinsFonts
    )
}

fun typographyOf(
    color: Color,
    fontFamily: BuiltInFontFamily,
    applyFontPadding: Boolean
): Typography {
    val textStyle = TextStyle(
        fontFamily = when {
            fontFamily == BuiltInFontFamily.System -> FontFamily.Default
            fontFamily.googleFont != null -> googleFontsFamilyFrom(fontFamily)
            else -> poppinsFontFamily
        },
        fontWeight = FontWeight.Normal,
        color = color,
        platformStyle = PlatformTextStyle(includeFontPadding = applyFontPadding)
    )

    return Typography(
        style = textStyle,
        fontFamily = fontFamily
    )
}
