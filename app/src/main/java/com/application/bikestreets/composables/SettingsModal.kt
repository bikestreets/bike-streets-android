package com.application.bikestreets.composables

import androidx.compose.runtime.Composable

@Composable
fun SettingsModal(onCloseSettingsClicked: () -> Unit) {
    // TODO: create a basic modal that contains the close and card shape
    BikeStreetsDialog(onCloseClicked = { onCloseSettingsClicked() }, true)
//    Dialog(onDismissRequest = { onCloseSettingsClicked() }) {
//        Card(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(200.dp)
//                .padding(16.dp),
//            shape = RoundedCornerShape(16.dp),
//        ) {
//            Text(
//                text = "This is a minimal dialog",
//                modifier = Modifier
//                    .fillMaxSize()
//                    .wrapContentSize(Alignment.Center),
//                textAlign = TextAlign.Center,
//            )
//        }
//    }
}