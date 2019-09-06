package com.klitsie.scopedobserver

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
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
