package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IndigoPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("terms_conditions_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Terms & Conditions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("terms_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = IndigoPrimary.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = "Terms & Conditions",
                        tint = IndigoPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Expense Manager Terms & Conditions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Effective Date: October 12, 2026 • Version 1.0",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Section 1: Acceptance
            LegalSectionCard(
                title = "1. Acceptance of Terms",
                content = "By downloading, installing, accessing, or using Expense Manager (\"the Application\"), you agree to be bound by these Terms and Conditions. If you do not agree to these terms, please do not use the Application."
            )

            // Section 2: Account Security
            LegalSectionCard(
                title = "2. User Account & Responsibilities",
                content = "• Account Security: You are responsible for safeguarding your Google account credentials used to sign in to the Application.\n" +
                        "• Accuracy of Information: You are responsible for the accuracy of all financial transactions, amounts, and category choices entered into the Application.\n" +
                        "• Lawful Use: You agree not to use the Application for any unlawful or prohibited activities."
            )

            // Section 3: Financial Advice Disclaimer
            LegalSectionCard(
                title = "3. No Financial or Tax Advice Disclaimer",
                content = "Expense Manager is a self-service personal budgeting and tracking utility. THE APPLICATION DOES NOT PROVIDE CERTIFIED FINANCIAL, LEGAL, TAX, OR INVESTMENT ADVICE.\n\n" +
                        "All reports, charts, and budget overspending warnings are calculated automatically based on inputs provided by you and are intended solely for personal informational purposes."
            )

            // Section 4: Cloud Sync & Service Availability
            LegalSectionCard(
                title = "4. Cloud Synchronization & Service Availability",
                content = "• Cloud Backup: Data synchronization is provided via Google Firebase infrastructure.\n" +
                        "• Availability: While we aim to provide continuous service availability, we do not guarantee uninterrupted or error-free operation. Occasional maintenance or network interruptions may temporarily impact cloud sync."
            )

            // Section 5: Intellectual Property
            LegalSectionCard(
                title = "5. Intellectual Property Rights",
                content = "All right, title, and interest in and to Expense Manager—including its software code, visual design, icons, graphics, and trademarks—are and remain the exclusive property of the developers."
            )

            // Section 6: Limitation of Liability
            LegalSectionCard(
                title = "6. Limitation of Liability",
                content = "To the maximum extent permitted by applicable law, Expense Manager and its developers shall not be liable for any indirect, incidental, special, or consequential damages resulting from lost data, device issues, or financial decisions made using the Application."
            )

            // Section 7: Account Termination
            LegalSectionCard(
                title = "7. Termination & Account Deletion",
                content = "You may terminate your agreement with Expense Manager at any time by deleting your account using the in-app 'Delete Account' option in Settings and uninstalling the Application."
            )

            // Section 8: Governing Law & Contact
            LegalSectionCard(
                title = "8. Contact Information",
                content = "If you have any questions or concerns regarding these Terms and Conditions, please contact support:\n\n" +
                        "Email: ranaarunkumar5@gmail.com"
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
