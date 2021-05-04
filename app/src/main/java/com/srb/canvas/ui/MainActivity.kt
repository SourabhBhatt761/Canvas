package com.srb.canvas.ui

import android.graphics.Path
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.firebase.database.FirebaseDatabase
import com.srb.canvas.BuildConfig
import com.srb.canvas.R
import com.srb.canvas.databinding.ActivityMainBinding
import com.srb.canvas.utils.setupTheme
import com.srb.canvas.utils.timber.DebugTree
import com.srb.canvas.utils.timber.ReleaseTree
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        // Set theme after splash screen.
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        // Use DataBinding to set the activity view.
        _binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this

        

        setupNavigation()
        setupTheme(this)
        setDebugTools()

    }

    /* ===================================== Navigation ===================================== */

    override fun onSupportNavigateUp(): Boolean {
        return (Navigation.findNavController(this, R.id.navHostFragment).navigateUp()
                || super.onSupportNavigateUp())
    }

    private fun setupNavigation() {
        val navController: NavController = findNavController(R.id.navHostFragment)
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)

        navController.addOnDestinationChangedListener { _, destination: NavDestination, _ ->
            val toolBar = supportActionBar ?: return@addOnDestinationChangedListener

            binding.toolbar.setBackgroundColor(
                ActivityCompat.getColor(this, R.color.backgroundColor)
            )
            this.window.navigationBarColor = ActivityCompat.getColor(this, R.color.primaryColor)
            this.window.statusBarColor = ActivityCompat.getColor(this, R.color.primaryColor)

            when (destination.id) {
                R.id.canvasFragment -> showToolbarTitleOrUp(toolBar, false)
//                R.id.aboutFragment -> showToolbarTitleOrUp(toolBar, true)
            }
        }
    }

    private fun showToolbarTitleOrUp(
        toolBar: ActionBar, showUpButton: Boolean, showTitle: Boolean = true
    ) {
        toolBar.setDisplayShowTitleEnabled(showTitle)
        toolBar.setDisplayHomeAsUpEnabled(showUpButton)
    }



    private fun setDebugTools() {
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }

}