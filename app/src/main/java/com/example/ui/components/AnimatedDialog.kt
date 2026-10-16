package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.IndigoPrimary

@Composable
fun AnimatedDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
        if (!visibleState.targetState && !visibleState.currentState) {
            onDismissRequest()
        }
    }

    BackHandler(enabled = visibleState.targetState) {
        visibleState.targetState = false
    }

    Dialog(
        onDismissRequest = {
            visibleState.targetState = false
        },
        properties = DialogProperties(
            dismissOnBackPress = properties.dismissOnBackPress,
            dismissOnClickOutside = false,
            securePolicy = properties.securePolicy,
            usePlatformDefaultWidth = properties.usePlatformDefaultWidth
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    visibleState.targetState = false
                }
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {}
            ) {
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = scaleIn(
                        initialScale = 0.8f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(150)),
                    exit = scaleOut(
                        targetScale = 0.8f,
                        animationSpec = tween(150)
                    ) + fadeOut(animationSpec = tween(150))
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun AnimatedAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation: Dp = 6.dp
) {
    AnimatedDialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                icon?.let {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 16.dp)
                    ) {
                        it()
                    }
                }
                title?.let {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 16.dp)
                    ) {
                        it()
                    }
                }
                text?.let {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 24.dp)
                    ) {
                        it()
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dismissButton?.invoke()
                    if (dismissButton != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    confirmButton()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    datePickerState: DatePickerState
) {
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
        if (!visibleState.targetState && !visibleState.currentState) {
            onDismissRequest()
        }
    }

    BackHandler(enabled = visibleState.targetState) {
        visibleState.targetState = false
    }

    Dialog(
        onDismissRequest = { visibleState.targetState = false },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    visibleState.targetState = false
                }
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {}
            ) {
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = scaleIn(
                        initialScale = 0.8f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(150)),
                    exit = scaleOut(
                        targetScale = 0.8f,
                        animationSpec = tween(150)
                    ) + fadeOut(animationSpec = tween(150))
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DatePicker(state = datePickerState)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { visibleState.targetState = false }) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        onConfirm()
                                        visibleState.targetState = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                                ) {
                                    Text("OK")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    timePickerState: TimePickerState
) {
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
        if (!visibleState.targetState && !visibleState.currentState) {
            onDismissRequest()
        }
    }

    BackHandler(enabled = visibleState.targetState) {
        visibleState.targetState = false
    }

    Dialog(
        onDismissRequest = { visibleState.targetState = false },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    visibleState.targetState = false
                }
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {}
            ) {
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = scaleIn(
                        initialScale = 0.8f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(150)),
                    exit = scaleOut(
                        targetScale = 0.8f,
                        animationSpec = tween(150)
                    ) + fadeOut(animationSpec = tween(150))
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 460.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Select Time",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            )

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                TimePicker(
                                    state = timePickerState,
                                    colors = TimePickerDefaults.colors(
                                        clockDialColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        selectorColor = IndigoPrimary,
                                        containerColor = Color.Transparent,
                                        periodSelectorBorderColor = IndigoPrimary,
                                        periodSelectorSelectedContainerColor = IndigoPrimary,
                                        periodSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        periodSelectorSelectedContentColor = Color.White,
                                        periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        timeSelectorSelectedContainerColor = IndigoPrimary.copy(alpha = 0.2f),
                                        timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onSurface,
                                        timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { visibleState.targetState = false }) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        onConfirm()
                                        visibleState.targetState = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                                ) {
                                    Text("OK")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
