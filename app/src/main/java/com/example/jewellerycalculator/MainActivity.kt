package com.example.jewellerycalculator

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textView = TextView(this).apply {
            text = """
                Jewellery Fair Price Calculator

                Gold Rate: Enter rate per gram
                Purity: Select 14K / 18K / 22K / 24K
                Gross Weight: Enter jewellery weight
                Stone Weight: Enter non-gold weight
                Diamond Carat: Enter diamond weight
                Diamond Rate: Enter rate per carat
                Wastage, Making & Miscellaneous: Enter percentages
                GST: Enter GST percentage

                The calculator will show:
                • Net gold weight
                • Gold value
                • Diamond value
                • Making/wastage/miscellaneous charges
                • GST
                • Final fair price
            """.trimIndent()

            textSize = 18f
            setPadding(40, 60, 40, 60)
        }

        setContentView(textView)
    }
}
