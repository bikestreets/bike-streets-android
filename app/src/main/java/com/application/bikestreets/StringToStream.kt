package com.application.bikestreets

import java.io.InputStream
import java.util.Scanner

class StringToStream {
    companion object {
        fun convert(input: InputStream): String {
            val scanner = Scanner(input).useDelimiter("\\A")
            return if (scanner.hasNext()) scanner.next() else ""
        }
    }
}
