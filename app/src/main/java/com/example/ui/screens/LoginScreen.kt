package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.ToastType
import com.example.ui.components.TopToastHost
import com.example.ui.components.rememberTopToastState
import com.example.ui.components.showSystemTopToast
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.FinanceViewModel
import com.example.utils.NetworkObserver

@Composable
fun LoginScreen(
    viewModel: FinanceViewModel,
    onLoginSuccess: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenTermsAndConditions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isGoogleLoading by remember { mutableStateOf(false) }
    val topToastState = rememberTopToastState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("login_screen")
    ) {
        TopToastHost(state = topToastState)

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
                    .size(88.dp)
                    .background(IndigoPrimary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "App Logo",
                    tint = IndigoPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Expense Manager",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sign in with your Google account to manage your finances and sync data securely.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Quick Instructions Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_instruction_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GppGood,
                            contentDescription = "Security Instructions",
                            tint = IndigoPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Instructions & Security",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Tap 'Sign in with Google' below to log in securely without passwords.\n• Your financial entries are synced to your Google Account on Firebase Cloud.\n• You can edit, export, or delete your account anytime in Settings.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Legal Terms & Privacy Policy Disclaimer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "By continuing, you agree to our",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    TextButton(
                        onClick = onOpenTermsAndConditions,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier.testTag("open_terms_button")
                    ) {
                        Text(
                            text = "Terms & Conditions",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary
                        )
                    }
                    Text(
                        text = " and ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onOpenPrivacyPolicy,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier.testTag("open_privacy_button")
                    ) {
                        Text(
                            text = "Privacy Policy",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

             // 3. Primary Action: Google Sign-In Button
             OutlinedButton(
                 onClick = {
                     if (!NetworkObserver(context).checkConnection()) {
                         return@OutlinedButton
                     }
                     isGoogleLoading = true
                     coroutineScope.launch {
                         try {
                             val credentialManager = CredentialManager.create(context)
                             val serverClientId = try {
                                 context.getString(R.string.default_web_client_id)
                             } catch (e: Exception) {
                                 "255738614645-j1reg4tppdfs3h61thoi6i4vf8kd9sjr.apps.googleusercontent.com"
                             }

                             val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = serverClientId)
                                 .build()

                             val request = GetCredentialRequest.Builder()
                                 .addCredentialOption(signInOption)
                                 .build()

                             val result = try {
                                 credentialManager.getCredential(context, request)
                             } catch (e: NoCredentialException) {
                                 val googleIdOption = GetGoogleIdOption.Builder()
                                     .setServerClientId(serverClientId)
                                     .setFilterByAuthorizedAccounts(false)
                                     .build()
                                 val fallbackRequest = GetCredentialRequest.Builder()
                                     .addCredentialOption(googleIdOption)
                                     .build()
                                 credentialManager.getCredential(context, fallbackRequest)
                             }

                             val credential = result.credential
                             if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                 val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                 val idToken = googleIdTokenCredential.idToken
                                 val email = googleIdTokenCredential.id
                                 val displayName = googleIdTokenCredential.displayName ?: email.substringBefore("@")
                                 val photoUrl = googleIdTokenCredential.profilePictureUri?.toString() ?: ""

                                 viewModel.googleLoginWithToken(idToken, email, displayName, photoUrl) { success, error ->
                                     isGoogleLoading = false
                                     if (success) {
                                         onLoginSuccess()
                                     } else {
                                         topToastState.show(error ?: "Google Sign-In failed", ToastType.ERROR)
                                     }
                                 }
                             } else {
                                 isGoogleLoading = false
                                 topToastState.show("Google Sign-In unavailable", ToastType.ERROR)
                             }
                         } catch (e: GetCredentialCancellationException) {
                             isGoogleLoading = false
                         } catch (e: NoCredentialException) {
                             isGoogleLoading = false
                         } catch (e: Exception) {
                             isGoogleLoading = false
                             val msg = e.localizedMessage ?: "Sign-in error"
                             if (!msg.contains("cancel", ignoreCase = true) && !msg.contains("No credential", ignoreCase = true)) {
                                 topToastState.show("Google Sign-In: $msg", ToastType.ERROR)
                             }
                         }
                     }
                 },
                 enabled = !isGoogleLoading,
                 modifier = Modifier
                     .fillMaxWidth()
                     .height(52.dp)
                     .testTag("google_sign_in_button"),
                 shape = CircleShape,
                 border = BorderStroke(1.5.dp, IndigoPrimary),
                 colors = ButtonDefaults.outlinedButtonColors(
                     containerColor = Color.Transparent,
                     contentColor = MaterialTheme.colorScheme.onBackground
                 )
             ) {
                 if (isGoogleLoading) {
                     CircularProgressIndicator(
                         color = IndigoPrimary,
                         modifier = Modifier.size(24.dp),
                         strokeWidth = 2.5.dp
                     )
                 } else {
                     Row(
                         verticalAlignment = Alignment.CenterVertically,
                         horizontalArrangement = Arrangement.Center
                     ) {
                         Icon(
                             painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_google_g),
                             contentDescription = "Google",
                             tint = Color.Unspecified,
                             modifier = Modifier.size(20.dp)
                         )
                         Spacer(modifier = Modifier.width(10.dp))
                         Text(
                             text = "Sign in with Google",
                             fontSize = 16.sp,
                             fontWeight = FontWeight.Bold,
                             color = MaterialTheme.colorScheme.onBackground
                         )
                     }
                 }
             }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
