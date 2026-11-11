package ir.hasabdari.paydar.ui.print

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import android.view.ViewGroup
import android.view.Window
import android.webkit.WebView
import android.webkit.WebViewClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoicePrintManager @Inject constructor() {

    fun print(html: String, jobName: String, activity: Activity) {
        try {
            // ═══ WebView داخل Dialog: proper context + layout ═══
            val dialog = Dialog(activity)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            val webView = WebView(activity).apply {
                setBackgroundColor(Color.WHITE)
                settings.apply {
                    javaScriptEnabled = false
                    loadsImagesAutomatically = true
                    domStorageEnabled = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
            }

            dialog.setContentView(webView)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // dialog نمایش داده نمی‌شود (شفاف)، فقط برای layout
            dialog.window?.setDimAmount(0f)
            dialog.show()
            dialog.hide()

            webView.webViewClient = object : WebViewClient() {
                private var started = false

                override fun onPageFinished(view: WebView, url: String?) {
                    if (started) return
                    started = true
                    // layout اجباری
                    view.measure(
                        android.view.View.MeasureSpec.makeMeasureSpec(
                            1080, android.view.View.MeasureSpec.EXACTLY
                        ),
                        android.view.View.MeasureSpec.makeMeasureSpec(
                            1920, android.view.View.MeasureSpec.AT_MOST
                        )
                    )
                    view.layout(0, 0, 1080, 1920)

                    try {
                        val printManager =
                            activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
                        val adapter = view.createPrintDocumentAdapter(jobName)
                        if (adapter == null) {
                            Log.e(TAG, "PrintDocumentAdapter is null")
                            dismissSafely(dialog)
                            return
                        }
                        val attrs = PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setResolution(
                                PrintAttributes.Resolution("pdf", "pdf", 300, 300)
                            )
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                            .build()
                        printManager.print(jobName, adapter, attrs)
                    } catch (e: Exception) {
                        Log.e(TAG, "Print failed", e)
                    } finally {
                        // WebView نیاز است تا کاربر پرینت را شروع کند؛
                        // با تأخیر کم طولانی، dialog را می‌بندیم
                        view.postDelayed({ dismissSafely(dialog) }, 2_000)
                    }
                }
            }

            webView.loadDataWithBaseURL(
                null,
                html,
                "text/html",
                "UTF-8",
                null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare print", e)
        }
    }

    private fun dismissSafely(dialog: Dialog) {
        try {
            if (dialog.isShowing) dialog.dismiss()
        } catch (_: Exception) { }
    }

    companion object {
        private const val TAG = "InvoicePrintManager"
    }
}