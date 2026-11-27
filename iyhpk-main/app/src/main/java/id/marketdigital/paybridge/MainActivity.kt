package id.marketdigital.paybridge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("bridge", MODE_PRIVATE) }
    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var url: EditText
    private lateinit var secret: EditText
    private lateinit var panelUrl: EditText
    private lateinit var pteroApiKey: EditText
    private lateinit var pteroServerId: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 42, 36, 36)
            gravity = Gravity.TOP
        }

        fun label(t: String) {
            root.addView(TextView(this).apply {
                text = t
                textSize = 15f
                setPadding(0, 18, 0, 8)
            })
        }

        fun field(hint: String, value: String, password: Boolean = false): EditText = EditText(this).apply {
            this.hint = hint
            setText(value)
            inputType = if (password) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT
            }
            layoutParams = ViewGroup.LayoutParams(-1, -2)
        }

        root.addView(TextView(this).apply {
            text = "WannerPay"
            textSize = 26f
            setPadding(0, 0, 0, 8)
        })

        root.addView(TextView(this).apply {
            text = "Payment bridge, webhook, dan kontrol server Pterodactyl."
            textSize = 14f
        })

        label("Webhook URL")
        url = field(
            "https://webhook.example.com/webhook/payment",
            prefs.getString("url", "https://webhook.marketdigital.web.id/webhook/payment").orEmpty()
        )
        root.addView(url)

        label("Webhook Secret")
        secret = field(
            "Secret webhook",
            prefs.getString("secret", "").orEmpty(),
            password = true
        )
        root.addView(secret)

        root.addView(Button(this).apply {
            text = "Simpan konfigurasi webhook"
            setOnClickListener {
                saveConfig()
                status.text = "Status: konfigurasi webhook tersimpan"
            }
        })

        root.addView(Button(this).apply {
            text = "Aktifkan akses notifikasi"
            setOnClickListener {
                saveConfig()
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        })

        label("Pterodactyl Panel")
        panelUrl = field(
            "https://panel.example.com",
            prefs.getString("ptero_panel", "").orEmpty()
        )
        root.addView(panelUrl)

        label("Pterodactyl Client API Key")
        pteroApiKey = field(
            "ptlc_...",
            prefs.getString("ptero_api_key", "").orEmpty(),
            password = true
        )
        root.addView(pteroApiKey)

        label("Server ID / Identifier")
        pteroServerId = field(
            "Contoh: 1a2b3c4d",
            prefs.getString("ptero_server_id", "").orEmpty()
        )
        root.addView(pteroServerId)

        root.addView(Button(this).apply {
            text = "Simpan Pterodactyl"
            setOnClickListener {
                saveConfig()
                status.text = "Status: konfigurasi Pterodactyl tersimpan"
            }
        })

        root.addView(Button(this).apply {
            text = "Test koneksi Pterodactyl"
            setOnClickListener { runPtero { it.testConnection() } }
        })

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        fun controlButton(text: String, signal: String) {
            controls.addView(Button(this@MainActivity).apply {
                this.text = text
                setOnClickListener { runPtero { it.power(signal) } }
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            })
        }
        controlButton("START", "start")
        controlButton("RESTART", "restart")
        controlButton("STOP", "stop")
        root.addView(controls)

        status = TextView(this).apply {
            text = "Status: siap dikonfigurasi"
            textSize = 14f
            setPadding(0, 20, 0, 0)
        }
        root.addView(status)

        root.addView(TextView(this).apply {
            text = "Pterodactyl menggunakan Client API. Simpan API key dengan aman dan jangan masukkan Application API key ke aplikasi ini."
            textSize = 12f
            setPadding(0, 22, 0, 0)
        })

        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
    }

    private fun saveConfig() {
        prefs.edit()
            .putString("url", url.text.toString().trim())
            .putString("secret", secret.text.toString())
            .putString("ptero_panel", panelUrl.text.toString().trim().trimEnd('/'))
            .putString("ptero_api_key", pteroApiKey.text.toString().trim())
            .putString("ptero_server_id", pteroServerId.text.toString().trim())
            .apply()
    }

    private fun runPtero(action: (PterodactylClient) -> PterodactylClient.Result) {
        saveConfig()
        val client = PterodactylClient(
            prefs.getString("ptero_panel", "").orEmpty(),
            prefs.getString("ptero_api_key", "").orEmpty(),
            prefs.getString("ptero_server_id", "").orEmpty()
        )
        status.text = "Status: menghubungi Pterodactyl..."
        executor.execute {
            val result = action(client)
            runOnUiThread { status.text = "Status: ${result.message}" }
        }
    }
}
