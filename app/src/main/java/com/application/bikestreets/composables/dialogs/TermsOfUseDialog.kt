package com.application.bikestreets.composables.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.application.bikestreets.R
import com.application.bikestreets.theme.Colors

@Composable
fun TermsOfUseDialog(onTermsAccepted: () -> Unit, viewFullTerms: () -> Unit) {
    BikeStreetsDialog(
        onCloseClicked = { onTermsAccepted() },
        title = "Terms of Use",
        dialogContent = { TermsOfUseContent(viewFullTerms = { viewFullTerms() }) },
        confirmationText = "Accept Terms",
        isDismissible = false
    )
}

@Composable
fun TermsOfUseContent(viewFullTerms: () -> Unit) {
    val annotatedText = buildAnnotatedString {
        // Styling the link part
        withStyle(
            style = SpanStyle(
                color = Colors.link,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append("Full Terms")
        }

    }
    Column {
        Text(
            stringResource(R.string.terms_instructions)
        )
        Spacer(modifier = Modifier.padding(vertical = 5.dp))
        ClickableText(
            text = annotatedText,
            onClick = { viewFullTerms() }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TermsOfUseDialogPreview() {
    BikeStreetsDialog(
        onCloseClicked = { },
        title = "Terms of Use",
        dialogContent = { TermsOfUseContent(viewFullTerms = { }) },
        confirmationText = "Accept Terms"
    )
}
