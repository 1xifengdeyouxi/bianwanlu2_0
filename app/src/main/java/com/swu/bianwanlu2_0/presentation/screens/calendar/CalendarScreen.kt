package com.swu.bianwanlu2_0.presentation.screens.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.swu.bianwanlu2_0.ui.theme.Bianwanlu2_0Theme

@Composable
fun CalendarScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "日历")
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarScreenPreview() {
    Bianwanlu2_0Theme {
        CalendarScreen()
    }
}
