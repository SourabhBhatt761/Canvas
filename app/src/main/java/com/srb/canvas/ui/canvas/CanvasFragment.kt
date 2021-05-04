package com.srb.canvas.ui.canvas

import android.Manifest
import android.R.color
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
import com.srb.canvas.utils.Constants.GALLERY
import com.srb.canvas.utils.Constants.STORAGE_PERMISSION_CODE
import com.srb.canvas.utils.snackBarMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class CanvasFragment : Fragment() {

    private var _binding: FragmentCanvasBinding? = null
    private val binding get() = _binding!!

    private var brushColor: Int = Color.BLACK
    private var fabClicked = false

    //animations
    private val fromLeft : Animation by lazy{ AnimationUtils.loadAnimation(
        requireContext(),
        R.anim.animation_from_left
    )}
    private val fromRight : Animation by lazy{ AnimationUtils.loadAnimation(
        requireContext(),
        R.anim.animation_from_right
    )}
    private val toLeft : Animation by lazy{ AnimationUtils.loadAnimation(
        requireContext(),
        R.anim.animation_to_left
    )}
    private val toRight : Animation by lazy{ AnimationUtils.loadAnimation(
        requireContext(),
        R.anim.animation_to_right
    )}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCanvasBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        binding.canvas = this
        binding.drawing = binding.drawingView

        // Initiate for long click.
//        eraser()
//        brush()



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

        }

//        return NavigationUI.onNavDestinationSelected(
//            item, requireView().findNavController()
//        ) || super.onOptionsItemSelected(item)

        return super.onOptionsItemSelected(item)
    }

    private fun logOut(): Boolean {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)

        requireActivity().finish()
        return true
    }

    private fun saveDrawing() {
        when {
            isReadStorageAllowed() -> {
                saveBitmap()
                showConfirmationDialog()
            }
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

        binding.ibEraseDraw.setColorFilter(ContextCompat.getColor(requireContext(),R.color.iconColor))
        binding.ibBrushSize.apply {
            setOnLongClickListener {
            showBrushSizeDialog(false)
            binding.drawingView.setBrushColor(brushColor)
            return@setOnLongClickListener true
        }
           // setBackgroundColor(ContextCompat.getColor(requireContext(),R.color.white))

           setColorFilter(ContextCompat.getColor(requireContext(),R.color.selectedIconColor))
        }
    }

    fun eraser() {
        binding.drawingView.setBrushColor(Color.WHITE)

        binding.ibBrushSize.setColorFilter(ContextCompat.getColor(requireContext(),R.color.iconColor))
        binding.ibEraseDraw.apply {
            setOnLongClickListener {
                showBrushSizeDialog(true)
                binding.drawingView.setBrushColor(Color.WHITE)
                return@setOnLongClickListener true
            }
            setColorFilter(ContextCompat.getColor(requireContext(),R.color.selectedIconColor))
        }
    }

    fun brushColor() {

        val colors = intArrayOf(
            Color.BLACK,
            Color.RED,
            Color.BLUE,
            Color.GREEN,
            Color.YELLOW,
            Color.MAGENTA,
            Color.GRAY,
            Color.CYAN,
            ContextCompat.getColor(requireContext(), R.color.beige),
            ContextCompat.getColor(requireContext(), R.color.orange),
            ContextCompat.getColor(requireContext(), R.color.greenLight),
            ContextCompat.getColor(requireContext(), R.color.purpleBlue)
        )

        binding.ibBrushColor.isClickable = false


            MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                title(R.string.dialog_choose_color)
                colorChooser(
                    colors,
                    allowCustomArgb = true,
                    showAlphaSelector = true,
                    initialSelection = brushColor
                ) { _, color ->
                    brushColor = color
                    binding.drawingView.setBrushColor(brushColor)
                    binding.ibBrushColor.setColorFilter(color)
                }
                positiveButton(R.string.dialog_select)
                negativeButton(R.string.dialog_negative)

        }

        Handler(Looper.getMainLooper()).postDelayed({
            binding.ibBrushColor.isClickable = true
        }, 1000)



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
//            snackBarMsg(requireView(), getString(R.string.drawing_saved))
            Toast.makeText(requireContext(),"Saved to Pictures",Toast.LENGTH_SHORT).show()
        }
    }

    fun fab() {
        setVisibility(fabClicked)
        setAnimation(fabClicked)
        fabClicked = !fabClicked
    }

    private fun setVisibility(fabClicked: Boolean) {
        if (!fabClicked) {
            binding.apply {
                cvToolsPanel.visibility = View.VISIBLE
                floatingActionButton.setImageResource(R.drawable.ic_arrow_right)
            }
        } else {
            binding.apply {
                cvToolsPanel.visibility = View.INVISIBLE
                floatingActionButton.setImageResource(R.drawable.ic_arrow_left)
            }
        }


    }

    private fun setAnimation(fabClicked: Boolean) {
    if (!fabClicked){
        binding.apply {
            cvToolsPanel.startAnimation(fromRight)

        }
    }else{
        binding.apply {
            cvToolsPanel.startAnimation(toRight)

        }
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

    private fun showConfirmationDialog(){
        MaterialDialog(requireContext()).show{
            title(text = "Upload to Firebase as well?")
            message(text = "With the help of 'okay' image will be uploaded to firebase as well as local storage and cancel will store only in pictures")
            positiveButton(text = "Okay"){

                val uri = binding.drawingView.imageUri
                binding.drawingView.uploadToFireStore(uri)
                hide()
            }
            negativeButton(text = "Cancel"){
                hide()
            }
        }
    }


}