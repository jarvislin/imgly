package co.id.indochat.img

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import ly.img.android.pesdk.VideoEditorSettingsList
import ly.img.android.pesdk.assets.filter.basic.FilterPackBasic
import ly.img.android.pesdk.assets.font.basic.FontPackBasic
import ly.img.android.pesdk.assets.frame.basic.FramePackBasic
import ly.img.android.pesdk.assets.overlay.basic.OverlayPackBasic
import ly.img.android.pesdk.backend.decoder.ImageSource
import ly.img.android.pesdk.backend.model.EditorSDKResult
import ly.img.android.pesdk.backend.model.state.LoadSettings
import ly.img.android.pesdk.backend.model.state.VideoCompositionSettings
import ly.img.android.pesdk.backend.model.state.VideoEditorSaveSettings
import ly.img.android.pesdk.ui.activity.VideoEditorBuilder
import ly.img.android.pesdk.ui.model.state.*
import ly.img.android.pesdk.ui.panels.item.ToolItem
import ly.img.android.pesdk.ui.utils.PermissionRequest
import ly.img.android.serializer._3.IMGLYFileWriter
import java.io.File
import java.io.IOException

class VideoActivity : Activity(), PermissionRequest.Response {

    companion object {
        const val VESDK_RESULT = 1
        const val GALLERY_RESULT = 2

        fun start(context: Context){
            Intent(context, VideoActivity::class.java)
                .let { context.startActivity(it) }
        }
    }

