package com.altosdecarpay.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altosdecarpay.app.ui.theme.*

enum class AdminTab(val label: String, val icon: ImageVector) {
    RESERVAS("Reservas", Icons.Outlined.CalendarToday),
    ESCANEAR("Escanear", Icons.Outlined.QrCodeScanner),
    TICKETS("Tickets", Icons.Outlined.Forum),
    PERFIL("Perfil", Icons.Outlined.PersonOutline)
}

@Composable
fun AdminTopBar(
    onNotificationsClick: (() -> Unit)? = null
) {
    // La campana abre el panel de avisos (antes no hacía nada).
    var verNotificaciones by remember { mutableStateOf(false) }
    if (verNotificaciones) {
        com.altosdecarpay.app.ui.components.NotificacionesDialog(esAdmin = true, onCerrar = { verNotificaciones = false })
    }
    Surface(
        color = CarpayPrimaryGreen,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Eco,
                    contentDescription = "Logo",
                    tint = CarpayWhite.copy(alpha = 0.9f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Altos de carpay",
                        fontFamily = FontFamily.Serif,
                        fontSize = 18.sp,
                        color = CarpayWhite
                    )
                    Text(
                        text = "ADMINISTRACIÓN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = CarpayWhite.copy(alpha = 0.75f)
                    )
                }
            }

            IconButton(
                onClick = { onNotificationsClick?.invoke() ?: run { verNotificaciones = true } },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(CarpayWhite.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notificaciones",
                    tint = CarpayWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AdminBottomBar(
    selectedTab: AdminTab,
    onTabSelected: (AdminTab) -> Unit
) {
    NavigationBar(
        containerColor = CarpaySurfaceCard,
        tonalElevation = 4.dp
    ) {
        AdminTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CarpayPrimaryGreen,
                    selectedTextColor = CarpayPrimaryGreen,
                    indicatorColor = CarpayGreenLight,
                    unselectedIconColor = CarpayTextSecondary,
                    unselectedTextColor = CarpayTextSecondary
                )
            )
        }
    }
}

@Composable
fun AdminScreenHeader(
    title: String,
    subtitle: String? = null,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showBackButton) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Volver",
                        tint = CarpayTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = title,
                fontFamily = FontFamily.Serif,
                fontSize = 28.sp,
                color = CarpayTextPrimary
            )
        }
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = CarpayTextSecondary
            )
        }
    }
}