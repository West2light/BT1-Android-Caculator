package com.example.bt_caculatorconstrainlayout

import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class MainActivity : AppCompatActivity() {
    // TextView hiển thị kết quả/toán hạng hiện tại
    private lateinit var displayView: TextView
    // TextView hiển thị biểu thức đang nhập
    private lateinit var expressionView: TextView
    // TextView hiển thị kết quả chuyển đổi tiền tệ
    private lateinit var currencyResultView: TextView
    // Spinner chọn tiền tệ nguồn và đích
    private lateinit var spinnerFromCurrency: Spinner
    private lateinit var spinnerToCurrency: Spinner

    // Trạng thái bộ nhớ của máy tính
    // currentOperandStr: chuỗi toán hạng đang nhập (dạng số nguyên)
    // storedOperand: toán hạng lưu bên trái khi chọn phép toán
    // pendingOperator: phép toán đang chờ thực hiện (+, -, x, /)
    // isStartingNewOperand: cờ đánh dấu chuẩn bị nhập toán hạng mới
    // lastActionWasEqual: cờ cho biết thao tác cuối là '=' để biết khi nào bắt đầu phép tính mới
    private var currentOperandStr: String = "0"
    private var storedOperand: Long? = null
    private var pendingOperator: Char? = null
    private var isStartingNewOperand: Boolean = true
    private var lastActionWasEqual: Boolean = false

    // Dữ liệu tỷ giá tiền tệ (tỷ giá so với VND - cố định, lấy từ cột "Bán" của ngân hàng)
    // Key: mã tiền tệ, Value: số VND đổi được cho 1 đơn vị tiền tệ đó
    private val exchangeRates = mapOf(
        "VND" to 1.0,           // Đồng Việt Nam (chuẩn)
        "USD" to 26361.00,      // Đô la Mỹ
        "EUR" to 31193.60,      // Euro
        "GBP" to 35132.64,      // Bảng Anh
        "JPY" to 175.17,        // Yên Nhật
        "AUD" to 17432.80,      // Đô la Úc
        "SGD" to 20540.68,      // Đô la Singapore
        "THB" to 829.89,        // Baht Thái
        "CAD" to 19058.62,      // Đô la Canada
        "CHF" to 33160.35,      // Franc Thụy Sĩ
        "HKD" to 3446.61,       // Đô la Hồng Kông
        "CNY" to 3752.45,       // Nhân dân tệ
        "DKK" to 4150.62,       // Krone Đan Mạch
        "INR" to 307.47,        // Rupee Ấn Độ
        "KRW" to 18.86,         // Won Hàn Quốc
        "KWD" to 89242.10       // Dinar Kuwait
    )

    // Danh sách mã tiền tệ để hiển thị trong Spinner
    private val currencyCodes = exchangeRates.keys.sorted().toList()

    // Cập nhật nội dung lên màn hình hiển thị
    private fun updateDisplay() {
        displayView.text = currentOperandStr
    }

    // Cập nhật dòng biểu thức 
    private fun updateExpression(afterEqual: Boolean = false) {
        val exp = when {
            pendingOperator != null && storedOperand != null ->
                "${storedOperand} ${pendingOperator} ${currentOperandStr}"
            else -> currentOperandStr
        }
        expressionView.text = if (afterEqual) "$exp =" else exp
    }

    // Xử lý khi nhấn phím số (0-9)
    private fun onDigit(d: Char) {
        if (lastActionWasEqual) {
            // Sau khi nhấn '=', nếu nhập số thì bắt đầu phép tính mới
            storedOperand = null
            pendingOperator = null
            lastActionWasEqual = false
        }

        if (isStartingNewOperand) {
            currentOperandStr = if (d == '0') "0" else d.toString()
            isStartingNewOperand = false
        } else {
            if (currentOperandStr == "0") {
                currentOperandStr = d.toString()
            } else {
                currentOperandStr += d
            }
        }
        updateDisplay()
        updateExpression()
        // Tự động chuyển đổi tiền tệ khi nhập số (chỉ khi không có phép toán đang chờ)
        if (pendingOperator == null) {
            performCurrencyConversion()
        }
    }

    // Chuyển chuỗi toán hạng hiện tại về Long an toàn
    private fun parseCurrent(): Long {
        return try {
            currentOperandStr.toLong()
        } catch (_: NumberFormatException) {
            0L
        }
    }

    // Nếu có phép toán đang chờ thì thực hiện với storedOperand (trái) và current (phải)
    private fun applyPendingIfAny(updateExpr: Boolean = true) {
        val op = pendingOperator ?: return
        val left = storedOperand ?: return
        val right = parseCurrent()

        val result: Long = when (op) {
            '+' -> left + right
            '-' -> left - right
            'x' -> left * right
            '/' -> {
                if (right == 0L) {
                    showErrorAndReset("Không thể chia cho 0")
                    return
                } else left / right
            }
            else -> right
        }

        storedOperand = result
        currentOperandStr = result.toString()
        updateDisplay()
        if (updateExpr) updateExpression()
    }

    // Xử lý khi chọn phép toán (+, -, x, /)
    private fun onOperator(op: Char) {
        if (lastActionWasEqual) {
            // Tiếp tục dùng kết quả trước làm toán hạng trái
            lastActionWasEqual = false
        }

        if (storedOperand == null) {
            storedOperand = parseCurrent()
        } else if (!isStartingNewOperand) {
            applyPendingIfAny()
        }

        pendingOperator = op
        isStartingNewOperand = true
        updateExpression()
    }

    // Xử lý khi nhấn '='
    private fun onEqual() {
        val op = pendingOperator ?: return // Không có phép toán để tính
        val left = storedOperand ?: parseCurrent()
        val rightStr = currentOperandStr // lưu chuỗi bên phải để hiển thị biểu thức

        applyPendingIfAny(updateExpr = false) // tính nhưng không cập nhật dòng biểu thức ở đây
        pendingOperator = null
        lastActionWasEqual = true
        isStartingNewOperand = true

        // Hiển thị biểu thức dạng: "12 + 3 ="
        expressionView.text = "$left $op $rightStr ="
    }

    // CE: Xóa toán hạng hiện tại về 0, giữ phép toán đang chờ
    private fun onClearEntry() {
        currentOperandStr = "0"
        isStartingNewOperand = true
        updateDisplay()
        updateExpression()
        // Xóa kết quả chuyển đổi tiền tệ
        currencyResultView.text = ""
    }

    // C: Xóa toàn bộ trạng thái, nhập lại từ đầu
    private fun onClearAll() {
        currentOperandStr = "0"
        storedOperand = null
        pendingOperator = null
        isStartingNewOperand = true
        lastActionWasEqual = false
        updateDisplay()
        // Xóa kết quả chuyển đổi tiền tệ
        currencyResultView.text = ""
    }

    // BS: Xóa một chữ số ở hàng đơn vị của toán hạng hiện tại
    private fun onBackspace() {
        if (isStartingNewOperand || currentOperandStr.isEmpty()) return
        val negativeOnly = currentOperandStr.length == 2 && currentOperandStr.startsWith("-")
        currentOperandStr = when {
            negativeOnly -> "0"
            currentOperandStr.length <= 1 -> "0"
            else -> currentOperandStr.dropLast(1)
        }
        updateDisplay()
        updateExpression()
        // Tự động chuyển đổi tiền tệ khi xóa số (chỉ khi không có phép toán đang chờ)
        if (pendingOperator == null) {
            performCurrencyConversion()
        }
    }

    // +/-: Đổi dấu của toán hạng hiện tại (0 không đổi)
    private fun onToggleSign() {
        if (currentOperandStr == "0") return
        currentOperandStr = if (currentOperandStr.startsWith("-")) {
            currentOperandStr.removePrefix("-")
        } else {
            "-" + currentOperandStr
        }
        updateDisplay()
        updateExpression()
        // Tự động chuyển đổi tiền tệ khi đổi dấu (chỉ khi không có phép toán đang chờ)
        if (pendingOperator == null) {
            performCurrencyConversion()
        }
    }

    // Dấu chấm (.) bị vô hiệu vì bài tập ở chế độ số nguyên
    private fun onDotPressed() {
        Toast.makeText(this, "Chỉ hỗ trợ số nguyên", Toast.LENGTH_SHORT).show()
    }

    // Hiển thị thông báo lỗi và đưa máy tính về trạng thái ban đầu
    private fun showErrorAndReset(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        onClearAll()
    }

    // Chuyển đổi tiền tệ từ loại này sang loại khác
    // Tỷ giá được lưu dưới dạng số VND đổi được cho 1 đơn vị tiền tệ
    private fun convertCurrency(amount: Double, fromCurrency: String, toCurrency: String): Double {
        // Lấy tỷ giá của tiền tệ nguồn và đích (số VND cho 1 đơn vị)
        val rateFrom = exchangeRates[fromCurrency] ?: 1.0
        val rateTo = exchangeRates[toCurrency] ?: 1.0
        
        // Chuyển đổi số tiền từ tiền tệ nguồn sang VND
        val amountInVND = amount * rateFrom
        
        // Chuyển đổi từ VND sang tiền tệ đích
        return amountInVND / rateTo
    }

    // Thực hiện chuyển đổi tiền tệ và hiển thị kết quả
    private fun performCurrencyConversion() {
        try {
            // Kiểm tra nếu Spinner chưa được khởi tạo
            if (!::spinnerFromCurrency.isInitialized || !::spinnerToCurrency.isInitialized) {
                return
            }

            // Lấy số tiền từ màn hình Calculator
            val amount = currentOperandStr.toDoubleOrNull() ?: 0.0
            if (currentOperandStr == "0" || currentOperandStr.isEmpty()) {
                currencyResultView.text = ""
                return
            }

            // Lấy mã tiền tệ từ Spinner
            val fromCurrency = spinnerFromCurrency.selectedItem as String
            val toCurrency = spinnerToCurrency.selectedItem as String

            // Chuyển đổi
            val convertedAmount = convertCurrency(amount, fromCurrency, toCurrency)

            // Hiển thị kết quả với định dạng số (làm tròn đến 2 chữ số thập phân)
            val formattedResult = String.format("%.2f", convertedAmount)
            currencyResultView.text = "$amount $fromCurrency = $formattedResult $toCurrency"
        } catch (e: Exception) {
            currencyResultView.text = "Lỗi chuyển đổi"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Gán layout chính của màn hình
        setContentView(R.layout.caculator_constrain_layout)
        // Tham chiếu TextView hiển thị kết quả và biểu thức
        displayView = findViewById(R.id.textView2)
        expressionView = findViewById(R.id.textExpression)
        currencyResultView = findViewById(R.id.textCurrencyResult)

        // Thiết lập Spinner cho chuyển đổi tiền tệ
        spinnerFromCurrency = findViewById(R.id.spinnerFromCurrency)
        spinnerToCurrency = findViewById(R.id.spinnerToCurrency)

        // Tạo ArrayAdapter cho Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencyCodes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Gán adapter cho cả hai Spinner
        spinnerFromCurrency.adapter = adapter
        spinnerToCurrency.adapter = adapter

        // Thiết lập giá trị mặc định (VND và USD)
        val defaultFromIndex = currencyCodes.indexOf("VND").coerceAtLeast(0)
        val defaultToIndex = currencyCodes.indexOf("USD").coerceAtLeast(0)
        spinnerFromCurrency.setSelection(defaultFromIndex)
        spinnerToCurrency.setSelection(defaultToIndex)

        // Lắng nghe sự kiện thay đổi Spinner để tự động chuyển đổi
        spinnerFromCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                // Tự động chuyển đổi khi thay đổi tiền tệ nguồn (chỉ khi không có phép toán đang chờ)
                if (pendingOperator == null) {
                    performCurrencyConversion()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerToCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                // Tự động chuyển đổi khi thay đổi tiền tệ đích (chỉ khi không có phép toán đang chờ)
                if (pendingOperator == null) {
                    performCurrencyConversion()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Gán sự kiện cho nút Convert
        findViewById<Button>(R.id.btnConvert).setOnClickListener {
            performCurrencyConversion()
        }

        // Gán sự kiện cho các phím số 0-9
        val digitIds = listOf(
            R.id.btn0 to '0',
            R.id.btn1 to '1',
            R.id.btn2 to '2',
            R.id.btn3 to '3',
            R.id.btn4 to '4',
            R.id.btn5 to '5',
            R.id.btn6 to '6',
            R.id.btn7 to '7',
            R.id.btn8 to '8',
            R.id.btn9 to '9',
        )
        digitIds.forEach { (id, ch) ->
            findViewById<Button>(id).setOnClickListener { onDigit(ch) }
        }

        // Gán sự kiện cho các phép toán
        findViewById<Button>(R.id.btnPlus).setOnClickListener { onOperator('+') }
        findViewById<Button>(R.id.btnMinus).setOnClickListener { onOperator('-') }
        findViewById<Button>(R.id.btnMultiplication).setOnClickListener { onOperator('x') }
        findViewById<Button>(R.id.btnDivide).setOnClickListener { onOperator('/') }

        // Gán sự kiện nút '='
        findViewById<Button>(R.id.btnEqual).setOnClickListener { onEqual() }

        // Gán sự kiện cho các nút CE, C, BS
        findViewById<Button>(R.id.btnCE).setOnClickListener { onClearEntry() }
        findViewById<Button>(R.id.btnC).setOnClickListener { onClearAll() }
        findViewById<Button>(R.id.btnBS).setOnClickListener { onBackspace() }

        // Gán sự kiện cho nút đổi dấu và dấu chấm
        findViewById<Button>(R.id.btnM).setOnClickListener { onToggleSign() }
        findViewById<Button>(R.id.btnDot).setOnClickListener { onDotPressed() }

        // Khởi tạo hiển thị ban đầu
        updateDisplay()
        updateExpression()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
