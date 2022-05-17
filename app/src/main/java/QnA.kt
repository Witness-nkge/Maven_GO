package com.maven.maven

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.*
import java.lang.StringBuilder
import java.util.regex.Pattern

class QnA : AppCompatActivity(), Runnable {
    private var mModule: Module? = null
    var edit: EditText? = null
    var go: TextView? = null
    var view: RecyclerView? = null
    var list: ArrayList<Data>? = null
    var context: String? = null;

    private var mEditTextQuestion: EditText? = null
    private var mTextViewAnswer: TextView? = null


    private var mTokenIdMap: HashMap<String, Long?>? = null
    private var mIdTokenMap: HashMap<Long, String>? = null

    private val MODEL_INPUT_LENGTH = 360
    private val EXTRA_ID_NUM = 3
    private val CLS = "[CLS]"
    private val SEP = "[SEP]"
    private val PAD = "[PAD]"
    private val START_LOGITS = "start_logits"
    private val END_LOGITS = "end_logits"
    var card: CardView? = null

    internal class QAException(override var message: String) : Exception()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mTextViewAnswer = findViewById(R.id.answer)

        card = findViewById(R.id.cardView)

        edit = findViewById<EditText>(R.id.search)
        go = findViewById<TextView>(R.id.go)
        view = findViewById<RecyclerView>(R.id.recycler)

        view!!.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(this)
        view!!.setLayoutManager(layoutManager)

        list = ArrayList<Data>()

        go?.setOnClickListener(View.OnClickListener {
            val search: String = edit!!.getText().toString()
            var str: String? = null
            var str1: String? = null
            if (search.contains("mitosis")) {
                str = "Mitosis"
                context = "Mitosis is a process where a single cell divides into two identical daughter cells"
            } else if (search.contains("meiosis")) {
                str = "Meiosis"
                context = "Meiosis is the process in eukaryotic, sexually-reproducing animals that reduces the number of chromosomes in a cell before reproduction. "
            } else {
            }
            if (list != null) {
                list!!.clear()
                parseEmpData(str!!)
            }
            val result = answer(edit!!.text.toString(), context!!)

                try {
                    val imm = this.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                    var view = currentFocus
                    if (view == null) view = View(this)
                    imm.hideSoftInputFromWindow(view!!.windowToken, 0)

                    val startIdx = edit!!.text.toString().toLowerCase().indexOf(result!!)
                    if (startIdx == -1) {
                        mTextViewAnswer!!.text = ""
                    }

                    mTextViewAnswer!!.text = result

                }
                catch (e:Exception){
                    Toast.makeText(applicationContext, "Error:seems like your phone cant handle algorithms implemented on this activity", Toast.LENGTH_SHORT).show()
                }

        })
       

