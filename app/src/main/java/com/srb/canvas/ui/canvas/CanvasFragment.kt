package com.srb.canvas.ui.canvas

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.HorizontalScrollView
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BasicGridItem
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.bottomsheets.gridItems
import com.afollestad.materialdialogs.color.colorChooser
import com.afollestad.materialdialogs.list.listItems
import com.google.firebase.auth.FirebaseAuth
import com.srb.canvas.R
import com.srb.canvas.databinding.FragmentCanvasBinding
import com.srb.canvas.ui.LoginActivity
import com.srb.canvas.ui.MainActivity
import com.srb.canvas.utils.Constants.GALLERY
import com.srb.canvas.utils.Constants.STORAGE_PERMISSION_CODE
import com.srb.canvas.utils.shareText
import com.srb.canvas.utils.snackBarMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CanvasFragment : Fragment() {

    private var _binding: FragmentCanvasBinding? = null
    private val binding get() = _binding!!

    private var brushColor: Int = Color.BLACK

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCanvasBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        binding.canvas = this
        binding.drawing = binding.drawingView

        // Initiate for long click.
        eraser()
        brush()



            setHasOptionsMenu(true)
            onBackPressed()



            return binding.root

    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        binding.hsvRight.setOnDragListener(View.OnDragListener { v, event ->
//            when (event.action) {
//                DragEvent.ACTION_DRAG_STARTED -> {
//                    checkScroll()
//                }
//
//                else -> checkScroll()
//            }
//
//        })
//
//
//    }
//
//    private fun checkScroll() : Boolean{
//        return if(binding.hsvRight.fullScroll(HorizontalScrollView.FOCUS_RIGHT)){
//            binding.ibMoveRight.visibility = View.INVISIBLE
//            binding.ibMoveLeft.visibility = View.VISIBLE
//            true
//        }else{
//            binding.ibMoveRight.visibility = View.VISIBLE
//            binding.ibMoveLeft.visibility = View.INVISIBLE
//            true
//        }
//    }

    /* ===================================== Permissions ===================================== */

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {
            resultCode == Activity.RESULT_OK && requestCode == GALLERY -> {
                try {
                    when {
                        data!!.data != null -> binding.ivBackground.setImageURI(data.data)
                        else -> snackBarMsg(requireView(), getString(R.string.error_parsing))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun isReadStorageAllowed(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            requireActivity(), arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), STORAGE_PERMISSION_CODE
        )
    }

    /* ===================================== Options Menu ===================================== */

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_canvas, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.option_save_drawing -> saveDrawing()
            R.id.log_Out -> logOut()
//            R.id.option_tell_friends -> shareText(
//                requireContext(), getString(R.string.option_tell_friends_msg)
//            )
//            R.id.option_rate_app -> rateApp()
//            R.id.option_about -> findNavController().navigate(R.id.action_canvasFragment_to_aboutFragment)
//            R.id.option_settings -> findNavController().navigate(R.id.settingsActivity)
        }

//        return NavigationUI.onNavDestinationSelected(
//            item, requireView().findNavController()
//        ) || super.onOptionsItemSelected(item)

        return super.onOptionsItemSelected(item)
    }

    private fun logOut(): Boolean {
       FirebaseAuth.getInstance().signOut()
        val intent = Intent(requireContext(),LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)

        requireActivity().finish()
        return true
    }

    private fun saveDrawing() {
        when {
            isReadStorageAllowed() -> saveBitmap()
            else -> requestStoragePermission()
        }
    }

    private fun rateApp() {
        val rateIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=com.anibalventura.likepaint")
        )
        startActivity(Intent.createChooser(rateIntent, null))
    }

    /* ===================================== Tools Panel ===================================== */

    fun brush() {
        binding.drawingView.setBrushColor(brushColor)

        binding.ibBrushSize.setOnLongClickListener {
            showBrushSizeDialog(false)
            binding.drawingView.setBrushColor(brushColor)
            return@setOnLongClickListener true
        }
    }

    fun eraser() {
        binding.drawingView.setBrushColor(Color.WHITE)

        binding.ibEraseDraw.setOnLongClickListener {
            showBrushSizeDialog(true)
            binding.drawingView.setBrushColor(Color.WHITE)
            return@setOnLongClickListener true
        }
    }

    fun brushColor() {

        val colors = intArrayOf(
            Color.BLACK, Color.RED, Color.BLUE, Color.GREEN,
            Color.YELLOW, Color.MAGENTA, Color.GRAY, Color.CYAN,
            ContextCompat.getColor(requireContext(),R.color.beige),
            ContextCompat.getColor(requireContext(),R.color.orange),
            ContextCompat.getColor(requireContext(),R.color.greenLight), ContextCompat.getColor(requireContext(),R.color.purpleBlue)
        )

        MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            title(R.string.dialog_choose_color)
            colorChooser(colors, allowCustomArgb = true, showAlphaSelector = true ,initialSelection = brushColor) { _, color ->
                brushColor = color
                binding.drawingView.setBrushColor(brushColor)
            }
            positiveButton(R.string.dialog_select)
            negativeButton(R.string.dialog_negative)
        }
    }

    fun imageBackground() {
        val options = listOf(
            getString(R.string.background_image_add), getString(R.string.background_image_clear)
        )

        MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            title(R.string.dialog_background_image)
            listItems(items = options) { _, index, _ ->
                when (index) {
                    0 -> when {
                        isReadStorageAllowed() -> requestImage()
                        else -> requestStoragePermission()
                    }
                    1 -> binding.ivBackground.setImageURI(null)
                }
            }
        }
    }

    fun shareDrawing() {
        binding.drawingView.saveBitmap(binding.drawingView.getBitmap(binding.flDrawingViewContainer))

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, Uri.parse(binding.drawingView.result))
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        startActivity(Intent.createChooser(intent, getString(R.string.share_drawing)))
    }

    fun moveScroll(direction: Int) {
        when (direction) {
            0 -> {
                binding.hsvRight.fullScroll(HorizontalScrollView.FOCUS_RIGHT)
                binding.ibMoveRight.visibility = View.INVISIBLE
                binding.ibMoveLeft.visibility = View.VISIBLE
            }
            1 -> {
                binding.hsvRight.fullScroll(HorizontalScrollView.FOCUS_LEFT)
                binding.ibMoveLeft.visibility = View.INVISIBLE
                binding.ibMoveRight.visibility = View.VISIBLE
            }
        }
    }

    /* ===================================== Tools Panel Utils ===================================== */

    private fun showBrushSizeDialog(eraser: Boolean) {
        val sizes = listOf(
            BasicGridItem(R.drawable.brush_small, getString(R.string.brush_small)),
            BasicGridItem(R.drawable.brush_medium, getString(R.string.brush_medium)),
            BasicGridItem(R.drawable.brush_large, getString(R.string.brush_large))
        )

        MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            when (eraser) {
                true -> title(R.string.dialog_choose_eraser_size)
                else -> title(R.string.dialog_choose_brush_size)
            }

            gridItems(sizes) { _, index, _ ->
                when (index) {
                    0 -> binding.drawingView.setBrushSize(5F)
                    1 -> binding.drawingView.setBrushSize(10F)
                    2 -> binding.drawingView.setBrushSize(20F)
                }
            }
        }
    }

    private fun requestImage() {
        try {
            val pickPhotoIntent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            startActivityForResult(pickPhotoIntent, GALLERY)
        } catch (e: Exception) {
            snackBarMsg(requireView(), getString(R.string.gallery_not_available))
        }
    }

    private fun saveBitmap() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.drawingView.saveBitmap(binding.drawingView.getBitmap(binding.flDrawingViewContainer))
            snackBarMsg(requireView(), getString(R.string.drawing_saved))
        }
    }

    /* ===================================== On app exit. ===================================== */

    private fun onBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                    MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                        title(R.string.dialog_exit)
                        message(R.string.dialog_exit_message)

                        positiveButton(R.string.option_save_drawing) {
                            saveDrawing()

                            GlobalScope.launch(Dispatchers.Main) {
                                delay(1500L)
                                if (isEnabled) {
                                    isEnabled = false
                                    requireActivity().onBackPressed()
                                }
                            }
                        }
                        negativeButton(R.string.dialog_exit_confirmation) {
                            if (isEnabled) {
                                isEnabled = false
                                requireActivity().onBackPressed()
                            }
                        }


                    }
                }
            }
        )
    }


}