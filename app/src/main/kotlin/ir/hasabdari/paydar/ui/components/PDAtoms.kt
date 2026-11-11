package ir.hasabdari.paydar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hasabdari.paydar.ui.theme.*

// ═══════════════════════════════════════════════════════════
// PDButton (Primary)
// ═══════════════════════════════════════════════════════════
@Composable
fun PDButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: String? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Teal700,
            contentColor = Color.White,
            disabledContainerColor = Slate200,
            disabledContentColor = Slate400
        ),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = if (icon != null) "$icon $text" else text,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// PDSecondaryButton (Amber)
// ═══════════════════════════════════════════════════════════
@Composable
fun PDSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Amber500,
            contentColor = Slate900,
            disabledContainerColor = Slate200,
            disabledContentColor = Slate400
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

// ═══════════════════════════════════════════════════════════
// PDOutlinedButton
// ═══════════════════════════════════════════════════════════
@Composable
fun PDOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, Teal200),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal700)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

// ═══════════════════════════════════════════════════════════
// PDTextField
// ═══════════════════════════════════════════════════════════
@Composable
fun PDTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    leadingIcon: String? = null
) {
    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label, fontSize = 12.sp) },
            placeholder = placeholder?.let { { Text(it, color = Slate400, fontSize = 13.sp) } },
            isError = isError,
            singleLine = singleLine,
            minLines = minLines,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            leadingIcon = leadingIcon?.let { { Text(it, fontSize = 16.sp) } },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Teal700,
                unfocusedBorderColor = Slate200,
                focusedLabelColor = Teal700,
                cursorColor = Teal700
            )
        )
        if (errorMessage != null) {
            Text(
                errorMessage,
                color = Rose600,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}