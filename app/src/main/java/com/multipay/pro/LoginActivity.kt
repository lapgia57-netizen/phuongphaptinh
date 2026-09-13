package com.multipay.pro

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.multipay.pro.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnLogin.setOnClickListener { doLogin() }
        binding.btnRegister.setOnClickListener { doRegister() }
    }

    override fun onStart() {
        super.onStart()
        // Nếu đã đăng nhập sẵn, chuyển thẳng vào MainActivity
        if (auth.currentUser != null) {
            goToMain()
        }
    }

    private fun showError(message: String) {
        binding.textError.visibility = View.VISIBLE
        binding.textError.text = message
    }

    private fun hideError() {
        binding.textError.visibility = View.GONE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.btnRegister.isEnabled = !loading
    }

    private fun doLogin() {
        hideError()
        val email = binding.editEmail.text?.toString()?.trim().orEmpty()
        val pass = binding.editPassword.text?.toString().orEmpty()

        if (email.isEmpty() || pass.isEmpty()) {
            showError("Vui lòng nhập đầy đủ Email và Mật khẩu.")
            return
        }

        setLoading(true)
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    goToMain()
                } else {
                    showError("Lỗi đăng nhập: ${task.exception?.localizedMessage ?: "Không xác định"}")
                }
            }
    }

    private fun doRegister() {
        hideError()
        val email = binding.editEmail.text?.toString()?.trim().orEmpty()
        val pass = binding.editPassword.text?.toString().orEmpty()

        if (email.isEmpty() || pass.length < 6) {
            showError("Mật khẩu cần ít nhất 6 ký tự.")
            return
        }

        setLoading(true)
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    goToMain()
                } else {
                    showError("Lỗi đăng ký: ${task.exception?.localizedMessage ?: "Không xác định"}")
                }
            }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
