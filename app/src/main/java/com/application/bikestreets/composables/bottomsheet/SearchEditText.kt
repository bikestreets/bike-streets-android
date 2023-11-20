package com.application.bikestreets.composables.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.application.bikestreets.theme.Dimens

@Composable
fun SearchEditText(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
        modifier = modifier.then(
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Dimens.defaultPadding,
                    vertical = Dimens.editTextVerticalPadding
                )
                .clip(RoundedCornerShape(8.dp))
                .background(color = Color.White)
        ),
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
        placeholder = { Text(text = hint) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search icon",
            )
        }
    )
}

@Composable
@Preview
fun SearchToEditTextPreview() {
    SearchEditText(value = "Test", onValueChange = { it }, hint = "Search", modifier = Modifier)
}