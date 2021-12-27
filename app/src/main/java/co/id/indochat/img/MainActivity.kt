package co.id.indochat.img

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button_camera).setOnClickListener {
            CameraActivity.start(this)
        }
        findViewById<Button>(R.id.button_editor).setOnClickListener {
            EditorActivity.start(this)
        }

        findViewById<Button>(R.id.button_video).setOnClickListener {
            VideoActivity.start(this)
        }
    }
}