package com.example.ui.components

import android.content.Context
import android.view.Gravity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

enum class ToastType {
    ERROR, SUCCESS, INFO
}

class TopToastState {
    var message by mutableStateOf<String?>(null)
        private set
    var type by mutableStateOf(ToastType.ERROR)
        private set

    fun show(msg: String, toastType: ToastType = ToastType.ERROR) {
        message = msg
        type = toastType
    }

    fun dismiss() {
        message = null
    }
}

@Composable
fun rememberTopToastState(): TopToastState {
    return remember { TopToastState() }
}

fun showSystemTopToast(context: Context, message: String) {
    try {
        val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 120)
        toast.show()
    } catch (_: Exception) {}
}

@Composable
fun TopToastHost(
    state: TopToastState,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(2600)
            state.dismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .zIndex(9999f)
            .padding(top = 12.dp, start = 20.dp, end = 20.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = state.message != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            state.message?.let { msg ->
                val (bgColor, borderColor, iconTint, icon) = when (state.type) {
                    ToastType.ERROR -> Quadruple(
                        Color(0xFF1E1010),
                        Color(0x55FF5252),
                        Color(0xFFFF5252),
                        Icons.Default.ErrorOutline
                    )
                    ToastType.SUCCESS -> Quadruple(
                        Color(0xFF0F2417),
                        Color(0x554CAF50),
                        Color(0xFF4CAF50),
                        Icons.Default.CheckCircle
                    )
                    ToastType.INFO -> Quadruple(
                        Color(0xFF141F32),
                        Color(0x5560A5FA),
                        Color(0xFF60A5FA),
                        Icons.Default.Info
                    )
                }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    border = BorderStroke(1.dp, borderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
