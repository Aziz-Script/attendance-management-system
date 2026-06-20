package com.example.aws

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        val emailField    = findViewById<TextInputEditText>(R.id.id_input)
        val passwordField = findViewById<TextInputEditText>(R.id.password_input)
        val btnLogin      = findViewById<MaterialButton>(R.id.login_button)

        btnLogin.setOnClickListener {
            val email = emailField.text.toString().trim().replace("\u0000", "")
            val password = passwordField.text.toString().trim().replace("\u0000", "")

            if (email.isEmpty()) {
                emailField.error = "Enter your email"
                emailField.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                passwordField.error = "Enter your password"
                passwordField.requestFocus()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "Signing in…"

            RetrofitClient.instance.login(
                email    = email,   // what the user typed
                id       = email,   // same value — APEX checks both columns
                password = password
            )
                .enqueue(object : Callback<ResponseBody> {

                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        btnLogin.isEnabled = true
                        btnLogin.text = getString(R.string.log_in)

                        val raw = response.body()?.string()
                            ?: response.errorBody()?.string()
                            ?: run {
                                Toast.makeText(this@LoginActivity, getString(R.string.toast_server_error), Toast.LENGTH_SHORT).show()
                                return
                            }

                        Log.d("LOGIN", "raw = $raw")

                        try {
                            val outer      = JSONObject(raw)
                            val bodyString = outer.optString("body", null) ?: raw
                            val user       = JSONObject(bodyString)

                            if (user.has("error")) {
                                Toast.makeText(
                                    this@LoginActivity,
                                    user.getString("error"),
                                    Toast.LENGTH_SHORT
                                ).show()
                                return
                            }

                            val id        = user.getString("id")
                            val firstName = user.getString("first_name")
                            val lastName  = user.getString("last_name")
                            val role      = user.getString("role")

                            // Load this user's saved settings into app_settings
                            val savedUserPrefs = getSharedPreferences("user_prefs_$id", MODE_PRIVATE)
                            getSharedPreferences("app_settings", MODE_PRIVATE).edit().apply {
                                putString("app_language", savedUserPrefs.getString("app_language", "en"))
                                putString("app_theme",    savedUserPrefs.getString("app_theme", "green"))
                                putBoolean("notifications_enabled", savedUserPrefs.getBoolean("notifications_enabled", true))
                                apply()
                            }

                            Toast.makeText(
                                this@LoginActivity,
                                getString(R.string.toast_welcome, firstName),
                                Toast.LENGTH_SHORT
                            ).show()

                            when (role.lowercase()) {
                                "student" -> {
                                    getSharedPreferences("user_session", MODE_PRIVATE).edit {
                                        putString("student_id", id)
                                        putString("user_id", id)
                                    }
                                    val intent = Intent(this@LoginActivity, StudentHomeActivity::class.java)
                                    intent.putExtra("student_id", id)
                                    intent.putExtra("first_name", firstName)
                                    intent.putExtra("last_name", lastName)
                                    intent.putExtra("gpa", user.optString("gpa", "N/A"))
                                    intent.putExtra("major", user.optString("major", ""))
                                    startActivity(intent)
                                }
                                "instructor" -> {
                                    getSharedPreferences("user_session", MODE_PRIVATE).edit {
                                        putString("user_id", id)
                                    }
                                    val intent = Intent(this@LoginActivity, InstructorHomeActivity::class.java)
                                    intent.putExtra("instructor_id", id)
                                    intent.putExtra("first_name", firstName)
                                    intent.putExtra("last_name", lastName)
                                    intent.putExtra("department", user.optString("department", ""))
                                    startActivity(intent)
                                }
                                else -> {
                                    Toast.makeText(this@LoginActivity, getString(R.string.toast_unknown_role, role), Toast.LENGTH_SHORT).show()
                                }
                            }
                            finish()

                        } catch (e: Exception) {
                            Log.e("LOGIN", "Parse error", e)
                            Toast.makeText(this@LoginActivity, getString(R.string.toast_unexpected_response), Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        btnLogin.isEnabled = true
                        btnLogin.text = getString(R.string.log_in)
                        Toast.makeText(this@LoginActivity, getString(R.string.toast_network_error), Toast.LENGTH_SHORT).show()
                        Log.e("LOGIN", "Network failure", t)
                    }
                })
        }
    }
}