package com.example.runup.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.runup.ui.theme.Black
import com.example.runup.ui.theme.PlaceholderTextColor
import com.example.runup.ui.theme.White


@Composable
fun RunupTextfield(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderText: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textcolor: Color = Black,
    placeholdercolor: Color = PlaceholderTextColor,
    containercolor: Color = White,
    indicatorcolor: Color = Color.Transparent,
    isPassword: Boolean = false,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholderText) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        colors = TextFieldDefaults.colors(
            focusedTextColor = textcolor,
            unfocusedTextColor = textcolor,
            focusedPlaceholderColor = placeholdercolor,
            unfocusedPlaceholderColor = placeholdercolor,
            focusedContainerColor = containercolor,
            unfocusedContainerColor = containercolor,
            focusedIndicatorColor = indicatorcolor,
            unfocusedIndicatorColor = indicatorcolor,
        ),
        shape = RoundedCornerShape(5.dp),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier
    )
}