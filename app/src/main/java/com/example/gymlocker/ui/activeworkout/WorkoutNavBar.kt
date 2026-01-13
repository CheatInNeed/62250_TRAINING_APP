package com.example.gymlocker.ui.activeworkout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlocker.ui.theme.GymLockerTheme

@Composable
fun WorkoutNumpadBar(
    modifier: Modifier = Modifier
) {
    val darkBackground = Color(0xFF1C1C1E)
    val buttonBackground = Color(0xFF2C2C2E)
    val buttonHighlight = Color(0xFF3A82F7)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = darkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            // Row 1: 1 2 3 [keyboard hide]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NumpadKey("1", buttonBackground)
                NumpadKey("2", buttonBackground)
                NumpadKey("3", buttonBackground)
                NumpadIconKey(
                    icon = Icons.Filled.KeyboardHide,
                    backgroundColor = buttonBackground,
                    contentDescription = "Hide keyboard"
                )
            }

            // Row 2: 4 5 6 [tune/settings]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NumpadKey("4", buttonBackground)
                NumpadKey("5", buttonBackground)
                NumpadKey("6", buttonBackground)
                NumpadIconKey(
                    icon = Icons.Filled.Tune,
                    backgroundColor = buttonBackground,
                    contentDescription = "Settings"
                )
            }

            // Row 3: 7 8 9 [- +]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NumpadKey("7", buttonBackground)
                NumpadKey("8", buttonBackground)
                NumpadKey("9", buttonBackground)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    NumpadIconKey(
                        icon = Icons.Filled.Remove,
                        backgroundColor = buttonBackground,
                        contentDescription = "Minus",
                        modifier = Modifier.weight(1f)
                    )
                    NumpadIconKey(
                        icon = Icons.Filled.Add,
                        backgroundColor = buttonBackground,
                        contentDescription = "Plus",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Row 4: , 0 [backspace] [Next]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NumpadKey(",", buttonBackground)
                NumpadKey("0", buttonBackground)
                NumpadIconKey(
                    icon = Icons.AutoMirrored.Filled.Backspace,
                    backgroundColor = buttonBackground,
                    contentDescription = "Backspace"
                )
                // Next button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(buttonHighlight)
                        .clickable { /* TODO */ },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Next",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.NumpadKey(
    text: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .weight(1f)
            .padding(horizontal = 4.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { /* TODO */ },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RowScope.NumpadIconKey(
    icon: ImageVector,
    backgroundColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .weight(1f)
            .padding(horizontal = 4.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { /* TODO */ },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WorkoutNumpadBarPreview() {
    GymLockerTheme {
        WorkoutNumpadBar()
    }
}

