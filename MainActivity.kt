package com.example.jewellerycalculator

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale
import kotlin.math.round

class MainActivity : AppCompatActivity() {
    private lateinit var goldRate: EditText
    private lateinit var purity: Spinner
    private lateinit var grossWeight: EditText
    private lateinit var stoneWeight: EditText
    private lateinit var diamondCt: EditText
    private lateinit var diamondRate: EditText
    private lateinit var wastage: EditText
    private lateinit var labour: EditText
    private lateinit var misc: EditText
    private lateinit var gst: EditText
    private lateinit var result: TextView

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) runOcr(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    private fun buildUi(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
        }

        fun label(s: String) = TextView(this).apply {
            text = s
            textSize = 14f
            setPadding(0, 12, 0, 4)
        }
        fun edit(hint: String, value: String = "") = EditText(this).apply {
            this.hint = hint
            setText(value)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        val title = TextView(this).apply {
            text = "Jewellery Fair Price Calculator"
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        root.addView(title)

        val upload = Button(this).apply {
            text = "📷 Upload Bill / Jewellery Image → Auto Read"
            setOnClickListener { pickImage.launch("image/*") }
        }
        root.addView(upload)

        goldRate = edit("Gold rate ₹/g", "8903")
        root.addView(label("Gold Rate (per gram)")); root.addView(goldRate)

        root.addView(label("Gold Purity"))
        purity = Spinner(this)
        purity.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            arrayOf("14K (58.5%)", "18K (75.0%)", "22K (91.6%)", "24K (99.9%)"))
        root.addView(purity)

        grossWeight = edit("Gross weight in grams")
        stoneWeight = edit("Non-gold/stones weight in grams", "0")
        diamondCt = edit("Diamond weight in carats", "0")
        diamondRate = edit("Diamond rate ₹/ct", "0")
        wastage = edit("Wastage %", "0")
        labour = edit("Labour / making %", "0")
        misc = edit("Other charges %", "0")
        gst = edit("GST %", "3")

        listOf(
            "Gross Weight" to grossWeight,
            "Stone / Non-gold Weight" to stoneWeight,
            "Diamond Weight" to diamondCt,
            "Diamond Rate" to diamondRate,
            "Wastage" to wastage,
            "Labour / Making" to labour,
            "Miscellaneous" to misc,
            "GST" to gst
        ).forEach { (n,v) -> root.addView(label(n)); root.addView(v) }

        val calculate = Button(this).apply {
            text = "CALCULATE FAIR PRICE"
            setOnClickListener { calculate() }
        }
        root.addView(calculate)

        result = TextView(this).apply {
            textSize = 16f
            setPadding(0, 24, 0, 24)
        }
        root.addView(result)

        val scroll = ScrollView(this)
        val holder = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.removeAllViews()
        holder.addView(title); holder.addView(upload)
        listOf(
            "Gold Rate (₹/g)" to goldRate, "Gold Purity" to purity,
            "Gross Weight (g)" to grossWeight, "Stone / Non-gold Weight (g)" to stoneWeight,
            "Diamond Weight (ct)" to diamondCt, "Diamond Rate (₹/ct)" to diamondRate,
            "Wastage (%)" to wastage, "Labour / Making (%)" to labour,
            "Miscellaneous (%)" to misc, "GST (%)" to gst
        ).forEach { (n,v) -> holder.addView(label(n)); holder.addView(v) }
        holder.addView(calculate); holder.addView(result)
        scroll.addView(holder)
        root.addView(scroll)
        return root
    }

    private fun num(e: EditText) = e.text.toString().toDoubleOrNull() ?: 0.0

    private fun purityFactor(): Double = when (purity.selectedItemPosition) {
        0 -> 0.585
        1 -> 0.750
        2 -> 0.916
        else -> 0.999
    }

    private fun calculate() {
        val rate = num(goldRate)
        val gross = num(grossWeight)
        val nonGold = num(stoneWeight)
        val netGold = (gross - nonGold).coerceAtLeast(0.0)
        val pf = purityFactor()
        val pureGoldEquivalent = netGold * pf
        val goldValue = pureGoldEquivalent * rate

        val dCt = num(diamondCt)
        val dRate = num(diamondRate)
        val diamondValue = dCt * dRate

        val waste = num(wastage) / 100.0
        val labourPct = num(labour) / 100.0
        val miscPct = num(misc) / 100.0
        val effectiveMakingPct = (waste + labourPct + miscPct) * 100.0
        val makingValue = goldValue * (waste + labourPct + miscPct)

        val subtotal = goldValue + diamondValue + makingValue
        val gstValue = subtotal * num(gst) / 100.0
        val fairTotal = subtotal + gstValue

        val formatted = String.format(Locale.US, "₹%,.2f", fairTotal)
        result.text = """
            FAIR PRICE BREAKDOWN

            Net gold: %.3f g
            Purity factor: %.1f%%
            Gold value: ₹%,.2f

            Diamond: %.3f ct
            Diamond value: ₹%,.2f

            Effective making/wastage/misc: %.2f%%
            Making + wastage + misc: ₹%,.2f

            Pre-GST subtotal: ₹%,.2f
            GST: ₹%,.2f

            ESTIMATED FAIR TOTAL: $formatted

            Note: Diamond rate is an input assumption. The app does not certify natural/lab-grown status from an image alone.
        """.trimIndent().format(
            netGold, pf * 100, goldValue, dCt, diamondValue,
            effectiveMakingPct, makingValue, subtotal, gstValue
        )
    }

    private fun runOcr(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    val numbers = Regex("""\d+(?:\.\d+)?""").findAll(text).map { it.value }.toList()
                    Toast.makeText(this,
                        "Bill read. Review fields before calculating. Found ${numbers.size} numeric values.",
                        Toast.LENGTH_LONG).show()
                    // Conservative OCR: do not blindly map numbers to financial fields.
                    // Future version can use field-specific keywords such as WEIGHT, RATE, GST, DIAMOND, etc.
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Could not read image. Please enter values manually.", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Image could not be opened.", Toast.LENGTH_LONG).show()
        }
    }
}