        try {
            val br = BufferedReader(InputStreamReader(assets.open("vocab.txt")))
            var line: String
            mTokenIdMap = HashMap()
            mIdTokenMap = HashMap()
            var count = 0L
            while (true) {
                val line = br.readLine()
                if (line != null) {
                    mTokenIdMap!![line] = count
                    mIdTokenMap!![count] = line
                    count++
                } else break
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(QAException::class)
    private fun tokenizer(question: String, text: String): LongArray {
        val tokenIdsQuestion = wordPieceTokenizer(question)
        if (tokenIdsQuestion.size >= MODEL_INPUT_LENGTH) throw QAException("Question too long")
        val tokenIdsText = wordPieceTokenizer(text)
        val inputLength = tokenIdsQuestion.size + tokenIdsText.size + EXTRA_ID_NUM
        val ids = LongArray(Math.min(MODEL_INPUT_LENGTH, inputLength))
        ids[0] = mTokenIdMap!![CLS]!!

        for (i in tokenIdsQuestion.indices) ids[i + 1] = tokenIdsQuestion[i]!!.toLong()
        ids[tokenIdsQuestion.size + 1] = mTokenIdMap!![SEP]!!
        val maxTextLength = Math.min(tokenIdsText.size, MODEL_INPUT_LENGTH - tokenIdsQuestion.size - EXTRA_ID_NUM)

        for (i in 0 until maxTextLength) {
            ids[tokenIdsQuestion.size + i + 2] = tokenIdsText[i]!!.toLong()
        }

        ids[tokenIdsQuestion.size + maxTextLength + 2] = mTokenIdMap!![SEP]!!
        return ids
    }

    private fun wordPieceTokenizer(questionOrText: String): List<Long?> {

        val tokenIds: MutableList<Long?> = ArrayList()
        val p = Pattern.compile("\\w+|\\S")
        val m = p.matcher(questionOrText)
        while (m.find()) {
            val token = m.group().toLowerCase()
            if (mTokenIdMap!!.containsKey(token)) tokenIds.add(mTokenIdMap!![token]) else {
                for (i in 0 until token.length) {
                    if (mTokenIdMap!!.containsKey(token.substring(0, token.length - i - 1))) {
                        tokenIds.add(mTokenIdMap!![token.substring(0, token.length - i - 1)])
                        var subToken = token.substring(token.length - i - 1)
                        var j = 0

                        while (j < subToken.length) {
                            if (mTokenIdMap!!.containsKey("##" + subToken.substring(0, subToken.length - j))) {
                                tokenIds.add(mTokenIdMap!!["##" + subToken.substring(0, subToken.length - j)])
                                subToken = subToken.substring(subToken.length - j)
                                j = subToken.length - j
                            } else if (j == subToken.length - 1) {
                                tokenIds.add(mTokenIdMap!!["##$subToken"])
                                break
                            } else j++
                        }
                        break
                    }
                }
            }
        }
        return tokenIds
    }


    fun assetFilePath(context: Context, assetName: String?): String? {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        context.assets.open(assetName!!).use { `is` ->
            FileOutputStream(file).use { os ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (`is`.read(buffer).also { read = it } != -1) {
                    os.write(buffer, 0, read)
                }
                os.flush()
            }
            return file.absolutePath
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("NewApi")
    private fun answer(question: String, text: String): String? {
        if (mModule == null) {
            mModule = LiteModuleLoader.load(this.assetFilePath(this, "qa360_quantized.ptl"))
        }

        try {
            val tokenIds = tokenizer(question, text)
            val inTensorBuffer = Tensor.allocateLongBuffer(MODEL_INPUT_LENGTH)
            for (n in tokenIds) inTensorBuffer.put(n.toLong())
            for (i in 0 until MODEL_INPUT_LENGTH - tokenIds.size) mTokenIdMap!![PAD]?.let { inTensorBuffer.put(it) }

            val inTensor = Tensor.fromBlob(inTensorBuffer, longArrayOf(1, MODEL_INPUT_LENGTH.toLong()))
            val outTensors = mModule!!.forward(IValue.from(inTensor)).toDictStringKey()
            val startTensor = outTensors[START_LOGITS]!!.toTensor()
            val endTensor = outTensors[END_LOGITS]!!.toTensor()

            val starts = startTensor.dataAsFloatArray
            val ends = endTensor.dataAsFloatArray
            val answerTokens: MutableList<String?> = ArrayList()
            val start = argmax(starts)
            val end = argmax(ends)
            for (i in start until end + 1) answerTokens.add(mIdTokenMap!![tokenIds[i]])

            return java.lang.String.join(" ", answerTokens).replace(" ##".toRegex(), "").replace("\\s+(?=\\p{Punct})".toRegex(), "")
        } catch (e: QAException) {
            runOnUiThread { mTextViewAnswer!!.text = e.message }
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun run() {
        val result = answer(edit!!.text.toString(), context!!)

        if (result == null) return

        runOnUiThread {
            try {
                val imm = this.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                var view = currentFocus
                if (view == null) view = View(this)
                imm.hideSoftInputFromWindow(view.windowToken, 0)

                val startIdx = edit!!.text.toString().toLowerCase().indexOf(result)
                if (startIdx == -1) {
                    mTextViewAnswer!!.text = ""
                    return@runOnUiThread
                }

                mTextViewAnswer!!.text = result
                card!!.visibility = View.VISIBLE

            }
            catch (e:Exception){
                Toast.makeText(applicationContext, "Error:seems like your phone cant handle algorithms implemented on this activity", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun argmax(array: FloatArray): Int {
        var maxIdx = 0
        var maxVal: Double = -java.lang.Double.MAX_VALUE
        for (j in array.indices) {
            if (array[j] > maxVal) {
                maxVal = array[j].toDouble()
                maxIdx = j
            }
        }
        return maxIdx
    }
    private fun parseEmpData(str: String) {
        try {
            val loadJsonData = fetchJSON()
            val rootJSONObject = JSONObject(loadJsonData)
            val empJSONArray = rootJSONObject.getJSONArray(str)
            for (i in 0 until empJSONArray.length()) {
                val mitosis = empJSONArray.getJSONObject(i)
                val title = mitosis.getString("title")
                val link = mitosis.getString("link")
                val description = mitosis.getString("description")
                val web = mitosis.getString("web")
                val data = Data()
                data.title = title
                data.link = link
                data.description = description
                data.web = web
                list!!.add(data)
                val adapter = DataAdapter(this, list)
                view!!.adapter = adapter
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    fun fetchJSON(): String {
        var inputStream: InputStream? = null
        val stringBuilder = StringBuilder()
        try {
            var jsonData: String? = null
            inputStream = resources.openRawResource(R.raw.dataset)
            val bufferedReader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            while (bufferedReader.readLine().also { jsonData = it } != null) {
                stringBuilder.append(jsonData)
            }
        } finally {
            inputStream?.close()
        }
        return String(stringBuilder)
    }
}