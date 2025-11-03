package com.example.bt_caculatorconstrainlayout

import android.os.Bundle
import android.widget.Button
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
    }

    // C: Xóa toàn bộ trạng thái, nhập lại từ đầu
    private fun onClearAll() {
        currentOperandStr = "0"
        storedOperand = null
        pendingOperator = null
        isStartingNewOperand = true
        lastActionWasEqual = false
        updateDisplay()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Gán layout chính của màn hình
        setContentView(R.layout.caculator_constrain_layout)
        // Tham chiếu TextView hiển thị kết quả và biểu thức
        displayView = findViewById(R.id.textView2)
        expressionView = findViewById(R.id.textExpression)

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
