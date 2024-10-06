package devilstudio.com.plantdiseasedetector

import android.os.Bundle
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {
    private lateinit var mClassifier: Classifier
    private lateinit var mBitmap: Bitmap

    lateinit var myDialog: Dialog

    private var pname: String? = ""
    private var pSymptoms: String? = ""
    private var pManage: String? = ""

    private var NameV: TextView? = null
    private var SymptomsV: TextView? = null
    private var ManageV: TextView? = null

    private val mCameraRequestCode = 0
    private val mGalleryRequestCode = 1
    private val mInputSize = 200 //224
    private val mModelPath = "model.tflite"
    private val mLabelPath = "labels.txt"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_main)
        mClassifier = Classifier(assets, mModelPath, mLabelPath, mInputSize)

        myDialog = Dialog(this)


        mCameraButton.setOnClickListener {
            val options = arrayOf<CharSequence>("Take Photo", "Choose from Gallery", "Cancel")
            val builder: android.support.v7.app.AlertDialog.Builder = android.support.v7.app.AlertDialog.Builder(this@MainActivity)
            builder.setTitle("Select Option")
            builder.setItems(options) { dialog, item ->
                when {
                    options[item] == "Take Photo" -> {
                        val takePicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        startActivityForResult(takePicture, mCameraRequestCode)
                    }
                    options[item] == "Choose from Gallery" -> {
                        val pickPhoto = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        startActivityForResult(pickPhoto, mGalleryRequestCode)
                    }
                    options[item] == "Cancel" -> {
                        dialog.dismiss()
                    }
                }
            }
            builder.show()
        }
    }

    private fun customDialog() {

        NameV?.text = mResultTextView.text

        val Sname = NameV?.text.toString()

        val jsonString = loadJSONFromAsset()
        if (jsonString.isNullOrEmpty()) {
            Log.e("MainActivity", "Failed to load JSON data")
            return
        }

        try {
            val obj = JSONObject(jsonString)
            val jArray = obj.getJSONArray("plant_disease")
            for (i in 0 until jArray.length()) {
                val plant = jArray.getJSONObject(i)
                pname = plant.getString("name")

                if (Sname == pname) {
                    pSymptoms = plant.getString("symptoms")
                    pManage = plant.getString("management")
                }
                SymptomsV?.text = pSymptoms
                ManageV?.text = pManage
                Log.d("Symptoms", SymptomsV?.text.toString())
                Log.d("Management", ManageV?.text.toString())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MainActivity", "Error parsing JSON data")
        }
    }

    private fun loadJSONFromAsset(): String? {
        var json: String? = null
        try {
            val inputStream: InputStream = this.assets.open("data.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            val charset: Charset = Charsets.UTF_8
            inputStream.read(buffer)
            inputStream.close()
            json = String(buffer, charset)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
        return json
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                mCameraRequestCode -> {
                    mBitmap = data.extras!!.get("data") as Bitmap
                    mBitmap = scaleImage(mBitmap)
                    mPhotoImageView.setImageBitmap(mBitmap)
                    val model_output = mClassifier.recognizeImage(scaleImage(mBitmap)).firstOrNull()
                    mResultTextView.text = model_output?.title

                    // Display confidence as percentage
                    val confidencePercentage = (model_output?.confidence ?: 0f) * 100
                    mResultTextView_2.text = "Confidence: %.2f%%".format(confidencePercentage)
                }
                mGalleryRequestCode -> {
                    val selectedImage = data.data
                    try {
                        mBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, selectedImage)
                        mBitmap = scaleImage(mBitmap)
                        mPhotoImageView.setImageBitmap(mBitmap)
                        val model_output = mClassifier.recognizeImage(scaleImage(mBitmap)).firstOrNull()
                        mResultTextView.text = model_output?.title

                        // Display confidence as percentage
                        val confidencePercentage = (model_output?.confidence ?: 0f) * 100
                        mResultTextView_2.text = "Confidence: %.2f%%".format(confidencePercentage)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }



    fun scaleImage(bitmap: Bitmap?): Bitmap {
        val width = bitmap!!.width
        val height = bitmap.height
        val scaledWidth = mInputSize.toFloat() / width
        val scaledHeight = mInputSize.toFloat() / height
        val matrix = Matrix()
        matrix.postScale(scaledWidth, scaledHeight)
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }
}
