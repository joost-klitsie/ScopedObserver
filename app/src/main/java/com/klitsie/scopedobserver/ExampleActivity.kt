package com.klitsie.scopedobserver

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.isVisible
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class ExampleActivity : AppCompatActivity() {

	private val model by lazy {
		ViewModelProvider(this).get(ExampleViewModel::class.java)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		setSupportActionBar(toolbar)
		model.data.observe(this) {
			loading.isVisible = it.isLoading
			text.text = it.text
		}
	}

	override fun onResume() {
		super.onResume()
		model.onResume()
	}

	override fun onPause() {
		model.onPause()
		super.onPause()
	}
}
