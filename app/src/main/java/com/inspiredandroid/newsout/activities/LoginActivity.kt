package com.inspiredandroid.newsout.activities

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.inspiredandroid.newsout.*
import com.inspiredandroid.newsout.dialogs.InfoDialog
import io.ktor.util.InternalAPI
import kotlinx.android.synthetic.main.activity_login.*

/*
 * Copyright 2019 Simon Schubert Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@InternalAPI
class LoginActivity : AppCompatActivity() {

    private var mode = MODE_NEWSOUT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        updateMode()

        passwordEt.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                if (mode == MODE_NEWSOUT) {
                    attemptSignup()
                } else {
                    attemptLogin()
                }
                return@OnEditorActionListener true
            }
            false
        })

        signInBtn.setOnClickListener {
            if (mode == MODE_NEWSOUT) {
                attemptSignup()
            } else {
                attemptLogin()
            }
        }
        infoBtn.setOnClickListener {
            val dialog = InfoDialog.getInstance()
            dialog.show(supportFragmentManager, "TAG")
        }
        tvResetPassword.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://nx3217.your-next.cloud"))
            startActivity(browserIntent)
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            mode = if (checkedId == R.id.nextcloudBtn) {
                MODE_CUSTOM
            } else {
                MODE_NEWSOUT
            }
            updateMode()
        }

        val accountManager = AccountManager.get(this)
        accountManager.getAccountsByType("com.inspiredandroid.newsout").forEach {
            val password = accountManager.getPassword(it)
            val url = accountManager.getUserData(it, "EXTRA_BASE_URL")
            Api.setCredentials(url, it.name, password)

            startActivity(Intent(this@LoginActivity, FeedsActivity::class.java))
            finish()
        }
    }

    private fun updateMode() {
        if (mode == MODE_NEWSOUT) {
            serverPathEt.visibility = View.GONE
            signInBtn.text = "Login/Create"
            emailEt.hint = "Email"
        } else {
            serverPathEt.visibility = View.VISIBLE
            signInBtn.text = "Login"
            emailEt.hint = "Email/Username"
        }
    }

    private fun attemptLogin() {
        hideKeyboard()
        emailEt.error = null
        passwordEt.error = null
        serverPathEt.error = null

        val email = emailEt.text.toString()
        val password = passwordEt.text.toString()

        val url = serverPathEt.text.toString()

        showLoading()

        Api.login(url, email, password, {
            if (isThere()) {
                saveLogin(url, email, password)

                startActivity(Intent(this@LoginActivity, FeedsActivity::class.java))
                finish()
            }
        }, {
            if (isThere()) {
                passwordEt.error = "Password or email might be wrong"
                hideLoading()
            }
        }, {
            if (isThere()) {
                serverPathEt.error = "Could not connect"
                hideLoading()
            }
        })
    }

    private fun attemptSignup() {
        hideKeyboard()
        emailEt.error = null
        passwordEt.error = null
        serverPathEt.error = null

        val email = emailEt.text.toString()
        val password = passwordEt.text.toString()
        val url = "https://nx3217.your-next.cloud"

        if (!email.isEmailValid()) {
            emailEt.error = "Please enter a correct email address"
            return
        }
        if (password.length < 6) {
            passwordEt.error = "Password is too short"
            return
        }

        showLoading()

        Api.createAccount(url, email, password, {
            if (isThere()) {
                saveLogin(url, email, password)

                startActivity(Intent(this@LoginActivity, FeedsActivity::class.java))
                finish()
            }
        }, {
            if (isThere()) {
                serverPathEt?.setText(url)
                attemptLogin()
            }
        }, {
            if (isThere()) {
                passwordEt.error = "Try again with a different password"
                hideLoading()
            }
        })
    }

    private fun saveLogin(url: String, email: String, password: String) {
        val accountManager = AccountManager.get(this)
        val account = Account(email, "com.inspiredandroid.newsout")
        accountManager.addAccountExplicitly(account, password, Bundle())
        accountManager.setUserData(account, "EXTRA_BASE_URL", url)
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        ivLogo.visibility = View.VISIBLE
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        ivLogo.visibility = View.INVISIBLE
    }

    companion object {
        const val MODE_CUSTOM = 1
        const val MODE_NEWSOUT = 2
    }
}
