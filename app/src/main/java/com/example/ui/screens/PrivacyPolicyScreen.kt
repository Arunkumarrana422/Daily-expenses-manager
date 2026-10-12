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
import androidx.compose.material.icons.filled.PrivacyTip
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
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("privacy_policy_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Privacy Policy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("privacy_back_button")
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
                        imageVector = Icons.Default.PrivacyTip,
                        contentDescription = "Privacy Policy",
                        tint = IndigoPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Expense Manager Privacy Policy",
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

            // Section 1: Overview
            LegalSectionCard(
                title = "1. Introduction",
                content = "Expense Manager (\"we\", \"our\", or \"app\") values your privacy and is committed to protecting your personal and financial information. This Privacy Policy explains how we collect, use, store, and safeguard your data when you use our mobile application."
            )

            // Section 2: Information We Collect
            LegalSectionCard(
                title = "2. Information We Collect",
                content = "We collect information necessary to provide personal budgeting and expense tracking services:\n\n" +
                        "• Google Account Profile: When you sign in using Google, we collect your display name, email address, and profile picture URL to create and identify your account.\n" +
                        "• Financial Transactions: Expense entries, income sources, category labels, transaction dates/times, payment methods, notes, and custom budget limits created by you.\n" +
                        "• Device & Network State: Internet connectivity status to sync your data with Google Firebase Firestore."
            )

            // Section 3: How We Use Your Data
            LegalSectionCard(
                title = "3. How We Use Your Information",
                content = "Your information is used solely for operating and improving Expense Manager:\n\n" +
                        "• To display financial summaries, spending analytics, and budget progress.\n" +
                        "• To securely sync your transactions across devices via Google Firebase.\n" +
                        "• To send local budget overspending and daily reminder alerts on your device.\n\n" +
                        "We DO NOT sell, rent, lease, or trade your personal or financial data to advertisers, data brokers, or third parties."
            )

            // Section 4: Data Security & Storage
            LegalSectionCard(
                title = "4. Data Storage & Security",
                content = "• Local Storage: Your records are cached locally on your device in an encrypted Room database.\n" +
                        "• Cloud Infrastructure: Online sync is powered by Google Firebase Firestore, protected by industry-standard TLS encryption in transit and strict user-isolated security rules in storage.\n" +
                        "• Access Control: Only your authenticated account can read or modify your data."
            )

            // Section 5: Third-Party Services
            LegalSectionCard(
                title = "5. Third-Party Services",
                content = "Expense Manager integrates with Google Cloud Services:\n\n" +
                        "• Google Credential Manager / Google Identity Services for user sign-in authentication.\n" +
                        "• Google Firebase Authentication & Firestore for cloud storage.\n\n" +
                        "These services process data according to Google's Privacy Policy (https://policies.google.com/privacy)."
            )

            // Section 6: User Control & Account Deletion
            LegalSectionCard(
                title = "6. Your Rights & Account Deletion",
                content = "You retain full control over your data:\n\n" +
                        "• Edit & Delete Records: You can edit or delete individual expenses, incomes, or budgets at any time.\n" +
                        "• Complete Account Deletion: You can permanently delete your account and all associated cloud data by tapping 'Delete Account' in Settings. All your Firestore collections and local database rows will be purged permanently."
            )

            // Section 7: Children's Privacy
            LegalSectionCard(
                title = "7. Children's Privacy",
                content = "Expense Manager does not knowingly collect personal information from children under the age of 13. If you believe a child has provided us with personal information, please contact us for immediate deletion."
            )

            // Section 8: Policy Updates & Contact
            LegalSectionCard(
                title = "8. Policy Updates & Contact Us",
                content = "We may update this Privacy Policy from time to time. Any changes will be reflected with a revised effective date in the app.\n\n" +
                        "For questions or data deletion requests, contact support:\n" +
                        "Email: ranaarunkumar5@gmail.com"
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun LegalSectionCard(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}
