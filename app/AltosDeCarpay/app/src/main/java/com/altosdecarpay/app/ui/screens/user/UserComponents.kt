package com.altosdecarpay.app.ui.screens.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altosdecarpay.app.ui.theme.*

enum class UserTab(val label: String, val icon: ImageVector) {
    RESERVAS("Mis reservas", Icons.Outlined.CalendarToday),
    EXPLORAR("Explorar", Icons.Outlined.Explore),
    AYUDA("Ayuda", Icons.Outlined.ChatBubbleOutline),
    PERFIL("Perfil", Icons.Outlined.PersonOutline)
}

@Composable
fun UserTopBar(
    onNotificationsClick: () -> Unit = {}
) {
    Surface(
        color = CarpayBackground,
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
                    tint = CarpayPrimaryGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Altos de carpay",
                        fontFamily = FontFamily.Serif,
                        fontSize = 18.sp,
                        color = CarpayTextPrimary
                    )
                    Text(
                        text = "ÁREA DE USUARIO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = CarpayTextSecondary
                    )
                }
            }

            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(CarpayGreenLight)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notificaciones",
                    tint = CarpayPrimaryGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun UserBottomBar(
    selectedTab: UserTab,
    onTabSelected: (UserTab) -> Unit
) {
    NavigationBar(
        containerColor = CarpaySurfaceCard,
        tonalElevation = 4.dp
    ) {
        UserTab.entries.forEach { tab ->
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
fun UserScreenHeader(
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