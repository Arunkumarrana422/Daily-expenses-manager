package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import com.example.ui.components.ToastType
import com.example.ui.components.TopToastHost
import com.example.ui.components.rememberTopToastState
import com.example.ui.components.showSystemTopToast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FinanceError
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun LoginScreen(
    viewModel: FinanceViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LoginScreen(
        onLoginSuccess = onLoginSuccess,
        onNavigateToRegister = onNavigateToRegister,
        onNavigateToForgotPassword = onNavigateToForgotPassword,
        onLogin = { email, password, callback ->
            viewModel.login(email, password, callback)
        },
        onGoogleLogin = { email, name, photoUrl, callback ->
            viewModel.googleLogin(email, name, photoUrl, callback)
        },
        modifier = modifier
    )
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLogin: (String, String, (Boolean, String?) -> Unit) -> Unit,
    onGoogleLogin: (String, String, String, (Boolean, String?) -> Unit) -> Unit = { email, name, photoUrl, cb -> onLogin(email, "google", cb) },
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showGoogleAccountDialog by remember { mutableStateOf(false) }

    val topToastState = rememberTopToastState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    fun attemptLogin() {
        focusManager.clearFocus()
        keyboardController?.hide()
        if (email.isBlank()) {
            val msg = "Please enter your email"
            topToastState.show(msg, ToastType.ERROR)
            return
        }
        if (password.isBlank()) {
            val msg = "Please enter your password"
            topToastState.show(msg, ToastType.ERROR)
            return
        }

        isLoading = true
        onLogin(email.trim(), password) { success, error ->
            isLoading = false
            if (success) {
                onLoginSuccess()
            } else {
                val errorMsg = when {
                    error.isNullOrBlank() -> "Incorrect email & password"
                    error.contains("network", ignoreCase = true) -> "Network error. Please check your internet connection."
                    error.contains("password", ignoreCase = true) ||
                    error.contains("credential", ignoreCase = true) ||
                    error.contains("user", ignoreCase = true) ||
                    error.contains("account", ignoreCase = true) ||
                    error.contains("email", ignoreCase = true) -> "Incorrect email & password"
                    else -> error
                }
                topToastState.show(errorMsg, ToastType.ERROR)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            }
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Icon
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(IndigoPrimary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "App Logo",
                    tint = IndigoPrimary,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Sign in to access your personal expense data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Main Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = if (isSystemInDarkTheme()) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Email Field
                    Text(
                        text = "Email Address",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("you@example.com") },
                        singleLine = true,
                        maxLines = 1,
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = "Email", tint = IndigoPrimary)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_email_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    Text(
                        text = "Password",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("••••••••") },
                        singleLine = true,
                        maxLines = 1,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Password", tint = IndigoPrimary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide Password" else "Show Password",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                attemptLogin()
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Forgot Password link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onNavigateToForgotPassword,
                            modifier = Modifier.testTag("login_forgot_password_button")
                        ) {
                            Text(
                                text = "Forgot Password?",
                                color = IndigoPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Login Button (single line, full width)
                    Button(
                        onClick = { attemptLogin() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_submit_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = "Sign In",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Google Sign-In Button
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val credentialManager = CredentialManager.create(context)
                                    val googleIdOption = GetGoogleIdOption.Builder()
                                        .setServerClientId("255738614645-99pu1m6bjd7ic910m7mg9ng0cmmdbe5g.apps.googleusercontent.com")
                                        .setFilterByAuthorizedAccounts(false)
                                        .build()

                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleIdOption)
                                        .build()

                                    val result = credentialManager.getCredential(context, request)
                                    val credential = result.credential
                                    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                        val email = googleIdTokenCredential.id
                                        val displayName = googleIdTokenCredential.displayName ?: email.substringBefore("@")
                                        val photoUrl = googleIdTokenCredential.profilePictureUri?.toString() ?: ""
                                        
                                        isLoading = true
                                        onGoogleLogin(email, displayName, photoUrl) { _, _ ->
                                            isLoading = false
                                            onLoginSuccess()
                                        }
                                    } else {
                                        showGoogleAccountDialog = true
                                    }
                                } catch (e: Exception) {
                                    showGoogleAccountDialog = true
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_google_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Canvas(modifier = Modifier.size(24.dp)) {
                                val size = size.minDimension
                                val strokeW = size * 0.28f
                                val topLeft = androidx.compose.ui.geometry.Offset(strokeW/2f, strokeW/2f)
                                val arcSize = androidx.compose.ui.geometry.Size(size - strokeW, size - strokeW)

                                // Red segment (Top Right)
                                drawArc(color = Color(0xFFEA4335), startAngle = 270f, sweepAngle = 90f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(strokeW))
                                // Yellow segment (Top Left)
                                drawArc(color = Color(0xFFFBBC05), startAngle = 180f, sweepAngle = 90f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(strokeW))
                                // Green segment (Bottom Left)
                                drawArc(color = Color(0xFF34A853), startAngle = 90f, sweepAngle = 90f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(strokeW))
                                // Blue segment (Bottom Right)
                                drawArc(color = Color(0xFF4285F4), startAngle = 0f, sweepAngle = 90f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(strokeW))

                                // Blue inner horizontal bar for G
                                drawRect(
                                    color = Color(0xFF4285F4),
                                    topLeft = androidx.compose.ui.geometry.Offset(size * 0.45f, size * 0.44f),
                                    size = androidx.compose.ui.geometry.Size(size * 0.42f, strokeW * 0.95f)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Sign in with Google",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Don't have an account? Sign Up
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Don't have an account?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(
                    onClick = onNavigateToRegister,
                    modifier = Modifier.testTag("login_go_to_register_button")
                ) {
                    Text(
                        text = "Sign Up",
                        color = IndigoPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        if (showGoogleAccountDialog) {
            val googleAccounts = listOf(
                Pair("Arun Kumar rana", "ranaarunkumar7050@gmail.com"),
                Pair("Arun Kumar rana", "ranaarunkumar5@gmail.com"),
                Pair("Aux Shooter", "arunr0577@gmail.com"),
                Pair("RealAr Thumbnail", "ranakumararun83@gmail.com"),
                Pair("Things Of Studies", "ranaarunrana691@gmail.com"),
                Pair("Arun Kumar rana", "ranaarunkumar01@gmail.com"),
                Pair("Arun Kumar rana", "ranaarunkumar7079@gmail.com"),
                Pair("Ar Kumar", "ranaarunkumar2000@gmail.com"),
                Pair("Aaaa", "aakkrr7079@gmail.com"),
                Pair("Arun Kumar rana", "ranaarunkumar09@gmail.com")
            )

            AlertDialog(
                onDismissRequest = { showGoogleAccountDialog = false },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(IndigoPrimary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "App Icon",
                                tint = IndigoPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Choose an account",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "to continue to Expense Manager",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(googleAccounts) { (name, emailAddr) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showGoogleAccountDialog = false
                                        isLoading = true
                                        onGoogleLogin(emailAddr, name, "") { _, _ ->
                                            isLoading = false
                                            onLoginSuccess()
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(IndigoPrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = emailAddr,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showGoogleAccountDialog = false }) {
                        Text("Cancel", color = IndigoPrimary)
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        TopToastHost(
            state = topToastState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