    // Important permission request for Android 6.0 and above, don't forget to add this!
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        PermissionRequest.onRequestPermissionsResult(requestCode, permissions, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun permissionGranted() {}

    override fun permissionDenied() {
        /* TODO: The Permission was rejected by the user. The Editor was not opened,
         * Show a hint to the user and try again. */
    }

    // Create a empty new SettingsList and apply the changes on this referance.
    // If you include our asset Packs and use our UI you also need to add them to the UI,
    // otherwise they are only available for the backend (like Serialisation)
    // See the specific feature sections of our guides if you want to know how to add our own Assets.
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private fun createVesdkSettingsList(): VideoEditorSettingsList {
        val list =  VideoEditorSettingsList()
            .configure<UiConfigFilter> {
                it.setFilterList(FilterPackBasic.getFilterPack())
            }
            .configure<UiConfigText> {
                it.setFontList(FontPackBasic.getFontPack())
            }
            .configure<UiConfigFrame> {
                it.setFrameList(FramePackBasic.getFramePack())
            }
            .configure<UiConfigOverlay> {
                it.setOverlayList(OverlayPackBasic.getOverlayPack())
            }
            .configure<VideoEditorSaveSettings> {
                it.setOutputToGallery(Environment.DIRECTORY_DCIM)
            }

        list.configure<UiConfigMainMenu> {
            // Set the tools you want keep sure you license is cover the feature and do not forget to include the correct modules in your build.gradle
            it.setToolList(
                // Composition tool (needs 'ui:video-composition')
                ToolItem(
                    "imgly_tool_composition",
                    R.string.vesdk_video_composition_title_name,
                    ImageSource.create(R.drawable.imgly_icon_tool_video_composition)
                ),
                // Trim tool (Is redundant together with the composition tool, needs 'ui:video-trim')
                ToolItem("imgly_tool_trim", R.string.vesdk_video_trim_title_name, ImageSource.create(R.drawable.imgly_icon_tool_trim)),
                // Audio Overlay tool (needs 'ui:audio-composition')
                ToolItem(
                    "imgly_tool_audio_overlay_options",
                    R.string.vesdk_audio_composition_title_name,
                    ImageSource.create(R.drawable.imgly_icon_tool_audio)
                ),
                // Transformation tool (needs 'ui:transform')
                ToolItem(
                    "imgly_tool_transform",
                    R.string.pesdk_transform_title_name,
                    ImageSource.create(R.drawable.imgly_icon_tool_transform)
                ),
                // Filter tool (needs 'ui:filter')
                ToolItem("imgly_tool_filter", R.string.pesdk_filter_title_name, ImageSource.create(R.drawable.imgly_icon_tool_filters)),
                // Adjustment tool (needs 'ui:adjustment')
                ToolItem(
                    "imgly_tool_adjustment",
                    R.string.pesdk_adjustments_title_name,
                    ImageSource.create(R.drawable.imgly_icon_tool_adjust)
                ),
                // Sticker tool (needs 'ui:sticker')
                ToolItem(
                    "imgly_tool_sticker_selection",
                    R.string.pesdk_sticker_title_name,
                    ImageSource.create(R.drawable.imgly_icon_tool_sticker)
                ),
                // Text Design tool (needs 'ui:text-design')
                ToolItem(
                    "imgly_tool_text_design",
                    R.string.pesdk_textDesign_title_name,
                    ImageSource.create(R.drawable.imgly_icon_tool_text_design)
                ),
                // Text tool (needs 'ui:text')
                ToolItem("imgly_tool_text", R.string.pesdk_text_title_name, ImageSource.create(R.drawable.imgly_icon_tool_text)),
                // Overlay tool (needs 'ui:overlay')
                ToolItem("imgly_tool_overlay", R.string.pesdk_overlay_title_name, ImageSource.create(R.drawable.imgly_icon_tool_overlay)),
                // Frame tool (needs 'ui:frame')
                ToolItem("imgly_tool_frame", R.string.pesdk_frame_title_name, ImageSource.create(R.drawable.imgly_icon_tool_frame)),
                // Brush tool (needs 'ui:brush')
                ToolItem("imgly_tool_brush", R.string.pesdk_brush_title_name, ImageSource.create(R.drawable.imgly_icon_tool_brush)),
                // Focus tool (needs 'ui:focus')
                ToolItem("imgly_tool_focus", R.string.pesdk_focus_title_name, ImageSource.create(R.drawable.imgly_icon_tool_focus))
            )
        }
        return list
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        openSystemGalleryToSelectAnVideo()
    }

    fun openSystemGalleryToSelectAnVideo() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,"video/*")

        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, GALLERY_RESULT)
        } else {
            Toast.makeText(
                this,
                "No Gallery APP installed",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun openEditor(inputVideo: Uri?) {
        val settingsList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            createVesdkSettingsList()
        } else {
            Toast.makeText(this, "Video support needs Android 4.3", Toast.LENGTH_LONG).show()
            return
        }

        settingsList.configure<LoadSettings> {
            it.source = inputVideo
        }

        settingsList[LoadSettings::class].source = inputVideo

        VideoEditorBuilder(this)
            .setSettingsList(settingsList)
            .startActivityForResult(this, VESDK_RESULT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if(intent ==null){
            finish()
        }

        if (resultCode == RESULT_OK && requestCode == GALLERY_RESULT) {
            // Open Editor with some uri in this case with an video selected from the system gallery.
            openEditor(intent!!.data)

        } else if (resultCode == RESULT_OK && requestCode == VESDK_RESULT) {
            // Editor has saved an Video.
            val data = EditorSDKResult(intent!!)

            Log.i("VESDK", "Source video is located here ${data.sourceUri}")
            Log.i("VESDK", "Result video is located here ${data.resultUri}")

            // TODO: Do something with the result video

            // OPTIONAL: read the latest state to save it as a serialisation
            val lastState = data.settingsList
            try {
                IMGLYFileWriter(lastState).writeJson(
                    File(
                        getExternalFilesDir(null),
                        "serialisationReadyToReadWithPESDKFileReader.json"
                    )
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }
            finish()
        } else if (resultCode == RESULT_CANCELED && requestCode == VESDK_RESULT) {
            // Editor was canceled
            val data = EditorSDKResult(intent!!)

            val sourceURI = data.sourceUri
            // TODO: Do something with the source...
        }
    }

}